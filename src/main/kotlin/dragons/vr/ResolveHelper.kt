package dragons.vr

import dragons.vulkan.RenderImageInfo
import dragons.vulkan.memory.VulkanBufferRange
import dragons.vulkan.memory.VulkanImage
import dragons.vulkan.queue.QueueManager
import dragons.vulkan.util.assertVkSuccess
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.*
import java.awt.Color
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.io.File
import java.nio.ByteBuffer
import javax.imageio.ImageIO

class ResolveHelper(
    private val leftSourceImage: VulkanImage,
    private val rightSourceImage: VulkanImage,
    val leftResolvedImage: VulkanImage,
    val rightResolvedImage: VulkanImage,
    val leftScreenshotStagingBuffer: VulkanBufferRange,
    val rightScreenshotStagingBuffer: VulkanBufferRange,
    val leftScreenshotHostBuffer: ByteBuffer,
    val rightScreenshotHostBuffer: ByteBuffer,
    vkDevice: VkDevice, queueManager: QueueManager, renderImageInfo: RenderImageInfo
) {

    private val resolveCommandPool: Long
    private val resolveCommandBuffer: VkCommandBuffer
    private val screenshotCommandBuffer: VkCommandBuffer
    private val fence: Long

    init {
        stackPush().use { stack ->
            val ciPool = VkCommandPoolCreateInfo.calloc(stack)
            ciPool.`sType$Default`()
            ciPool.flags(0)
            ciPool.queueFamilyIndex(queueManager.generalQueueFamily.index)

            val pPool = stack.callocLong(1)
            assertVkSuccess(
                vkCreateCommandPool(vkDevice, ciPool, null, pPool),
                "CreateCommandPool", "resolve eye images"
            )
            this.resolveCommandPool = pPool[0]

            val aiBuffer = VkCommandBufferAllocateInfo.calloc(stack)
            aiBuffer.`sType$Default`()
            aiBuffer.commandPool(resolveCommandPool)
            aiBuffer.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
            aiBuffer.commandBufferCount(2)

            val pBuffer = stack.callocPointer(2)
            assertVkSuccess(
                vkAllocateCommandBuffers(vkDevice, aiBuffer, pBuffer),
                "AllocateCommandBuffers", "resolve eye images"
            )
            this.resolveCommandBuffer = VkCommandBuffer(pBuffer[0], vkDevice)
            this.screenshotCommandBuffer = VkCommandBuffer(pBuffer[1], vkDevice)

            val biCommand = VkCommandBufferBeginInfo.calloc(stack)
            biCommand.`sType$Default`()
            biCommand.flags(0)

            assertVkSuccess(
                vkBeginCommandBuffer(this.resolveCommandBuffer, biCommand),
                "BeginCommandBuffer", "resolve eye images"
            )

            fun fillSrr(srr: VkImageSubresourceRange) {
                srr.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                srr.baseMipLevel(0)
                srr.baseArrayLayer(0)
                srr.levelCount(1)
                srr.layerCount(1)
            }

            val pImageSourceBarriers = VkImageMemoryBarrier.calloc(2, stack)
            for ((index, image) in arrayOf(this.leftSourceImage, this.rightSourceImage).withIndex()) {
                val imageSourceBarrier = pImageSourceBarriers[index]
                imageSourceBarrier.`sType$Default`()
                imageSourceBarrier.srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                imageSourceBarrier.dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT)
                imageSourceBarrier.oldLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                imageSourceBarrier.newLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)
                imageSourceBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                imageSourceBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                imageSourceBarrier.image(image.handle)
                imageSourceBarrier.subresourceRange(::fillSrr)
            }

            val pImageResolveBarriers = VkImageMemoryBarrier.calloc(2, stack)
            for ((index, image) in arrayOf(this.leftResolvedImage, this.rightResolvedImage).withIndex()) {
                val imageResolveBarrier = pImageResolveBarriers[index]
                imageResolveBarrier.`sType$Default`()
                imageResolveBarrier.srcAccessMask(0)
                imageResolveBarrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                imageResolveBarrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)
                imageResolveBarrier.newLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                imageResolveBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                imageResolveBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                imageResolveBarrier.image(image.handle)
                imageResolveBarrier.subresourceRange(::fillSrr)
            }

            vkCmdPipelineBarrier(
                this.resolveCommandBuffer, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
                0, null, null, pImageSourceBarriers
            )
            vkCmdPipelineBarrier(
                this.resolveCommandBuffer, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
                0, null, null, pImageResolveBarriers
            )

            fun fillSrl(srl: VkImageSubresourceLayers) {
                srl.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                srl.mipLevel(0)
                srl.baseArrayLayer(0)
                srl.layerCount(1)
            }

            val width = this.leftSourceImage.width
            val height = this.rightSourceImage.height

            if (renderImageInfo.sampleCountBit != VK_SAMPLE_COUNT_1_BIT) {
                val pResolve = VkImageResolve.calloc(stack)
                pResolve.srcSubresource(::fillSrl)
                pResolve.srcOffset { offset -> offset.set(0, 0, 0) }
                pResolve.dstSubresource(::fillSrl)
                pResolve.dstOffset { offset -> offset.set(0, 0, 0) }
                pResolve.extent { extent -> extent.set(width, height, 1) }

                vkCmdResolveImage(
                    this.resolveCommandBuffer,
                    this.leftSourceImage.handle, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                    this.leftResolvedImage.handle, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    pResolve
                )
                vkCmdResolveImage(
                    this.resolveCommandBuffer,
                    this.rightSourceImage.handle, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                    this.rightResolvedImage.handle, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    pResolve
                )
            } else {
                val pCopies = VkImageCopy.calloc(1, stack)
                val pCopy = pCopies[0]
                pCopy.srcSubresource(::fillSrl)
                pCopy.srcOffset { offset -> offset.set(0, 0, 0) }
                pCopy.dstSubresource(::fillSrl)
                pCopy.dstOffset { offset -> offset.set(0, 0, 0) }
                pCopy.extent { extent -> extent.set(width, height, 1) }

                vkCmdCopyImage(
                    this.resolveCommandBuffer,
                    this.leftSourceImage.handle, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                    this.leftResolvedImage.handle, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    pCopies
                )
                vkCmdCopyImage(
                    this.resolveCommandBuffer,
                    this.rightSourceImage.handle, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                    this.rightResolvedImage.handle, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    pCopies
                )
            }

            for ((index, image) in arrayOf(this.leftResolvedImage, this.rightResolvedImage).withIndex()) {
                val imageResolveBarrier = pImageResolveBarriers[index]
                imageResolveBarrier.`sType$Default`()
                imageResolveBarrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                imageResolveBarrier.dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT)
                imageResolveBarrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                imageResolveBarrier.newLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)
                imageResolveBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                imageResolveBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                imageResolveBarrier.image(image.handle)
                imageResolveBarrier.subresourceRange(::fillSrr)
            }
            vkCmdPipelineBarrier(
                this.resolveCommandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
                0, null, null, pImageResolveBarriers
            )

            assertVkSuccess(
                vkEndCommandBuffer(this.resolveCommandBuffer),
                "EndCommandBuffer", "resolve eye images"
            )

            assertVkSuccess(
                vkBeginCommandBuffer(this.screenshotCommandBuffer, biCommand),
                "BeginCommandBuffer", "screenshot"
            )

            val screenshotRegions = VkBufferImageCopy.calloc(1, stack)
            val screenshotRegion = screenshotRegions[0]
            // bufferOffset will be set later
            screenshotRegion.bufferRowLength(0)
            screenshotRegion.bufferImageHeight(0)
            screenshotRegion.imageSubresource(::fillSrl)
            screenshotRegions.imageOffset { it.set(0, 0, 0) }
            screenshotRegion.imageExtent { it.set(leftResolvedImage.width, leftResolvedImage.height, 1) }

            screenshotRegion.bufferOffset(leftScreenshotStagingBuffer.offset)
            vkCmdCopyImageToBuffer(
                screenshotCommandBuffer, leftResolvedImage.handle, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                leftScreenshotStagingBuffer.buffer.handle, screenshotRegions
            )

            screenshotRegion.bufferOffset(rightScreenshotStagingBuffer.offset)
            vkCmdCopyImageToBuffer(
                screenshotCommandBuffer, rightResolvedImage.handle, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                rightScreenshotStagingBuffer.buffer.handle, screenshotRegions
            )

            assertVkSuccess(
                vkEndCommandBuffer(screenshotCommandBuffer), "EndCommandBuffer", "screenshot"
            )

            val ciFence = VkFenceCreateInfo.calloc(stack)
            ciFence.`sType$Default`()

            val pFence = stack.callocLong(1)
            assertVkSuccess(
                vkCreateFence(vkDevice, ciFence, null, pFence),
                "CreateFence", "resolve eye images"
            )
            this.fence = pFence[0]
        }
    }

    fun resolve(vkDevice: VkDevice, queueManager: QueueManager, waitSemaphore: Long, takeScreenshot: Boolean) {
        stackPush().use { stack ->
            val pSubmitInfo = VkSubmitInfo.calloc(1, stack)
            pSubmitInfo.`sType$Default`()
            pSubmitInfo.waitSemaphoreCount(1)
            pSubmitInfo.pWaitSemaphores(stack.longs(waitSemaphore))
            pSubmitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
            pSubmitInfo.pCommandBuffers(stack.pointers(this.resolveCommandBuffer.address()))
            // No signal semaphores because we use a fence instead

            queueManager.generalQueueFamily.getRandomPriorityQueue().submit(pSubmitInfo, this.fence)
            assertVkSuccess(
                vkWaitForFences(vkDevice, stack.longs(this.fence), true, -1),
                "WaitForFences", "resolve eye images"
            )
            assertVkSuccess(
                vkResetFences(vkDevice, stack.longs(this.fence)), "ResetFences", "resolve eye images"
            )

            if (takeScreenshot) {
                pSubmitInfo.waitSemaphoreCount(0)
                pSubmitInfo.pCommandBuffers(stack.pointers(screenshotCommandBuffer.address()))

                queueManager.generalQueueFamily.getRandomPriorityQueue().submit(pSubmitInfo, this.fence)
                assertVkSuccess(
                    vkWaitForFences(vkDevice, stack.longs(this.fence), true, -1),
                    "WaitForFences", "screenshot"
                )
                assertVkSuccess(
                    vkResetFences(vkDevice, stack.longs(this.fence)), "ResetFences", "screenshot"
                )

                for ((resolvedImage, screenshotHostBuffer, suffix) in arrayOf(
                    Triple(leftResolvedImage, leftScreenshotHostBuffer, "Left"),
                    Triple(rightResolvedImage, rightScreenshotHostBuffer, "Right")
                )
                ) {
                    val screenshot = BufferedImage(resolvedImage.width, resolvedImage.height, TYPE_INT_ARGB)
                    for (x in 0 until resolvedImage.width) {
                        for (y in 0 until resolvedImage.height) {
                            val bufferIndex = 4 * (x + y * resolvedImage.width)
                            val red = screenshotHostBuffer[bufferIndex].toInt() and 0xFF
                            val green = screenshotHostBuffer[bufferIndex + 1].toInt() and 0xFF
                            val blue = screenshotHostBuffer[bufferIndex + 2].toInt() and 0xFF
                            val alpha = screenshotHostBuffer[bufferIndex + 3].toInt() and 0xFF
                            val color = Color(red, green, blue, alpha)
                            screenshot.setRGB(x, y, color.rgb)
                        }
                    }

                    ImageIO.write(screenshot, "PNG", File("screenshot$suffix.png"))
                }
            }
        }
    }

    fun destroy(vkDevice: VkDevice) {
        vkDestroyCommandPool(vkDevice, resolveCommandPool, null)
        vkDestroyFence(vkDevice, fence, null)
    }
}
