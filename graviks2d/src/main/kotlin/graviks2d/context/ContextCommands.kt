package graviks2d.context

import graviks2d.resource.text.rasterizeTextAtlas
import kotlinx.coroutines.CompletableDeferred
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import troll.exceptions.VulkanFailureException.assertVkSuccess
import troll.sync.ResourceUsage
import troll.sync.WaitSemaphore

internal class ContextCommands(
    private val context: GraviksContext
) {

    private val commandPool: Long
    private val commandBuffer: VkCommandBuffer
    private val fence: Long

    private val waitSemaphores = mutableListOf<WaitSemaphore>()

    private var hasDrawnBefore = false

    private var isStillRecording = false
    private var hasPendingSubmission = false

    init {
        val troll = context.instance.troll
        this.commandPool = troll.commands.createPool(
            VK_COMMAND_POOL_CREATE_TRANSIENT_BIT,
            troll.queueFamilies().graphics.index,
            "GraviksContextCommandPool"
        )
        this.commandBuffer = troll.commands.createPrimaryBuffers(this.commandPool, 1, "GraviksContextCommandBuffer")[0]
        this.fence = troll.sync.createFences(false, 1, "GraviksContextCommandFence")[0]
        this.initImageLayouts()
    }

    private fun resetBeginCommandBuffer(stack: MemoryStack) {

        if (isStillRecording) throw IllegalStateException("Already recording commands")

        if (hasPendingSubmission) throw IllegalStateException("Can't reset command buffer with pending submission")

        assertVkSuccess(
            vkResetCommandPool(this.context.instance.troll.vkDevice(), this.commandPool, 0),
            "vkResetCommandPool", "ContextCommands"
        )

        context.instance.troll.commands.begin(commandBuffer, stack, "GraviksContextCommands")
        isStillRecording = true
    }

    private fun endSubmitCommandBuffer(
        signalSemaphore: Long?, submissionMarker: CompletableDeferred<Unit>?
    ) {

        if (!isStillRecording) throw IllegalStateException("No commands are recorded")

        assertVkSuccess(
            vkEndCommandBuffer(this.commandBuffer), "vkEndCommandBuffer", "ContextCommands"
        )

        val signalSemaphores = if (signalSemaphore != null) longArrayOf(signalSemaphore) else LongArray(0)

        this.context.instance.troll.queueFamilies().graphics.queues.random().submit(
            commandBuffer, "ContextCommands.endSubmitCommandBuffer",
            waitSemaphores.toTypedArray(), fence, *signalSemaphores
        )
        waitSemaphores.clear()

        submissionMarker?.complete(Unit)
        isStillRecording = false
        hasPendingSubmission = true
    }

    internal fun awaitPendingSubmission(stack: MemoryStack) {

        if (!hasPendingSubmission) throw IllegalStateException("There is no pending submission to await")

        // If this simple command can't complete within this timeout, something is wrong
        val timeout = 10_000_000_000L
        context.instance.troll.sync.waitAndReset(stack, fence, timeout)

        hasPendingSubmission = false
    }

    private fun endSubmitWaitCommandBuffer(
        stack: MemoryStack, signalSemaphore: Long?,
        submissionMarker: CompletableDeferred<Unit>?
    ) {
        endSubmitCommandBuffer(signalSemaphore, submissionMarker)
        awaitPendingSubmission(stack)
    }

    private fun initImageLayouts() {
        stackPush().use { stack ->

            resetBeginCommandBuffer(stack)

            context.instance.troll.commands.transitionColorLayout(
                stack, commandBuffer, context.targetImages.colorImage.vkImage,
                VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, null,
                ResourceUsage(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
            )
        }
    }

    fun addWaitSemaphore(semaphore: WaitSemaphore) {
        waitSemaphores.add(semaphore)
    }

    fun copyColorImageTo(
        destImage: Long?, destBuffer: Long?, destImageFormat: Int?,
        signalSemaphore: Long?, submissionMarker: CompletableDeferred<Unit>?,
        originalImageLayout: Int?, imageSrcUsage: ResourceUsage?, imageDstUsage: ResourceUsage?,
        finalImageLayout: Int?, shouldAwaitCompletion: Boolean
    ) {
        stackPush().use { stack ->
            if (hasPendingSubmission) throw IllegalStateException("Still has pending submission")

            if (!isStillRecording) {
                resetBeginCommandBuffer(stack)
            }

            val troll = context.instance.troll
            troll.commands.transitionColorLayout(
                stack, commandBuffer, context.targetImages.colorImage.vkImage,
                VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                ResourceUsage(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT),
                ResourceUsage(VK_ACCESS_TRANSFER_READ_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT)
            )

            if (destImage != null) {
                fun checkPresent(value: Int?, name: String) {
                    if (value == null) {
                        throw IllegalArgumentException("When destImage is not null, $name must not be null")
                    }
                }

                checkPresent(originalImageLayout, "originalImageLayout")
                checkPresent(finalImageLayout, "finalImageLayout")

                if (originalImageLayout != VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
                    troll.commands.transitionColorLayout(
                        stack, commandBuffer, destImage, originalImageLayout!!, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                            imageSrcUsage, ResourceUsage(VK_ACCESS_TRANSFER_WRITE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT)
                    )
                }

                if (destImageFormat == TARGET_COLOR_FORMAT) {
                    troll.commands.copyImage(
                        commandBuffer, stack, context.width, context.height, VK_IMAGE_ASPECT_COLOR_BIT,
                        context.targetImages.colorImage.vkImage, destImage
                    )
                } else {
                    troll.commands.blitImage(
                        commandBuffer, stack, VK_IMAGE_ASPECT_COLOR_BIT, VK_FILTER_NEAREST,
                        context.targetImages.colorImage.vkImage, context.width, context.height,
                        destImage, context.width, context.height
                    )
                }

                if (finalImageLayout != VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
                    troll.commands.transitionColorLayout(
                        stack, commandBuffer, destImage, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, finalImageLayout!!,
                        ResourceUsage(VK_ACCESS_TRANSFER_WRITE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT), imageDstUsage
                    )
                }
            }

            if (destBuffer != null) {
                troll.commands.copyImageToBuffer(
                    commandBuffer, stack, VK_IMAGE_ASPECT_COLOR_BIT, context.targetImages.colorImage.vkImage,
                    context.width, context.height, destBuffer
                )
            }

            troll.commands.transitionColorLayout(
                stack, commandBuffer, context.targetImages.colorImage.vkImage,
                VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                ResourceUsage(VK_ACCESS_TRANSFER_READ_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT),
                ResourceUsage(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
            )

            if (shouldAwaitCompletion) {
                endSubmitWaitCommandBuffer(stack, signalSemaphore, submissionMarker)
            } else {
                endSubmitCommandBuffer(signalSemaphore, submissionMarker)
            }
        }
    }

    fun draw(drawCommands: List<DrawCommand>, endSubmitAndWait: Boolean) {
        stackPush().use { stack ->

            if (hasDrawnBefore) {
                resetBeginCommandBuffer(stack)
            }

            rasterizeTextAtlas(context.instance.troll, commandBuffer, this.context.textShapeCache, !hasDrawnBefore)

            hasDrawnBefore = true

            val biRenderPass = VkRenderPassBeginInfo.calloc(stack)
            biRenderPass.`sType$Default`()
            biRenderPass.renderPass(this.context.instance.pipeline.vkRenderPass)
            biRenderPass.framebuffer(this.context.targetImages.framebuffer)
            biRenderPass.renderArea {
                it.offset { offset -> offset.set(0, 0) }
                it.extent { extent -> extent.set(this.context.width, this.context.height) }
            }

            val viewports = VkViewport.calloc(1, stack)
            val viewport = viewports[0]
            viewport.x(0f)
            viewport.y(0f)
            viewport.width(this.context.width.toFloat())
            viewport.height(this.context.height.toFloat())

            val scissors = VkRect2D.calloc(1, stack)
            val scissor = scissors[0]
            scissor.offset { it.set(0, 0) }
            scissor.extent { it.set(this.context.width, this.context.height) }

            var isInsideRenderPass = false

            for (drawCommand in drawCommands) {
                if (drawCommand.numVertices > 0) {
                    if (!isInsideRenderPass) {
                        vkCmdBeginRenderPass(this.commandBuffer, biRenderPass, VK_SUBPASS_CONTENTS_INLINE)

                        vkCmdBindPipeline(
                            this.commandBuffer,
                            VK_PIPELINE_BIND_POINT_GRAPHICS,
                            this.context.instance.pipeline.vkPipeline
                        )

                        vkCmdSetViewport(this.commandBuffer, 0, viewports)
                        vkCmdSetScissor(this.commandBuffer, 0, scissors)

                        vkCmdBindVertexBuffers(
                            this.commandBuffer,
                            0,
                            stack.longs(this.context.buffers.vertexBuffer.buffer.vkBuffer),
                            stack.longs(0L)
                        )
                        vkCmdBindDescriptorSets(
                            this.commandBuffer,
                            VK_PIPELINE_BIND_POINT_GRAPHICS,
                            this.context.instance.pipeline.vkPipelineLayout,
                            0,
                            stack.longs(this.context.descriptors.descriptorSet),
                            null
                        )
                        isInsideRenderPass = true
                    }

                    vkCmdDraw(
                        this.commandBuffer, drawCommand.numVertices, 1, drawCommand.vertexIndex,0
                    )
                }
            }

            if (isInsideRenderPass) {
                vkCmdEndRenderPass(this.commandBuffer)
            }

            if (endSubmitAndWait) {
                endSubmitWaitCommandBuffer(stack, null, null)
            }
        }
    }

    fun destroy() {
        if (isStillRecording && hasDrawnBefore) throw IllegalStateException("Can't destroy ContextCommands while commands are being recorded")
        if (hasPendingSubmission) stackPush().use { stack -> awaitPendingSubmission(stack) }

        vkDestroyCommandPool(context.instance.troll.vkDevice(), this.commandPool, null)
        vkDestroyFence(context.instance.troll.vkDevice(), this.fence, null)
    }
}
