package graviks2d.context

import graviks2d.resource.text.rasterizeTextAtlas
import graviks2d.util.assertSuccess
import kotlinx.coroutines.CompletableDeferred
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

internal class ContextCommands(
    private val context: GraviksContext
) {

    private val commandPool: Long
    private val commandBuffer: VkCommandBuffer
    private val fence: Long

    private val waitSemaphores = mutableListOf<Long>()

    private var hasDrawnBefore = false

    private var isStillRecording = false
    private var hasPendingSubmission = false

    init {
        stackPush().use { stack ->

            val ciCommandPool = VkCommandPoolCreateInfo.calloc(stack)
            ciCommandPool.`sType$Default`()
            ciCommandPool.flags(VK_COMMAND_POOL_CREATE_TRANSIENT_BIT)
            ciCommandPool.queueFamilyIndex(context.instance.queueFamilyIndex)

            val pCommandPool = stack.callocLong(1)
            assertSuccess(
                vkCreateCommandPool(context.instance.device, ciCommandPool, null, pCommandPool),
                "vkCreateCommandPool"
            )
            this.commandPool = pCommandPool[0]

            val aiCommandBuffer = VkCommandBufferAllocateInfo.calloc(stack)
            aiCommandBuffer.`sType$Default`()
            aiCommandBuffer.commandPool(commandPool)
            aiCommandBuffer.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
            aiCommandBuffer.commandBufferCount(1)

            val pCommandBuffer = stack.callocPointer(1)
            assertSuccess(
                vkAllocateCommandBuffers(context.instance.device, aiCommandBuffer, pCommandBuffer),
                "vkAllocateCommandBuffers"
            )
            this.commandBuffer = VkCommandBuffer(pCommandBuffer[0], context.instance.device)

            val ciFence = VkFenceCreateInfo.calloc(stack)
            ciFence.`sType$Default`()

            val pFence = stack.callocLong(1)
            assertSuccess(
                vkCreateFence(context.instance.device, ciFence, null, pFence),
                "vkCreateFence"
            )
            this.fence = pFence[0]

            this.initImageLayouts()
        }
    }

    private fun resetBeginCommandBuffer(stack: MemoryStack) {

        if (isStillRecording) throw IllegalStateException("Already recording commands")

        if (hasPendingSubmission) throw IllegalStateException("Can't reset command buffer with pending submission")

        assertSuccess(
            vkResetCommandPool(this.context.instance.device, this.commandPool, 0),
            "vkResetCommandPool"
        )

        val biCommandBuffer = VkCommandBufferBeginInfo.calloc(stack)
        biCommandBuffer.`sType$Default`()
        biCommandBuffer.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)

        assertSuccess(
            vkBeginCommandBuffer(this.commandBuffer, biCommandBuffer),
            "vkBeginCommandBuffer"
        )

        isStillRecording = true
    }

    private fun endSubmitCommandBuffer(
        stack: MemoryStack, signalSemaphore: Long?,
        submissionMarker: CompletableDeferred<Unit>?
    ) {

        if (!isStillRecording) throw IllegalStateException("No commands are recorded")

        assertSuccess(
            vkEndCommandBuffer(this.commandBuffer), "vkEndCommandBuffer"
        )

        val pSubmitInfo = VkSubmitInfo.calloc(1, stack)
        val submitInfo = pSubmitInfo[0]
        submitInfo.`sType$Default`()
        submitInfo.waitSemaphoreCount(waitSemaphores.size)
        if (waitSemaphores.isNotEmpty()) {
            val pWaitSemaphores = stack.callocLong(waitSemaphores.size)
            val pWaitDstStageMasks = stack.callocInt(waitSemaphores.size)
            for ((index, semaphore) in waitSemaphores.withIndex()) {
                pWaitSemaphores.put(index, semaphore)
                pWaitDstStageMasks.put(index, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT)
            }
            submitInfo.pWaitSemaphores(pWaitSemaphores)
            submitInfo.pWaitDstStageMask(pWaitDstStageMasks)
        }
        waitSemaphores.clear()
        if (signalSemaphore != null) {
            submitInfo.pSignalSemaphores(stack.longs(signalSemaphore))
        } else {
            submitInfo.pSignalSemaphores(null)
        }
        submitInfo.pCommandBuffers(stack.pointers(this.commandBuffer.address()))

        assertSuccess(
            this.context.instance.synchronizedQueueSubmit(pSubmitInfo, this.fence),
            "synchronizedQueueSubmit"
        )

        submissionMarker?.complete(Unit)
        isStillRecording = false
        hasPendingSubmission = true
    }

    internal fun awaitPendingSubmission(stack: MemoryStack) {

        if (!hasPendingSubmission) throw IllegalStateException("There is no pending submission to await")

        // If this simple command can't complete within this timeout, something is wrong
        val timeout = 10_000_000_000L
        assertSuccess(
            vkWaitForFences(this.context.instance.device, stack.longs(this.fence), true, timeout),
            "vkWaitForFences"
        )
        assertSuccess(
            vkResetFences(this.context.instance.device, stack.longs(this.fence)),
            "vkResetFences"
        )

        hasPendingSubmission = false
    }

    private fun endSubmitWaitCommandBuffer(
        stack: MemoryStack, signalSemaphore: Long?,
        submissionMarker: CompletableDeferred<Unit>?
    ) {
        endSubmitCommandBuffer(stack, signalSemaphore, submissionMarker)
        awaitPendingSubmission(stack)
    }

    private fun initImageLayouts() {
        stackPush().use { stack ->

            resetBeginCommandBuffer(stack)

            transitionColorImageLayout(
                stack, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                0, VK_ACCESS_COLOR_ATTACHMENT_READ_BIT
            )
        }
    }

    private fun transitionColorImageLayout(
        stack: MemoryStack, currentLayout: Int, desiredLayout: Int,
        srcStageMask: Int, dstStageMask: Int, srcAccessMask: Int, dstAccessMask: Int
    ) {
        transitionImageLayout(
            stack, this.context.targetImages.colorImage, currentLayout, desiredLayout,
            srcStageMask, dstStageMask, srcAccessMask, dstAccessMask
        )
    }

    private fun transitionImageLayout(
        stack: MemoryStack, image: Long, currentLayout: Int, desiredLayout: Int,
        srcStageMask: Int, dstStageMask: Int, srcAccessMask: Int, dstAccessMask: Int
    ) {

        val imageBarriers = VkImageMemoryBarrier.calloc(1, stack)
        val imageBarrier = imageBarriers[0]
        imageBarrier.`sType$Default`()
        imageBarrier.srcAccessMask(srcAccessMask)
        imageBarrier.dstAccessMask(dstAccessMask)
        imageBarrier.oldLayout(currentLayout)
        imageBarrier.newLayout(desiredLayout)
        imageBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
        imageBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
        imageBarrier.image(image)
        imageBarrier.subresourceRange {
            it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
            it.baseMipLevel(0)
            it.baseArrayLayer(0)
            it.levelCount(1)
            it.layerCount(1)
        }

        vkCmdPipelineBarrier(
            this.commandBuffer, srcStageMask, dstStageMask, 0,
            null, null, imageBarriers
        )
    }

    fun addWaitSemaphore(semaphore: Long) {
        waitSemaphores.add(semaphore)
    }

    fun copyColorImageTo(
        destImage: Long?, destBuffer: Long?, destImageFormat: Int?,
        signalSemaphore: Long?, submissionMarker: CompletableDeferred<Unit>?,
        originalImageLayout: Int?, imageSrcAccessMask: Int?, imageSrcStageMask: Int?,
        finalImageLayout: Int?, imageDstAccessMask: Int?, imageDstStageMask: Int?,
        shouldAwaitCompletion: Boolean
    ) {

        stackPush().use { stack ->
            if (hasPendingSubmission) throw IllegalStateException("Still has pending submission")

            if (!isStillRecording) {
                resetBeginCommandBuffer(stack)
            }

            transitionColorImageLayout(
                stack, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
                VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT, VK_ACCESS_TRANSFER_READ_BIT
            )

            fun populateSubresource(srr: VkImageSubresourceLayers) {
                srr.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                srr.mipLevel(0)
                srr.baseArrayLayer(0)
                srr.layerCount(1)
            }

            if (destImage != null) {
                fun checkPresent(value: Int?, name: String) {
                    if (value == null) {
                        throw IllegalArgumentException("When destImage is not null, $name must not be null")
                    }
                }

                checkPresent(originalImageLayout, "originalImageLayout")
                checkPresent(imageSrcAccessMask, "imageSrcAccessMask")
                checkPresent(imageSrcStageMask, "imageSrcStageMask")
                checkPresent(finalImageLayout, "finalImageLayout")
                checkPresent(imageDstAccessMask, "imageDstAccessMask")
                checkPresent(imageDstStageMask, "imageDstStageMask")

                if (originalImageLayout != VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
                    transitionImageLayout(
                        stack, destImage,
                        currentLayout = originalImageLayout!!, desiredLayout = VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                        srcStageMask = imageSrcStageMask!!, dstStageMask = VK_PIPELINE_STAGE_TRANSFER_BIT,
                        srcAccessMask = imageSrcAccessMask!!, dstAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT
                    )
                }

                if (destImageFormat == TARGET_COLOR_FORMAT) {

                    val imageCopyRegions = VkImageCopy.calloc(1, stack)
                    val copyRegion = imageCopyRegions[0]
                    populateSubresource(copyRegion.srcSubresource())
                    copyRegion.srcOffset { it.set(0, 0, 0) }
                    populateSubresource(copyRegion.dstSubresource())
                    copyRegion.dstOffset { it.set(0, 0, 0) }
                    copyRegion.extent { it.set(this.context.width, this.context.height, 1) }

                    vkCmdCopyImage(
                        this.commandBuffer,
                        this.context.targetImages.colorImage, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                        destImage, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                        imageCopyRegions
                    )
                } else {
                    val imageCopyRegions = VkImageBlit.calloc(1, stack)
                    val copyRegion = imageCopyRegions[0]
                    populateSubresource(copyRegion.srcSubresource())
                    copyRegion.srcOffsets { offsets ->
                        offsets[0].set(0, 0, 0)
                        offsets[1].set(this.context.width, this.context.height, 1)
                    }
                    populateSubresource(copyRegion.dstSubresource())
                    copyRegion.dstOffsets { offsets ->
                        offsets[0].set(0, 0, 0)
                        offsets[1].set(this.context.width, this.context.height, 1)
                    }

                    vkCmdBlitImage(
                        this.commandBuffer,
                        this.context.targetImages.colorImage, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                        destImage, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                        imageCopyRegions, VK_FILTER_NEAREST
                    )
                }

                if (finalImageLayout != VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
                    transitionImageLayout(
                        stack, destImage,
                        currentLayout = VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, desiredLayout = finalImageLayout!!,
                        srcStageMask = VK_PIPELINE_STAGE_TRANSFER_BIT, dstStageMask = imageDstStageMask!!,
                        srcAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT, dstAccessMask = imageDstAccessMask!!
                    )
                }
            }

            if (destBuffer != null) {
                val bufferCopyRegions = VkBufferImageCopy.calloc(1, stack)
                val copyRegion = bufferCopyRegions[0]
                copyRegion.bufferOffset(0)
                copyRegion.bufferRowLength(this.context.width)
                copyRegion.bufferImageHeight(this.context.height)
                populateSubresource(copyRegion.imageSubresource())
                copyRegion.imageOffset { it.set(0, 0, 0) }
                copyRegion.imageExtent { it.set(this.context.width, this.context.height, 1) }

                vkCmdCopyImageToBuffer(
                    this.commandBuffer,
                    this.context.targetImages.colorImage, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                    destBuffer, bufferCopyRegions
                )
            }

            transitionColorImageLayout(
                stack, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                VK_ACCESS_TRANSFER_READ_BIT, VK_ACCESS_COLOR_ATTACHMENT_READ_BIT
            )

            if (shouldAwaitCompletion) {
                endSubmitWaitCommandBuffer(stack, signalSemaphore, submissionMarker)
            } else {
                endSubmitCommandBuffer(stack, signalSemaphore, submissionMarker)
            }
        }
    }

    fun draw(drawCommands: List<DrawCommand>, endSubmitAndWait: Boolean) {
        stackPush().use { stack ->

            if (hasDrawnBefore) {
                resetBeginCommandBuffer(stack)
            }

            rasterizeTextAtlas(commandBuffer, this.context.textShapeCache, !hasDrawnBefore)

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
                            stack.longs(this.context.buffers.vertexVkBuffer),
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

        vkDestroyCommandPool(context.instance.device, this.commandPool, null)
        vkDestroyFence(context.instance.device, this.fence, null)
    }
}
