package dsl.pm2.renderer

import dsl.pm2.renderer.pipeline.Pm2PipelineInfo
import org.joml.Matrix3x2f
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.util.vma.VmaAllocationCreateInfo
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

class Pm2Scene internal constructor(
    private val vkDevice: VkDevice,
    private val vmaAllocator: Long,
    private val queueFamilyIndex: Int,
    private val backgroundRed: Int,
    private val backgroundGreen: Int,
    private val backgroundBlue: Int,
    private val width: Int,
    private val height: Int
) {
    private val colorImage: Long
    private val colorImageAllocation: Long
    private val colorImageView: Long
    private val framebuffer: Long

    private val renderPass: Long
    private val commandPool: Long
    private val commandBuffer: VkCommandBuffer
    private val fence: Long

    init {
        stackPush().use { stack ->
            val ciImage = VkImageCreateInfo.calloc(stack)
            ciImage.`sType$Default`()
            ciImage.flags(0)
            ciImage.imageType(VK_IMAGE_TYPE_2D)
            ciImage.format(VK_FORMAT_R8G8B8A8_SRGB)
            ciImage.extent().set(width, height, 1)
            ciImage.mipLevels(1)
            ciImage.arrayLayers(1)
            ciImage.samples(VK_SAMPLE_COUNT_1_BIT)
            ciImage.tiling(VK_IMAGE_TILING_OPTIMAL)
            ciImage.usage(VK_IMAGE_USAGE_TRANSFER_SRC_BIT or VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
            ciImage.sharingMode(VK_SHARING_MODE_EXCLUSIVE)
            ciImage.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)

            val ciAllocation = VmaAllocationCreateInfo.calloc(stack)
            ciAllocation.usage(VMA_MEMORY_USAGE_AUTO)

            val pImage = stack.callocLong(1)
            val pAllocation = stack.callocPointer(1)

            checkReturnValue(vmaCreateImage(
                vmaAllocator, ciImage, ciAllocation, pImage, pAllocation, null
            ), "VmaCreateImage")

            colorImage = pImage[0]
            colorImageAllocation = pAllocation[0]

            val ciImageView = VkImageViewCreateInfo.calloc(stack)
            ciImageView.`sType$Default`()
            ciImageView.flags(0)
            ciImageView.image(colorImage)
            ciImageView.viewType(VK_IMAGE_VIEW_TYPE_2D)
            ciImageView.format(ciImage.format())
            ciImageView.components().set(
                VK_COMPONENT_SWIZZLE_IDENTITY, VK_COMPONENT_SWIZZLE_IDENTITY,
                VK_COMPONENT_SWIZZLE_IDENTITY, VK_COMPONENT_SWIZZLE_IDENTITY
            )
            ciImageView.subresourceRange {
                it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                it.baseMipLevel(0)
                it.levelCount(1)
                it.baseArrayLayer(0)
                it.layerCount(1)
            }

            val pImageView = stack.callocLong(1)
            checkReturnValue(vkCreateImageView(vkDevice, ciImageView, null, pImageView), "CreateImageView")
            colorImageView = pImageView[0]

            val attachments = VkAttachmentDescription.calloc(1, stack)
            val colorAttachment = attachments[0]
            colorAttachment.flags(0)
            colorAttachment.format(ciImage.format())
            colorAttachment.samples(ciImage.samples())
            colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
            colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE)
            colorAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
            colorAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
            colorAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
            colorAttachment.finalLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)

            val subpassColorAttachments = VkAttachmentReference.calloc(1, stack)
            val subpassColorAttachment = subpassColorAttachments[0]
            subpassColorAttachment.attachment(0)
            subpassColorAttachment.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)

            val subpasses = VkSubpassDescription.calloc(1, stack)
            val subpass = subpasses[0]
            subpass.flags(0)
            subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
            subpass.pInputAttachments(null)
            subpass.colorAttachmentCount(1)
            subpass.pColorAttachments(subpassColorAttachments)
            subpass.pResolveAttachments(null)
            subpass.pDepthStencilAttachment(null)
            subpass.pPreserveAttachments(null)

            val dependencies = VkSubpassDependency.calloc(1, stack)
            dependencies[0].srcSubpass(0)
            dependencies[0].dstSubpass(VK_SUBPASS_EXTERNAL)
            dependencies[0].srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
            dependencies[0].dstStageMask(VK_PIPELINE_STAGE_TRANSFER_BIT)
            dependencies[0].srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
            dependencies[0].dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT)
            dependencies[0].dependencyFlags(0)

            val ciRenderPass = VkRenderPassCreateInfo.calloc(stack)
            ciRenderPass.`sType$Default`()
            ciRenderPass.flags(0)
            ciRenderPass.pAttachments(attachments)
            ciRenderPass.pSubpasses(subpasses)
            ciRenderPass.pDependencies(dependencies)

            val pRenderPass = stack.callocLong(1)
            checkReturnValue(vkCreateRenderPass(vkDevice, ciRenderPass, null, pRenderPass), "CreateRenderPass")
            renderPass = pRenderPass[0]

            val ciFramebuffer = VkFramebufferCreateInfo.calloc(stack)
            ciFramebuffer.`sType$Default`()
            ciFramebuffer.flags(0)
            ciFramebuffer.renderPass(renderPass)
            ciFramebuffer.attachmentCount(1)
            ciFramebuffer.pAttachments(stack.longs(colorImageView))
            ciFramebuffer.width(width)
            ciFramebuffer.height(height)
            ciFramebuffer.layers(1)

            val pFramebuffer = stack.callocLong(1)
            checkReturnValue(vkCreateFramebuffer(vkDevice, ciFramebuffer, null, pFramebuffer), "CreateFramebuffer")
            framebuffer = pFramebuffer[0]

            val ciCommandPool = VkCommandPoolCreateInfo.calloc(stack)
            ciCommandPool.`sType$Default`()
            ciCommandPool.flags(0)
            ciCommandPool.queueFamilyIndex(queueFamilyIndex)

            val pCommandPool = stack.callocLong(1)
            checkReturnValue(vkCreateCommandPool(vkDevice, ciCommandPool, null, pCommandPool), "CreateCommandPool")
            commandPool = pCommandPool[0]

            val aiCommandBuffer = VkCommandBufferAllocateInfo.calloc(stack)
            aiCommandBuffer.`sType$Default`()
            aiCommandBuffer.commandPool(commandPool)
            aiCommandBuffer.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
            aiCommandBuffer.commandBufferCount(1)

            val pCommandBuffer = stack.callocPointer(1)
            checkReturnValue(vkAllocateCommandBuffers(vkDevice, aiCommandBuffer, pCommandBuffer), "AllocateCommandBuffers")
            commandBuffer = VkCommandBuffer(pCommandBuffer[0], vkDevice)

            val ciFence = VkFenceCreateInfo.calloc(stack)
            ciFence.`sType$Default`()
            ciFence.flags(VK_FENCE_CREATE_SIGNALED_BIT)

            val pFence = stack.callocLong(1)
            checkReturnValue(vkCreateFence(vkDevice, ciFence, null, pFence), "CreateFence")
            fence = pFence[0]
        }
    }

    fun drawAndCopy(
        instance: Pm2Instance, meshes: List<Pm2Mesh>, cameraMatrix: Matrix3x2f,
        signalSemaphore: Long?, submit: (VkSubmitInfo.Buffer, fence: Long) -> Int,
        destImage: Long, oldLayout: Int, srcAccessMask: Int, srcStageMask: Int,
        newLayout: Int, dstAccessMask: Int, dstStageMask: Int,
        offsetX: Int, offsetY: Int, blitSizeX: Int, blitSizeY: Int
    ) {
        val pipelineInfo = Pm2PipelineInfo(renderPass, 0) { stack, blendState ->
            val blendAttachments = VkPipelineColorBlendAttachmentState.calloc(1, stack)
            val blendAttachment = blendAttachments[0]
            blendAttachment.blendEnable(false)
            blendAttachment.colorWriteMask(
                VK_COLOR_COMPONENT_R_BIT or VK_COLOR_COMPONENT_G_BIT or VK_COLOR_COMPONENT_B_BIT or VK_COLOR_COMPONENT_A_BIT
            )

            blendState.`sType$Default`()
            blendState.flags(0)
            blendState.logicOpEnable(false)
            blendState.attachmentCount(1)
            blendState.pAttachments(blendAttachments)
        }

        stackPush().use { stack ->
            checkReturnValue(
                vkWaitForFences(vkDevice, stack.longs(fence), true, 10_000_000_000L), "WaitForFences"
            )
            checkReturnValue(vkResetFences(vkDevice, stack.longs(fence)), "ResetFences")
            checkReturnValue(vkResetCommandPool(vkDevice, commandPool, 0), "ResetCommandPool")

            val biCommands = VkCommandBufferBeginInfo.calloc(stack)
            biCommands.`sType$Default`()
            biCommands.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)
            biCommands.pInheritanceInfo(null)

            checkReturnValue(vkBeginCommandBuffer(commandBuffer, biCommands), "BeginCommandBuffer")

            val clearValues = VkClearValue.calloc(1, stack)
            clearValues.color().float32(0, backgroundRed / 255f)
            clearValues.color().float32(1, backgroundGreen / 255f)
            clearValues.color().float32(2, backgroundBlue / 255f)
            clearValues.color().float32(3, 1f)

            val biRenderPass = VkRenderPassBeginInfo.calloc(stack)
            biRenderPass.`sType$Default`()
            biRenderPass.renderPass(renderPass)
            biRenderPass.framebuffer(framebuffer)
            biRenderPass.renderArea().offset().set(0, 0)
            biRenderPass.renderArea().extent().set(width, height)
            biRenderPass.clearValueCount(1)
            biRenderPass.pClearValues(clearValues)

            vkCmdBeginRenderPass(commandBuffer, biRenderPass, VK_SUBPASS_CONTENTS_INLINE)

            val viewport = VkViewport.calloc(1, stack)
            viewport[0].set(0f, 0f, width.toFloat(), height.toFloat(), 0f, 1f)
            vkCmdSetViewport(commandBuffer, 0, viewport)

            val scissor = VkRect2D.calloc(1, stack)
            scissor.extent().set(width, height)

            vkCmdSetScissor(commandBuffer, 0, scissor)

            instance.recordDraw(commandBuffer, pipelineInfo, meshes, cameraMatrix)

            vkCmdEndRenderPass(commandBuffer)

            if (oldLayout != VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
                val imageBarriers = VkImageMemoryBarrier.calloc(1, stack)
                val barrier = imageBarriers[0]
                barrier.`sType$Default`()
                barrier.srcAccessMask(srcAccessMask)
                barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                barrier.oldLayout(oldLayout)
                barrier.newLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                barrier.image(destImage)
                barrier.subresourceRange {
                    it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    it.baseMipLevel(0)
                    it.levelCount(1)
                    it.baseArrayLayer(0)
                    it.layerCount(1)
                }

                vkCmdPipelineBarrier(
                    commandBuffer, srcStageMask, VK_PIPELINE_STAGE_TRANSFER_BIT, 0,
                    null, null, imageBarriers
                )
            }

            val blitRegion = VkImageBlit.calloc(1, stack)
            blitRegion.srcSubresource {
                it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                it.mipLevel(0)
                it.baseArrayLayer(0)
                it.layerCount(1)
            }
            blitRegion.srcOffsets(0).set(0, 0, 0)
            blitRegion.srcOffsets(1).set(width, height, 1)
            blitRegion.dstSubresource {
                it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                it.mipLevel(0)
                it.baseArrayLayer(0)
                it.layerCount(1)
            }
            blitRegion.dstOffsets(0).set(offsetX, offsetY, 0)
            blitRegion.dstOffsets(1).set(blitSizeX, blitSizeY, 1)

            vkCmdBlitImage(
                commandBuffer, colorImage, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                destImage, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, blitRegion, VK_FILTER_LINEAR
            )

            if (newLayout != VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
                val imageBarriers = VkImageMemoryBarrier.calloc(1, stack)
                val barrier = imageBarriers[0]
                barrier.`sType$Default`()
                barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                barrier.dstAccessMask(dstAccessMask)
                barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                barrier.newLayout(newLayout)
                barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                barrier.image(destImage)
                barrier.subresourceRange {
                    it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    it.baseMipLevel(0)
                    it.levelCount(1)
                    it.baseArrayLayer(0)
                    it.layerCount(1)
                }

                vkCmdPipelineBarrier(
                    commandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, dstStageMask, 0,
                    null, null, imageBarriers
                )
            }

            checkReturnValue(vkEndCommandBuffer(commandBuffer), "EndCommandBuffer")

            val pSubmit = VkSubmitInfo.calloc(1, stack)
            pSubmit.`sType$Default`()
            pSubmit.waitSemaphoreCount(0)
            pSubmit.pCommandBuffers(stack.pointers(commandBuffer.address()))
            if (signalSemaphore != null) pSubmit.pSignalSemaphores(stack.longs(signalSemaphore))

            checkReturnValue(submit(pSubmit, fence), "QueueSubmit")
        }
    }

    fun destroy() {
        stackPush().use { stack ->
            checkReturnValue(vkWaitForFences(vkDevice, stack.longs(fence), true, 10_000_000_000L), "WaitForFences")
        }
        vkDestroyFence(vkDevice, fence, null)
        vkDestroyCommandPool(vkDevice, commandPool, null)
        vkDestroyRenderPass(vkDevice, renderPass, null)
        vkDestroyFramebuffer(vkDevice, framebuffer, null)
        vkDestroyImageView(vkDevice, colorImageView, null)
        vmaDestroyImage(vmaAllocator, colorImage, colorImageAllocation)
    }
}
