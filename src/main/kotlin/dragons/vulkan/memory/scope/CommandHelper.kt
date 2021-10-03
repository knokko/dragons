package dragons.vulkan.memory.scope

import dragons.vulkan.memory.VulkanBuffer
import dragons.vulkan.memory.VulkanImage
import dragons.vulkan.memory.claim.ImageMemoryClaim
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
    val copySemaphore: Long,
    /**
     * For transitioning the images from the transfer destination layout to the desired layout, and to transfer
     * queue family ownership of the device buffers and images.
     */
    private val finalTransitionBuffer: VkCommandBuffer
    // TODO Destroy everything once finished
) {
    fun performInitialTransition(images: Map<ImageMemoryClaim, VulkanImage>, description: String) {
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

            for ((claim, image) in images) {

                val needsPrefill = claim.prefill != null
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
                    imageBarrier.image(image.handle)
                    imageBarrier.subresourceRange().aspectMask(claim.aspectMask)

                    vkCmdPipelineBarrier(
                        initialTransitionBuffer, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, dstStageMask,
                        0, null, null, imageBarriers
                    )
                }
            }

            assertVkSuccess(
                vkEndCommandBuffer(initialTransitionBuffer), "EndCommandBuffer",
                "Scope $description: initial image transition"
            )
        }
    }

    fun performStagingCopy(
        tempStagingBuffer: Long, stagingPlacementMap: Map<QueueFamily?, StagingPlacements>,
        claimsToImageMap: Map<ImageMemoryClaim, VulkanImage>, imageClaimFilter: (ImageMemoryClaim) -> Boolean,
        queueFamilyToBufferMap: Map<QueueFamily?, VulkanBuffer>,
        ownQueueFamily: QueueFamily, description: String
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
            val bufferBarriers = VkBufferMemoryBarrier.calloc(1, stack)
            val imageBarriers = VkImageMemoryBarrier.calloc(1, stack)

            for ((queueFamily, placements) in stagingPlacementMap) {
                if (placements.totalBufferSize > 0L) {

                    val copyRegion = bufferCopyRegions[0]
                    copyRegion.srcOffset(placements.externalOffset + placements.internalPlacements.prefilledBufferStagingOffset)
                    copyRegion.dstOffset(placements.internalPlacements.prefilledBufferDeviceOffset)
                    copyRegion.size(placements.totalBufferSize)

                    val deviceBuffer = queueFamilyToBufferMap[queueFamily]!!
                    vkCmdCopyBuffer(copyBuffer, tempStagingBuffer, deviceBuffer.handle, bufferCopyRegions)

                    // If another exclusive queue family needs the buffer, we need to release our ownership
                    if (queueFamily != null && queueFamily != ownQueueFamily) {

                        val bufferBarrier = bufferBarriers[0]
                        bufferBarrier.sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
                        bufferBarrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                        bufferBarrier.dstAccessMask(0)
                        bufferBarrier.srcQueueFamilyIndex(ownQueueFamily.index)
                        bufferBarrier.dstQueueFamilyIndex(queueFamily.index)
                        bufferBarrier.buffer(deviceBuffer.handle)
                        bufferBarrier.offset(0)
                        bufferBarrier.size(VK_WHOLE_SIZE)

                        vkCmdPipelineBarrier(
                            copyBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                            0, null, bufferBarriers, null
                        )
                    }
                }

                for (placedImageClaim in placements.internalPlacements.prefilledImageClaims.filter { imageClaimFilter(it.claim) }) {
                    val deviceImage = claimsToImageMap[placedImageClaim.claim]!!

                    val copyRegion = imageCopyRegions[0]
                    // TODO Ensure that this is a multiple of 4
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
                        copyBuffer, tempStagingBuffer, deviceImage.handle, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, imageCopyRegions
                    )

                    val needsOwnershipTransfer = queueFamily != null && queueFamily != ownQueueFamily
                    val needsLayoutTransition = placedImageClaim.claim.initialLayout != VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL

                    if (needsOwnershipTransfer || needsLayoutTransition) {
                        val imageBarrier = imageBarriers[0]
                        imageBarrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                        imageBarrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                        imageBarrier.dstAccessMask(if (needsOwnershipTransfer) { 0 } else { placedImageClaim.claim.accessMask })
                        imageBarrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                        imageBarrier.newLayout(placedImageClaim.claim.initialLayout)
                        if (needsOwnershipTransfer) {
                            imageBarrier.srcQueueFamilyIndex(ownQueueFamily.index)
                            imageBarrier.dstQueueFamilyIndex(queueFamily!!.index)
                        } else {
                            imageBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                            imageBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                        }
                        imageBarrier.image(deviceImage.handle)
                        imageBarrier.subresourceRange { ssr ->
                            // TODO Add support for multiple mip levels and/or array layers
                            ssr.aspectMask(placedImageClaim.claim.aspectMask)
                            ssr.baseMipLevel(0)
                            ssr.levelCount(1)
                            ssr.baseArrayLayer(0)
                            ssr.layerCount(1)
                        }

                        vkCmdPipelineBarrier(
                            copyBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                            0, null, null, imageBarriers
                        )
                    }
                }
            }

            assertVkSuccess(
                vkEndCommandBuffer(copyBuffer), "EndCommandBuffer",
                "Scope $description: staging transfer"
            )

            val siCopies = VkSubmitInfo.calloc(1, stack)
            val siCopy = siCopies[0]
            siCopy.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
            siCopy.waitSemaphoreCount(1)
            siCopy.pWaitSemaphores(stack.longs(initialTransitionSemaphore))
            // TODO Figure out what the right pipeline stage would be
            siCopy.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT))
            siCopy.pCommandBuffers(stack.pointers(copyBuffer.address()))
            siCopy.pSignalSemaphores(stack.longs(copySemaphore))

            ownQueueFamily.getRandomBackgroundQueue().submit(siCopies, VK_NULL_HANDLE)
        }
    }

    fun acquireOwnership(
        claimsToImageMap: Map<ImageMemoryClaim, VulkanImage>,
        queueFamilyToBufferMap: Map<QueueFamily?, VulkanBuffer>,
        ownQueueFamily: QueueFamily, queueManager: QueueManager, description: String
    ) {
        stackPush().use { stack ->
            val biTransfers = VkCommandBufferBeginInfo.calloc(stack)
            biTransfers.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
            biTransfers.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)

            assertVkSuccess(
                vkBeginCommandBuffer(finalTransitionBuffer, biTransfers),
                "BeginCommandBuffer", "Scope $description: final ownership transfer"
            )

            if (ownQueueFamily != queueManager.getTransferQueueFamily()) {
                // TODO Acquire buffer ownership
                val bufferBarriers = VkBufferMemoryBarrier.calloc(1, stack)
                val bufferBarrier = bufferBarriers[0]
                bufferBarrier.sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
                bufferBarrier.srcAccessMask(0)
                bufferBarrier.dstAccessMask(ehm)
                bufferBarrier.srcQueueFamilyIndex(queueManager.getTransferQueueFamily().index)
                bufferBarrier.dstQueueFamilyIndex(ownQueueFamily.index)
                bufferBarrier.buffer(queueFamilyToBufferMap[ownQueueFamily]!!.handle)
                bufferBarrier.offset(0)
                bufferBarrier.size(VK_WHOLE_SIZE)

                vkCmdPipelineBarrier(
                    finalTransitionBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                    0, null, bufferBarriers, null
                )
            }

            val imageBarriers = VkImageMemoryBarrier.calloc(1, stack)
            for ((claim, image) in claimsToImageMap) {

                if (claim.prefill != null && claim.queueFamily == ownQueueFamily) {
                    val usedTransferQueueFamily = if (requiresGraphicsFamily(claim)) {
                        queueManager.generalQueueFamily
                    } else {
                        queueManager.getTransferQueueFamily()
                    }

                    if (usedTransferQueueFamily != claim.queueFamily) {
                        val imageBarrier = imageBarriers[0]
                        imageBarrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                        imageBarrier.srcAccessMask(0)
                        imageBarrier.dstAccessMask(claim.accessMask)
                        imageBarrier.oldLayout(claim.initialLayout) // TODO I'm not sure about this one
                        imageBarrier.newLayout(claim.initialLayout)
                        imageBarrier.srcQueueFamilyIndex(usedTransferQueueFamily.index)
                        imageBarrier.dstQueueFamilyIndex(ownQueueFamily.index)
                        imageBarrier.image(image.handle)
                        imageBarrier.subresourceRange { ssr ->
                            // TODO Handle multiple mip levels and/or array layers
                            ssr.aspectMask(claim.aspectMask)
                            ssr.baseMipLevel(0)
                            ssr.levelCount(1)
                            ssr.baseArrayLayer(0)
                            ssr.layerCount(1)
                        }
                    }
                }
            }

            vkEndCommandBuffer(finalTransitionBuffer)

            // TODO Handle all transition/copy barriers with fences rather than semaphores
            val siTransfers = VkSubmitInfo.calloc(1, stack)
            val siTransfer = siTransfers[0]
            siTransfer.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
            siTransfer.pCommandBuffers(stack.pointers(finalTransitionBuffer.address()))

            // TODO Create a fence that the FamiliesCommands should eventually wait on

            // TODO Submit
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
                    "CreateSemaphore", "Scope $description: initial image layout transition"
                )
                val initialTransitionSemaphore = pSemaphore[0]

                assertVkSuccess(
                    vkCreateSemaphore(vkDevice, ciSemaphore, null, pSemaphore),
                    "CreateSemaphore", "Scope $description: initial image layout transition"
                )
                val copySemaphore = pSemaphore[0]

                return FamilyCommands(
                    commandPool = commandPool,
                    initialTransitionBuffer = VkCommandBuffer(pCommandBuffers[0], vkDevice),
                    initialTransitionSemaphore = initialTransitionSemaphore,
                    copyBuffer = VkCommandBuffer(pCommandBuffers[1], vkDevice),
                    copySemaphore = copySemaphore,
                    finalTransitionBuffer = VkCommandBuffer(pCommandBuffers[2], vkDevice)
                )
            }
        }
    }
}

