package dragons.vulkan.memory.scope

import dragons.vulkan.memory.claim.PlacedQueueFamilyClaims
import dragons.vulkan.queue.QueueFamily
import dragons.vulkan.util.assertVkSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
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
     * The total number of bytes that are reserved for this queue family in the shared staging buffer.
     */
    val totalBufferSize: Long,
    val internalPlacements: PlacedQueueFamilyClaims
)

internal suspend fun fillStagingBuffer(
    vkDevice: VkDevice, scope: CoroutineScope, stack: MemoryStack,
    tempStagingMemory: Long, familyClaimsMap: Map<QueueFamily?, PlacedQueueFamilyClaims>, startStagingAddress: Long,
    description: String
): Map<QueueFamily?, StagingPlacements> {
    val logger = getLogger("Vulkan")
    logger.info("Scope $description: Filling temporary staging combined memory...")
    val stagingFillTasks = ArrayList<Deferred<Unit>>(familyClaimsMap.values.sumOf {
        it.prefilledBufferClaims.size + it.prefilledImageClaims.size
    })

    var stagingOffset = 0L

    val placementMap = mutableMapOf<QueueFamily?, StagingPlacements>()
    for ((queueFamily, placements) in familyClaimsMap.entries) {

        placementMap[queueFamily] = StagingPlacements(
            stagingOffset, placements.prefilledBufferClaims.sumOf { it.claim.size.toLong() }, placements
        )

        for (prefilledClaim in placements.prefilledBufferClaims) {
            val claimedStagingPlace = MemoryUtil.memByteBuffer(
                startStagingAddress + stagingOffset + placements.prefilledBufferStagingOffset + prefilledClaim.offset,
                prefilledClaim.claim.size
            )
            stagingFillTasks.add(scope.async { prefilledClaim.claim.prefill!!(claimedStagingPlace) })
        }

        for (prefilledClaim in placements.prefilledImageClaims) {
            val claimedStagingPlace = MemoryUtil.memByteBuffer(
                startStagingAddress + stagingOffset + placements.prefilledImageStagingOffset + prefilledClaim.offset,
                prefilledClaim.claim.getByteSize()
            )
            stagingFillTasks.add(scope.async { prefilledClaim.claim.prefill!!(claimedStagingPlace) })
        }

        stagingOffset += placements.prefilledBufferClaims.sumOf { it.claim.size } + placements.prefilledImageClaims.sumOf { it.claim.getByteSize() }
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

    return placementMap
}
