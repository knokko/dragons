package dragons.vulkan.memory.scope

import dragons.util.nextMultipleOf
import dragons.vulkan.memory.claim.Placed
import dragons.vulkan.memory.claim.PlacedQueueFamilyClaims
import dragons.vulkan.memory.claim.QueueFamilyClaims
import dragons.vulkan.queue.QueueFamily
import dragons.vulkan.util.assertVkSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memByteBuffer
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkMappedMemoryRange
import org.slf4j.LoggerFactory.getLogger

/**
 * The placements into the shared staging buffer for *a single* queue family.
 */
internal class StagingPlacements(
    /**
     * The number of bytes between the start of the shared staging buffer and the start address of this queue family.
     */
    val externalOffset: Long,
    /**
     * The total number of bytes that are reserved for **both buffers and images** for this queue family in the shared staging buffer.
     */
    val stagingBufferSize: Long,
    /**
     * The total number of bytes that are reserved for **buffers** for this queue family in the shared staging buffer.
     */
    val stagingBufferOnlyBufferSize: Long,
    /**
     * The size (in bytes) that the device local buffer will need.
     *
     * Note: this field doesn't really belong in this class, but it is convenient to put it here (at least for now).
     */
    val deviceBufferSize: Long,
    val internalPlacements: PlacedQueueFamilyClaims
)

/**
 * The placements into the shared staging buffer of *all* queue families.
 */
internal class CombinedStagingPlacements(
    /**
     * The total size (in bytes) of the shared staging buffer
     */
    val totalSize: Long,
    /**
     * The placements *per queue family*
     */
    val queueFamilies: Map<QueueFamily?, StagingPlacements>
)

internal fun determineStagingPlacements(
    familyClaimsMap: Map<QueueFamily?, QueueFamilyClaims>
): CombinedStagingPlacements {

    val familyPlacementMap = mutableMapOf<QueueFamily?, StagingPlacements>()

    var sharedOffset = 0L
    for ((queueFamily, familyClaims) in familyClaimsMap) {
        val externalOffset = sharedOffset
        val claims = familyClaims.claims

        var prefilledBufferFamilyOffset = 0L
        val prefilledBufferClaims = claims.prefilledBufferClaims.map { claim ->
            /**
             * The final offset for the claim will be claimOffset + prefilledBufferDeviceOffset. But, since
             * prefilledBufferDeviceOffset is always 0, this is simply claimOffset.
             *
             * We need to ensure that claimOffset is a multiple of the alignment of the claim.
             */
            val gapSize = nextMultipleOf(claim.alignment.toLong(), prefilledBufferFamilyOffset) - prefilledBufferFamilyOffset
            val claimOffset = prefilledBufferFamilyOffset + gapSize
            prefilledBufferFamilyOffset += claim.size + gapSize
            sharedOffset += claim.size + gapSize
            Placed(claim, claimOffset)
        }
        val totalPrefilledBufferSize = prefilledBufferFamilyOffset

        val prefilledImageClaims = claims.prefilledImageClaims.map { claim ->
            // Ensure VUID-vkCmdCopyBufferToImage-bufferOffset-01558 and VUID-vkCmdCopyBufferToImage-srcImage-04053
            // (In some cases, the image alignment must be 4. In some cases, the image alignment must be *bytesPerPixel*.)
            // If we use an offset that is a multiple of both, we are always safe. Also, this wastes very little space.
            val alignment = if (4 % claim.bytesPerPixel!! == 0) { 4 } else { 4 * claim.bytesPerPixel }
            sharedOffset = nextMultipleOf(alignment.toLong(), sharedOffset)
            val claimOffset = sharedOffset - externalOffset - totalPrefilledBufferSize
            sharedOffset += claim.getStagingByteSize()
            Placed(claim, claimOffset)
        }
        val familyStagingSize = sharedOffset - externalOffset

        var uninitializedBufferFamilyOffset = 0L
        val uninitializedBufferClaims = claims.uninitializedBufferClaims.map { claim ->
            /*
             * The final device offset for the claim will be claimOffset + uninitializedBufferDeviceOffset,
             * which is equal to claimOffset + totalPrefilledBufferSize.
             *
             * We need to ensure that this is a multiple of the claim's alignment.
             */
            val idealClaimFinalOffset = uninitializedBufferFamilyOffset + totalPrefilledBufferSize
            val requiredClaimFinalOffset = nextMultipleOf(claim.alignment.toLong(), idealClaimFinalOffset)
            val gapSize = requiredClaimFinalOffset - idealClaimFinalOffset
            uninitializedBufferFamilyOffset += gapSize

            val claimOffset = uninitializedBufferFamilyOffset
            uninitializedBufferFamilyOffset += claim.size
            Placed(claim, claimOffset)
        }

        var stagingBufferFamilyOffset = 0L
        val stagingBufferClaims = claims.stagingBufferClaims.map { claim ->
            val claimOffset = stagingBufferFamilyOffset
            stagingBufferFamilyOffset += claim.size
            Placed(claim, claimOffset)
        }

        val placedClaims = PlacedQueueFamilyClaims(
            prefilledBufferClaims = prefilledBufferClaims,
            prefilledBufferStagingOffset = 0,
            prefilledBufferDeviceOffset = 0,
            uninitializedBufferClaims = uninitializedBufferClaims,
            uninitializedBufferDeviceOffset = totalPrefilledBufferSize,
            stagingBufferClaims = stagingBufferClaims,
            stagingBufferOffset = 0,
            prefilledImageStagingOffset = totalPrefilledBufferSize,
            prefilledImageClaims = prefilledImageClaims,
            uninitializedImageClaims = claims.uninitializedImageClaims
        )

        familyPlacementMap[queueFamily] = StagingPlacements(
            externalOffset = externalOffset,
            stagingBufferSize = familyStagingSize,
            stagingBufferOnlyBufferSize = totalPrefilledBufferSize,
            deviceBufferSize = uninitializedBufferFamilyOffset + totalPrefilledBufferSize,
            internalPlacements = placedClaims
        )
    }

    return CombinedStagingPlacements(totalSize = sharedOffset, familyPlacementMap)
}

