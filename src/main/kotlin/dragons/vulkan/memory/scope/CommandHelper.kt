package dragons.vulkan.memory.scope

import dragons.vulkan.memory.claim.ImageMemoryClaim
import dragons.vulkan.memory.claim.PrefilledImageMemoryClaim
import dragons.vulkan.queue.QueueFamily
import dragons.vulkan.queue.QueueManager
import dragons.vulkan.util.assertVkSuccess
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.*

internal class FamilyCommands(
    private val commandPool: Long,
    /** For transitioning the images from the undefined layout to the desired initial layout */
    val initialTransitionBuffer: VkCommandBuffer,
    val initialTransitionSemaphore: Long,
    /** For copying the staging buffer to the device buffers and images */
    private val copyBuffer: VkCommandBuffer,
    /**
     * For transitioning the images from the transfer destination layout to the desired layout, and to transfer
     * queue family ownership of the device buffers and images.
     */
    private val finalTransitionBuffer: VkCommandBuffer
) {
    fun performInitialTransition(images: Collection<Pair<Long, ImageMemoryClaim>>) {
        stackPush().use { stack ->
            val biCommandBuffer = VkCommandBufferBeginInfo.calloc(stack)
            biCommandBuffer.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
            biCommandBuffer.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)

            assertVkSuccess(
                vkBeginCommandBuffer(initialTransitionBuffer, biCommandBuffer),
                "BeginCommandBuffer", "initial image layout transition"
            )

            val imageBarriers = VkImageMemoryBarrier.calloc(1, stack)
            val imageBarrier = imageBarriers[0]
            imageBarrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
            imageBarrier.srcAccessMask(0)
            // dstAccessMask will be filled per image
            imageBarrier.oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
            // newLayout will be filled per image
            imageBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            imageBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            // image will be filled per image
            imageBarrier.subresourceRange { ssr ->
                // TODO Add support for multiple mip levels and array layers
                // aspect mask will be filled per image
                ssr.baseMipLevel(0)
                ssr.levelCount(1)
                ssr.baseArrayLayer(0)
                ssr.layerCount(1)
            }

            for ((image, claim) in images) {

                val needsPrefill = claim is PrefilledImageMemoryClaim
                val needsTransition = needsPrefill || claim.initialLayout != VK_IMAGE_LAYOUT_UNDEFINED

                if (needsTransition) {
                    val dstStageMask: Int
                    if (needsPrefill) {
                        imageBarrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                        dstStageMask = VK_PIPELINE_STAGE_TRANSFER_BIT
                    } else {
                        imageBarrier.dstAccessMask(claim.accessMask)
                        dstStageMask = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT
                    }
                    imageBarrier.newLayout(claim.initialLayout)
                    imageBarrier.image(image)
                    imageBarrier.subresourceRange().aspectMask(claim.aspectMask)

                    vkCmdPipelineBarrier(
                        initialTransitionBuffer, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, dstStageMask,
                        0, null, null, imageBarriers
                    )
                }
            }

            assertVkSuccess(
                vkEndCommandBuffer(initialTransitionBuffer), "EndCommandBuffer", "initial image transition"
            )
        }
    }

    fun performStagingCopy(
        tempStagingBuffer: Long, stagingPlacementMap: Map<QueueFamily?, StagingPlacements>, description: String
    ) {
        stackPush().use { stack ->
            val biCopyCommands = VkCommandBufferBeginInfo.calloc(stack)
            biCopyCommands.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
            biCopyCommands.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)

            assertVkSuccess(
                vkBeginCommandBuffer(copyBuffer, biCopyCommands),
                "BeginCommandBuffer", "Scope $description: staging transfer"
            )

            val bufferCopyRegions = VkBufferCopy.calloc(1, stack)
            val imageCopyRegions = VkBufferImageCopy.calloc(1, stack)
            for (placements in stagingPlacementMap.values) {
                if (placements.totalBufferSize > 0L) {

                    val copyRegion = bufferCopyRegions[0]
                    copyRegion.srcOffset(placements.externalOffset + placements.internalPlacements.prefilledBufferStagingOffset)
                    copyRegion.dstOffset(placements.internalPlacements.prefilledBufferDeviceOffset)
                    copyRegion.size(placements.totalBufferSize)

                    vkCmdCopyBuffer(copyBuffer, tempStagingBuffer, deviceBuffer, bufferCopyRegions)
                }

                for (placedImageClaim in placements.internalPlacements.prefilledImageClaims) {
                    val deviceImage = claimsToImageMap[placedImageClaim.claim]!!

                    val copyRegion = imageCopyRegions[0]
                    copyRegion.bufferOffset(
                        placements.externalOffset + placements.internalPlacements.prefilledImageStagingOffset
                                + placedImageClaim.offset
                    )
                    copyRegion.bufferRowLength(0)
                    copyRegion.bufferImageHeight(0)
                    copyRegion.imageSubresource { subresource ->
                        subresource.aspectMask(placedImageClaim.claim.aspectMask)
                        // TODO Handle multiple mip levels
                        subresource.mipLevel(0)
                        subresource.baseArrayLayer(0)
                        // TODO Double-check this once using more than 1 layer is possible
                        val layerCount = 1
                        subresource.layerCount(layerCount)
                    }
                    copyRegion.imageOffset { offset ->
                        offset.set(0, 0, 0)
                    }
                    copyRegion.imageExtent { extent ->
                        extent.set(placedImageClaim.claim.width, placedImageClaim.claim.height, 1)
                    }
                    vkCmdCopyBufferToImage(
                        copyBuffer, tempStagingBuffer, deviceImage, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, imageCopyRegions
                    )
                }
            }

            assertVkSuccess(
                vkEndCommandBuffer(copyBuffer), "EndCommandBuffer", "Scope $description: staging transfer"
            )

            // TODO Wait until its completed
        }
    }

    companion object {
        fun construct(vkDevice: VkDevice, queueFamily: QueueFamily, description: String): FamilyCommands {
            stackPush().use { stack ->
                val ciCommandPool = VkCommandPoolCreateInfo.calloc(stack)
                ciCommandPool.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                ciCommandPool.flags(VK_COMMAND_POOL_CREATE_TRANSIENT_BIT)
                ciCommandPool.queueFamilyIndex(queueFamily.index)

                val pCommandPool = stack.callocLong(1)
                assertVkSuccess(
                    vkCreateCommandPool(vkDevice, ciCommandPool, null, pCommandPool),
                    "CreateCommandPool", description
                )
                val commandPool = pCommandPool[0]

                val aiCommandBuffers = VkCommandBufferAllocateInfo.calloc(stack)
                aiCommandBuffers.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                aiCommandBuffers.commandPool(commandPool)
                aiCommandBuffers.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                aiCommandBuffers.commandBufferCount(3)

                val pCommandBuffers = stack.callocPointer(3)
                assertVkSuccess(
                    vkAllocateCommandBuffers(vkDevice, aiCommandBuffers, pCommandBuffers),
                    "AllocateCommandBuffers", description
                )

                val ciSemaphore = VkSemaphoreCreateInfo.calloc(stack)
                ciSemaphore.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)

                val pSemaphore = stack.callocLong(1)
                assertVkSuccess(
                    vkCreateSemaphore(vkDevice, ciSemaphore, null, pSemaphore),
                    "CreateSemaphore", "initial image layout transition"
                )
                val initialTransitionSemaphore = pSemaphore[0]

                return FamilyCommands(
                    commandPool = commandPool,
                    initialTransitionBuffer = VkCommandBuffer(pCommandBuffers[0], vkDevice),
                    initialTransitionSemaphore = initialTransitionSemaphore,
                    copyBuffer = VkCommandBuffer(pCommandBuffers[1], vkDevice),
                    finalTransitionBuffer = VkCommandBuffer(pCommandBuffers[2], vkDevice)
                )
            }
        }
    }
}

