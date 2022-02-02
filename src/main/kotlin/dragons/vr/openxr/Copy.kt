package dragons.vr.openxr

import dragons.state.StaticGraphicsState
import dragons.vulkan.memory.VulkanImage
import dragons.vulkan.util.assertVkSuccess
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

private const val DESCRIPTION = "OpenXR swapchain image copy"

internal class SwapchainCopyHelper(
    private val graphicsState: StaticGraphicsState
) {

    private val commandPool: Long
    private val commandBuffer: VkCommandBuffer
    private val fence: Long

    init {
        stackPush().use { stack ->
            val ciCommandPool = VkCommandPoolCreateInfo.calloc(stack)
            ciCommandPool.`sType$Default`()
            ciCommandPool.flags(VK_COMMAND_POOL_CREATE_TRANSIENT_BIT)
            ciCommandPool.queueFamilyIndex(graphicsState.queueManager.generalQueueFamily.index)

            val pCommandPool = stack.callocLong(1)
            assertVkSuccess(
                vkCreateCommandPool(graphicsState.vkDevice, ciCommandPool, null, pCommandPool),
                "CreateCommandPool", DESCRIPTION
            )
            this.commandPool = pCommandPool[0]

            val aiCommandBuffer = VkCommandBufferAllocateInfo.calloc(stack)
            aiCommandBuffer.`sType$Default`()
            aiCommandBuffer.commandPool(this.commandPool)
            aiCommandBuffer.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
            aiCommandBuffer.commandBufferCount(1)

            val pCommandBuffer = stack.callocPointer(1)
            assertVkSuccess(
                vkAllocateCommandBuffers(graphicsState.vkDevice, aiCommandBuffer, pCommandBuffer),
                "AllocateCommandBuffers", DESCRIPTION
            )
            this.commandBuffer = VkCommandBuffer(pCommandBuffer[0], graphicsState.vkDevice)

            val ciFence = VkFenceCreateInfo.calloc(stack)
            ciFence.`sType$Default`()

            val pFence = stack.callocLong(1)
            assertVkSuccess(
                vkCreateFence(graphicsState.vkDevice, ciFence, null, pFence),
                "CreateFence", DESCRIPTION
            )
            this.fence = pFence[0]
        }
    }

    fun copyToSwapchainImages(
        leftImage: VulkanImage, rightImage: VulkanImage
    ) {
        stackPush().use { stack ->
            assertVkSuccess(
                vkResetCommandPool(graphicsState.vkDevice, this.commandPool, 0),
                "ResetCommandPool", DESCRIPTION
            )

            val biCommandBuffer = VkCommandBufferBeginInfo.calloc(stack)
            biCommandBuffer.`sType$Default`()
            biCommandBuffer.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)

            assertVkSuccess(
                vkBeginCommandBuffer(this.commandBuffer, biCommandBuffer),
                "BeginCommandBuffer", DESCRIPTION
            )

            fun fillSubresource(subresourceLayers: VkImageSubresourceLayers) {
                subresourceLayers.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                subresourceLayers.mipLevel(0)
                subresourceLayers.baseArrayLayer(0)
                subresourceLayers.layerCount(1)
            }

            val pCopyRegion = VkImageCopy.calloc(1, stack)
            val copyRegion = pCopyRegion[0]
            copyRegion.srcSubresource(::fillSubresource)
            // src offset will stay 0
            copyRegion.dstSubresource(::fillSubresource)
            // dst offset will stay 0
            copyRegion.extent { extent ->
                extent.set(leftImage.width, leftImage.height, 1)
            }

            // TODO Wait... what is the initial layout of the swapchain images?
            vkCmdCopyImage(
                this.commandBuffer,
                graphicsState.coreMemory.leftResolveImage.handle,
                VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                leftImage.handle,
                VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                pCopyRegion
            )
            vkCmdCopyImage(
                this.commandBuffer,
                graphicsState.coreMemory.rightResolveImage.handle,
                VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                rightImage.handle,
                VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                pCopyRegion
            )

            assertVkSuccess(
                vkEndCommandBuffer(this.commandBuffer), "EndCommandBuffer", DESCRIPTION
            )

            val pSubmitInfo = VkSubmitInfo.calloc(1, stack)
            val submitInfo = pSubmitInfo[0]
            submitInfo.`sType$Default`()
            // No need to wait on semaphores because the ResolveManager waits until the resolving is finished
            submitInfo.pCommandBuffers(stack.pointers(this.commandBuffer.address()))
            // No need to signal any semaphores because we will wait on a fence

            graphicsState.queueManager.generalQueueFamily.getRandomPriorityQueue().submit(pSubmitInfo, this.fence)

            assertVkSuccess(
                vkWaitForFences(graphicsState.vkDevice, stack.longs(this.fence), true, 10_000_000_000),
                "WaitForFences", DESCRIPTION
            )

            assertVkSuccess(
                vkResetFences(graphicsState.vkDevice, stack.longs(this.fence)),
                "ResetFences", DESCRIPTION
            )
        }
    }

    fun destroy() {
        vkDestroyFence(graphicsState.vkDevice, this.fence, null)
        vkDestroyCommandPool(graphicsState.vkDevice, this.commandPool, null)
    }
}
