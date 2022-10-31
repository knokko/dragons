package dragons.plugins.standard.vulkan.render

import dragons.plugins.standard.vulkan.pipeline.BasicGraphicsPipeline
import dragons.plugins.standard.vulkan.render.entity.StandardEntityRenderer
import dragons.plugins.standard.vulkan.render.tile.StandardTileRenderer
import dragons.vulkan.queue.QueueFamily
import dragons.vulkan.util.assertVkSuccess
import dragons.world.render.SceneRenderTarget
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

internal class SceneCommands(
    private val vkDevice: VkDevice, private val queueFamily: QueueFamily,
    private val basicRenderPass: Long, private val basicPipeline: BasicGraphicsPipeline,
    private val staticDescriptorSet: Long, private val renderTarget: SceneRenderTarget
) {

    private val commandPool: Long
    // TODO Add support for multiple command buffers (avoids the need to wait on the old one before a new one can be created)
    private val commandBuffer: VkCommandBuffer
    private val submissionFence: Long

    // Initializing this to true ensures that the commands are always recorded during the first frame
    var shouldRecordCommandsAgain = true
        private set

    init {
        stackPush().use { stack ->
            val ciCommandPool = VkCommandPoolCreateInfo.calloc(stack)
            ciCommandPool.`sType$Default`()
            ciCommandPool.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
            ciCommandPool.queueFamilyIndex(this.queueFamily.index)

            val pCommandPool = stack.callocLong(1)
            assertVkSuccess(
                vkCreateCommandPool(this.vkDevice, ciCommandPool, null, pCommandPool),
                "CreateCommandPool", "SceneCommands"
            )
            this.commandPool = pCommandPool[0]

            val aiCommandBuffer = VkCommandBufferAllocateInfo.calloc(stack)
            aiCommandBuffer.`sType$Default`()
            aiCommandBuffer.commandPool(this.commandPool)
            aiCommandBuffer.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
            aiCommandBuffer.commandBufferCount(1)

            val pCommandBuffer = stack.callocPointer(1)
            assertVkSuccess(
                vkAllocateCommandBuffers(this.vkDevice, aiCommandBuffer, pCommandBuffer),
                "AllocateCommandBuffers", "SceneCommands"
            )
            this.commandBuffer = VkCommandBuffer(pCommandBuffer[0], this.vkDevice)

            val ciFence = VkFenceCreateInfo.calloc(stack)
            ciFence.`sType$Default`()

            // Initially, there will be no 'previous submission'
            // so awaitLastSubmission() should return immediately when called before the first submission.
            ciFence.flags(VK_FENCE_CREATE_SIGNALED_BIT)

            val pFence = stack.callocLong(1)
            assertVkSuccess(
                vkCreateFence(this.vkDevice, ciFence, null, pFence),
                "CreateFence", "SceneCommands"
            )
            this.submissionFence = pFence[0]
        }
    }

    fun awaitLastSubmission(stack: MemoryStack) {
        assertVkSuccess(
            vkWaitForFences(this.vkDevice, stack.longs(this.submissionFence), true, 10_000_000_000L),
            "WaitForFences", "SceneCommands"
        )
    }

    fun record(
        cameraBufferManager: CameraBufferManager,
        transformationMatrixManager: TransformationMatrixManager,
        tileRenderer: StandardTileRenderer,
        entityRenderer: StandardEntityRenderer
    ) {
        stackPush().use { stack ->

            // We shouldn't overwrite the command buffer before the previous submission is finished
            this.awaitLastSubmission(stack)

            val biCommandBuffer = VkCommandBufferBeginInfo.calloc(stack)
            biCommandBuffer.`sType$Default`()
            biCommandBuffer.flags(0)
            biCommandBuffer.pInheritanceInfo(null)

            assertVkSuccess(
                vkBeginCommandBuffer(this.commandBuffer, biCommandBuffer),
                "BeginCommandBuffer", "SceneCommands"
            )

            cameraBufferManager.recordCommands(this.commandBuffer)
            transformationMatrixManager.recordCommands(this.commandBuffer)

            entityRenderer.recordCommandsBeforeRenderPass(this.commandBuffer)

            val clearValues = VkClearValue.calloc(2, stack)
            val colorClearValue = clearValues[0]
            colorClearValue.color { color ->
                color.float32(0, 68f / 255f)
                color.float32(1, 107f / 255f)
                color.float32(2, 176f / 255f)
                color.float32(3, 1f)
            }
            val depthClearValue = clearValues[1]
            depthClearValue.depthStencil { depthStencil ->
                depthStencil.depth(1f) // 1 is at the far plane
                depthStencil.stencil(0) // I don't really know what to do with this...
            }

            val biRenderPass = VkRenderPassBeginInfo.calloc(stack)
            biRenderPass.`sType$Default`()
            biRenderPass.renderPass(this.basicRenderPass)
            // framebuffer will be filled in later
            biRenderPass.renderArea { renderArea ->
                renderArea.offset { offset -> offset.set(0, 0) }
                renderArea.extent { extent -> extent.set(this.renderTarget.width, this.renderTarget.height) }
            }
            biRenderPass.clearValueCount(2)
            biRenderPass.pClearValues(clearValues)

            for ((eyeIndex, framebuffer) in arrayOf(this.renderTarget.leftFramebuffer, this.renderTarget.rightFramebuffer).withIndex()) {
                biRenderPass.framebuffer(framebuffer)

                vkCmdBeginRenderPass(this.commandBuffer, biRenderPass, VK_SUBPASS_CONTENTS_INLINE)
                vkCmdBindPipeline(
                    this.commandBuffer,
                    VK_PIPELINE_BIND_POINT_GRAPHICS,
                    this.basicPipeline.handle
                )
                vkCmdPushConstants(
                    this.commandBuffer,
                    this.basicPipeline.pipelineLayout,
                    VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT,
                    0,
                    stack.ints(eyeIndex)
                )

                tileRenderer.recordCommandsDuringRenderPass(this.commandBuffer, this.basicPipeline, this.staticDescriptorSet)
                entityRenderer.recordCommandsDuringRenderPass(this.commandBuffer, this.basicPipeline, this.staticDescriptorSet)

                vkCmdEndRenderPass(this.commandBuffer)
            }

            assertVkSuccess(vkEndCommandBuffer(this.commandBuffer), "EndCommandBuffer", "SceneCommands")
        }

        this.shouldRecordCommandsAgain = false
    }

    fun submit(waitSemaphores: LongArray, waitStageMasks: IntArray, signalSemaphores: LongArray) {
        if (waitSemaphores.size != waitStageMasks.size) {
            throw IllegalArgumentException("#of waitSemaphores (${waitSemaphores.size}) is not equal to #of waitStageMasks (${waitStageMasks.size})")
        }

        stackPush().use { stack ->
            val pSubmits = VkSubmitInfo.calloc(1, stack)
            val pSubmit = pSubmits[0]
            pSubmit.`sType$Default`()
            pSubmit.waitSemaphoreCount(waitSemaphores.size)
            if (waitSemaphores.isNotEmpty()) {

                val pWaitSemaphores = stack.callocLong(waitSemaphores.size)
                for ((index, semaphore) in waitSemaphores.withIndex()) {
                    pWaitSemaphores.put(index, semaphore)
                }
                pSubmit.pWaitSemaphores(pWaitSemaphores)

                val pWaitStageMasks = stack.callocInt(waitStageMasks.size)
                for ((index, stageMask) in waitStageMasks.withIndex()) {
                    pWaitStageMasks.put(index, stageMask)
                }
                pSubmit.pWaitDstStageMask(pWaitStageMasks)
            }
            pSubmit.pCommandBuffers(stack.pointers(this.commandBuffer.address()))
            if (signalSemaphores.isNotEmpty()) {

                val pSignalSemaphores = stack.callocLong(signalSemaphores.size)
                for ((index, semaphore) in signalSemaphores.withIndex()) {
                    pSignalSemaphores.put(index, semaphore)
                }
                pSubmit.pSignalSemaphores(pSignalSemaphores)
            }

            assertVkSuccess(
                vkResetFences(this.vkDevice, this.submissionFence),
                "ResetFences", "SceneCommands"
            )
            this.queueFamily.getRandomPriorityQueue().submit(pSubmits, this.submissionFence)
        }
    }

    fun destroy() {
        stackPush().use(this::awaitLastSubmission)
        vkDestroyFence(this.vkDevice, this.submissionFence, null)
        vkDestroyCommandPool(this.vkDevice, this.commandPool, null)
    }
}