fun requiresGraphicsFamily(claim: ImageMemoryClaim) = claim.aspectMask == VK_IMAGE_ASPECT_DEPTH_BIT || claim.aspectMask == VK_IMAGE_ASPECT_STENCIL_BIT

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

    fun performInitialTransition(
        images: Map<ImageMemoryClaim, VulkanImage>, queueManager: QueueManager, description: String
    ) {
        for ((queueFamily, familyCommands) in familyMap) {
            familyCommands.performInitialTransition(images.filter { (claim, _) ->
                if (claim.prefill != null) {
                    /**
                     * If the image needs to be prefilled, the layout transition should be done by the same queue
                     * family that will perform the staging transfer. This is either the general or transfer queue
                     * family (depending on the aspect mask of the claim).
                     */
                    if (requiresGraphicsFamily(claim)) {
                        queueFamily == queueManager.generalQueueFamily
                    } else {
                        queueFamily == queueManager.getTransferQueueFamily()
                    }
                } else {
                    /*
                     * If the image doesn't need to be prefilled, the image sharing mode determines which queue family
                     * should perform the layout transition. If the exclusive image sharing mode is used, the image
                     * layout must be performed by the queue family for which the image was claimed.
                     *
                     * If the concurrent image sharing mode is used, it doesn't matter which queue family performs the
                     * layout transition. In this case, we pick the transfer queue family.
                     */
                    if (claim.queueFamily != null) {
                        queueFamily == claim.queueFamily
                    } else {
                        queueFamily == queueManager.getTransferQueueFamily()
                    }
                }
            }, description)
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

    fun performStagingCopy(
        tempStagingBuffer: Long,
        stagingPlacementMap: Map<QueueFamily?, StagingPlacements>,
        claimsToImageMap: Map<ImageMemoryClaim, VulkanImage>,
        queueFamilyToBufferMap: Map<QueueFamily?, VulkanBuffer>,
        queueManager: QueueManager, description: String
    ) {
        // The general queue family is needed for depth-stencil transfers
        familyMap[queueManager.generalQueueFamily]!!.performStagingCopy(
            tempStagingBuffer, stagingPlacementMap, claimsToImageMap, {
                    claim -> requiresGraphicsFamily(claim)
            }, queueFamilyToBufferMap, queueManager.generalQueueFamily, description
        )

        // The transfer queue family should be used for everything else.
        familyMap[queueManager.getTransferQueueFamily()]!!.performStagingCopy(
            tempStagingBuffer, stagingPlacementMap, claimsToImageMap, {
                claim -> !requiresGraphicsFamily(claim)
            }, queueFamilyToBufferMap, queueManager.getTransferQueueFamily(), description
        )

        // Finally, each image and buffer should be acquired by the right queue family (if needed)
        for ((queueFamily, familyCommands) in familyMap) {
            familyCommands.acquireOwnership(
                claimsToImageMap, queueFamilyToBufferMap, queueFamily, queueManager, description
            )
        }

        // TODO Wait for completion
    }
}