package graviks2d.context

import graviks2d.util.assertSuccess
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

    private var shouldClearDepthImage = false
    private var hasDrawnBefore = false

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
    }

    private fun endSubmitWaitCommandBuffer(stack: MemoryStack) {
        assertSuccess(
            vkEndCommandBuffer(this.commandBuffer), "vkEndCommandBuffer"
        )

        val pSubmitInfo = VkSubmitInfo.calloc(1, stack)
        val submitInfo = pSubmitInfo[0]
        submitInfo.`sType$Default`()
        submitInfo.waitSemaphoreCount(0)
        submitInfo.pCommandBuffers(stack.pointers(this.commandBuffer.address()))

        assertSuccess(
            this.context.instance.synchronizedQueueSubmit(pSubmitInfo, this.fence),
            "synchronizedQueueSubmit"
        )

        // If this simple command can't complete within this timeout, something is wrong
        val timeout = 1_000_000_000L
        assertSuccess(
            vkWaitForFences(this.context.instance.device, stack.longs(this.fence), true, timeout),
            "vkWaitForFences"
        )
        assertSuccess(
            vkResetFences(this.context.instance.device, stack.longs(this.fence)),
            "vkResetFences"
        )
    }

    private fun initImageLayouts() {
        stackPush().use { stack ->

            resetBeginCommandBuffer(stack)

            transitionColorImageLayout(
                stack, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                0, VK_ACCESS_COLOR_ATTACHMENT_READ_BIT
            )
            transitionDepthImageLayout(
                stack, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
                VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT,
                0, VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT
            )
        }
    }

    private fun transitionColorImageLayout(
        stack: MemoryStack, currentLayout: Int, desiredLayout: Int,
        srcStageMask: Int, dstStageMask: Int, srcAccessMask: Int, dstAccessMask: Int
    ) {
        transitionImageLayout(
            stack, this.context.targetImages.colorImage, VK_IMAGE_ASPECT_COLOR_BIT,
            currentLayout, desiredLayout,
            srcStageMask, dstStageMask, srcAccessMask, dstAccessMask
        )
    }

    private fun transitionDepthImageLayout(
        stack: MemoryStack, currentLayout: Int, desiredLayout: Int,
        srcStageMask: Int, dstStageMask: Int, srcAccessMask: Int, dstAccessMask: Int
    ) {
        transitionImageLayout(
            stack, this.context.targetImages.depthImage, VK_IMAGE_ASPECT_DEPTH_BIT,
            currentLayout, desiredLayout,
            srcStageMask, dstStageMask, srcAccessMask, dstAccessMask
        )
    }

    private fun transitionImageLayout(
        stack: MemoryStack, image: Long, aspectBit: Int,
        currentLayout: Int, desiredLayout: Int,
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
            it.aspectMask(aspectBit)
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

    fun clearDepthImage() {
        this.shouldClearDepthImage = true
    }

    private fun clearDepthImageNow(stack: MemoryStack, currentDepthImageLayout: Int) {
        transitionDepthImageLayout(
            stack,
            currentDepthImageLayout,
            VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
            VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
            VK_PIPELINE_STAGE_TRANSFER_BIT,
            0,
            VK_ACCESS_TRANSFER_READ_BIT
        )

        val clearRanges = VkImageSubresourceRange.calloc(1, stack)
        val clearRange = clearRanges[0]
        clearRange.aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT)
        clearRange.baseMipLevel(0)
        clearRange.baseArrayLayer(0)
        clearRange.levelCount(1)
        clearRange.layerCount(1)

        vkCmdClearDepthStencilImage(
            this.commandBuffer,
            this.context.targetImages.depthImage,
            VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
            VkClearDepthStencilValue.calloc(stack).set(1f, 0),
            clearRanges
        )

        transitionDepthImageLayout(
            stack,
            VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
            VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
            VK_PIPELINE_STAGE_TRANSFER_BIT,
            VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT,
            VK_ACCESS_TRANSFER_READ_BIT,
            VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT
        )
    }

    fun copyColorImageTo(destImage: Long?, destBuffer: Long?) {
        stackPush().use { stack ->
            resetBeginCommandBuffer(stack)

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

            endSubmitWaitCommandBuffer(stack)
        }
    }

    fun draw(numVertices: Int, maxDepth: Int) {
        stackPush().use { stack ->

            if (hasDrawnBefore) {
                resetBeginCommandBuffer(stack)
            }
            hasDrawnBefore = true

            if (this.shouldClearDepthImage) {
                this.clearDepthImageNow(stack, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)
                this.shouldClearDepthImage = false
            }

            val biRenderPass = VkRenderPassBeginInfo.calloc(stack)
            biRenderPass.`sType$Default`()
            biRenderPass.renderPass(this.context.instance.pipeline.vkRenderPass)
            biRenderPass.framebuffer(this.context.targetImages.framebuffer)
            biRenderPass.renderArea {
                it.offset { offset -> offset.set(0, 0) }
                it.extent { extent -> extent.set(this.context.width, this.context.height) }
            }

            vkCmdBeginRenderPass(this.commandBuffer, biRenderPass, VK_SUBPASS_CONTENTS_INLINE)

            vkCmdBindPipeline(
                this.commandBuffer,
                VK_PIPELINE_BIND_POINT_GRAPHICS,
                this.context.instance.pipeline.vkPipeline
            )

            val viewports = VkViewport.calloc(1, stack)
            val viewport = viewports[0]
            viewport.x(0f)
            viewport.y(0f)
            viewport.width(this.context.width.toFloat())
            viewport.height(this.context.height.toFloat())
            viewport.minDepth(0f)
            viewport.maxDepth(1f)

            val scissors = VkRect2D.calloc(1, stack)
            val scissor = scissors[0]
            scissor.offset { it.set(0, 0) }
            scissor.extent { it.set(this.context.width, this.context.height) }

            vkCmdSetViewport(this.commandBuffer, 0, viewports)
            vkCmdSetScissor(this.commandBuffer, 0, scissors)

            vkCmdPushConstants(
                this.commandBuffer,
                this.context.instance.pipeline.vkPipelineLayout,
                VK_SHADER_STAGE_VERTEX_BIT,
                0,
                stack.ints(maxDepth)
            )

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

            vkCmdDraw(
                this.commandBuffer, numVertices, 1, 0,0
            )

            vkCmdEndRenderPass(this.commandBuffer)
            endSubmitWaitCommandBuffer(stack)
        }
    }

    fun destroy() {
        vkDestroyCommandPool(context.instance.device, this.commandPool, null)
        vkDestroyFence(context.instance.device, this.fence, null)
    }
}