internal class FamiliesCommands(
    private val familyMap: Map<QueueFamily, FamilyCommands>
) {
    companion object {
        fun construct(vkDevice: VkDevice, queueManager: QueueManager, description: String): FamiliesCommands {

            val familyMap = mutableMapOf<QueueFamily, FamilyCommands>()
            for (queueFamily in queueManager.allQueueFamilies) {
                familyMap[queueFamily] = FamilyCommands.construct(vkDevice, queueFamily, description)
            }

            return FamiliesCommands(familyMap.toMap())
        }
    }

    fun performInitialTransition(images: Collection<Pair<Long, ImageMemoryClaim>>, queueManager: QueueManager) {
        for ((queueFamily, familyCommands) in familyMap.entries) {
            // TODO Wait... we need to distinguish the case of prefilled images vs uninitialized images
            /*
             * For the exclusive images, the right queue family must perform the transition from the undefined
             * image layout to the desired initial image layout.
             * For the concurrent images, it doesn't matter which queue family is used for it, but some choice has
             * to be made. We choose the transfer queue family (and the general queue family as back-up).
             */
            familyCommands.performInitialTransition(images.filter {
                it.second.queueFamily == queueFamily ||
                        (it.second.queueFamily == null && queueFamily == queueManager.getTransferQueueFamily())
            })
        }

        stackPush().use { stack ->
            val pCommandBuffer = stack.callocPointer(1)
            val pSignalSemaphore = stack.callocLong(1)

            val pSubmits = VkSubmitInfo.calloc(1, stack)

            val submission = pSubmits[0]
            submission.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
            submission.pCommandBuffers(pCommandBuffer)
            submission.pSignalSemaphores(pSignalSemaphore)

            for ((queueFamily, familyCommands) in familyMap.entries) {
                pCommandBuffer.put(0, familyCommands.initialTransitionBuffer)
                pSignalSemaphore.put(0, familyCommands.initialTransitionSemaphore)
                queueFamily.getRandomBackgroundQueue().submit(pSubmits, VK_NULL_HANDLE)
            }
        }
    }

    fun performStagingCopy(stagingPlacementMap: Map<QueueFamily?, StagingPlacements>, queueManager: QueueManager) {
        // TODO Use some filtering to ensure each queue family only copies the parts it is supposed to copy
        // The general queue family is needed for depth-stencil transfers
        familyMap[queueManager.generalQueueFamily]!!.performStagingCopy(stagingPlacementMap)
        // The transfer queue family should be used for everything else.
        familyMap[queueManager.getTransferQueueFamily()]!!.performStagingCopy(stagingPlacementMap)
    }
}