internal suspend fun fillStagingBuffer(
    vkDevice: VkDevice, scope: CoroutineScope, stack: MemoryStack,
    tempStagingMemory: Long, stagingPlacements: CombinedStagingPlacements, startStagingAddress: Long,
    description: String
) {
    val logger = getLogger("Vulkan")
    logger.info("Scope $description: Filling temporary staging combined memory...")
    val stagingFillTasks = ArrayList<Deferred<Unit>>(stagingPlacements.queueFamilies.values.sumOf {
        it.internalPlacements.prefilledBufferClaims.size + it.internalPlacements.prefilledImageClaims.size
    })

    for (placements in stagingPlacements.queueFamilies.values) {

        for (placedClaim in placements.internalPlacements.prefilledBufferClaims) {
            val claimedStagingPlace = memByteBuffer(
                startStagingAddress + placements.externalOffset
                        + placements.internalPlacements.prefilledBufferStagingOffset + placedClaim.offset,
                placedClaim.claim.size
            )
            stagingFillTasks.add(scope.async { placedClaim.claim.prefill!!(claimedStagingPlace) })
        }

        for (placedClaim in placements.internalPlacements.prefilledImageClaims) {
            val claimedStagingPlace = memByteBuffer(
                startStagingAddress + placements.externalOffset
                        + placements.internalPlacements.prefilledImageStagingOffset + placedClaim.offset,
                placedClaim.claim.getStagingByteSize()
            )
            stagingFillTasks.add(scope.async { placedClaim.claim.prefill!!(claimedStagingPlace) })
        }
    }

    for (task in stagingFillTasks) {
        task.await()
    }
    logger.info("Scope $description: Filled staging combined memory")

    // There is no guarantee that the buffer is coherent, so I may have to flush it explicitly
    val flushRanges = VkMappedMemoryRange.calloc(1, stack)
    val flushRange = flushRanges[0]
    flushRange.sType(VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE)
    flushRange.memory(tempStagingMemory)
    flushRange.offset(0)
    flushRange.size(VK_WHOLE_SIZE)

    logger.info("Scope $description: Flushing combined staging memory...")
    assertVkSuccess(
        vkFlushMappedMemoryRanges(vkDevice, flushRanges),
        "FlushMappedMemoryRanges", "Scope $description: combined staging"
    )
    logger.info("Scope $description: Flushed combined staging memory; Unmapping it...")
    vkUnmapMemory(vkDevice, tempStagingMemory)
    logger.info("Scope $description: Unmapped combined staging memory")
}
