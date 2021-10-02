package dragons.vulkan.memory.scope

import dragons.vulkan.memory.claim.PlacedQueueFamilyClaims
import dragons.vulkan.memory.claim.QueueFamilyClaims
import dragons.vulkan.memory.claim.placeMemoryClaims
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

internal class StagingPlacements(
    val externalOffset: Long,
    val totalBufferSize: Long,
    val internalPlacements: PlacedQueueFamilyClaims
)

internal suspend fun fillStagingBuffer(
    vkDevice: VkDevice, scope: CoroutineScope, stack: MemoryStack,
    tempStagingMemory: Long, familyClaimsMap: Map<QueueFamily?, QueueFamilyClaims>, startStagingAddress: Long,
    description: String
): Map<QueueFamily?, StagingPlacements> {
    val logger = getLogger("Vulkan")
    logger.info("Scope $description: Filling temporary staging combined memory...")
    val stagingFillTasks = ArrayList<Deferred<Unit>>(familyClaimsMap.values.sumOf {
        it.claims.prefilledBufferClaims.size + it.claims.prefilledImageClaims.size
    })

    var stagingOffset = 0L

    val placementMap = mutableMapOf<QueueFamily?, StagingPlacements>()
    for ((queueFamily, claims) in familyClaimsMap.entries) {

        val placements = placeMemoryClaims(claims)
        placementMap[queueFamily] = StagingPlacements(
            stagingOffset, placements.prefilledBufferClaims.sumOf { it.claim.size.toLong() }, placements
        )

        for (prefilledClaim in placements.prefilledBufferClaims) {
            val claimedStagingPlace = MemoryUtil.memByteBuffer(
                startStagingAddress + stagingOffset + placements.prefilledBufferStagingOffset + prefilledClaim.offset,
                prefilledClaim.claim.size
            )
            stagingFillTasks.add(scope.async { prefilledClaim.claim.prefill(claimedStagingPlace) })
        }

        for (prefilledClaim in placements.prefilledImageClaims) {
            val claimedStagingPlace = MemoryUtil.memByteBuffer(
                startStagingAddress + stagingOffset + placements.prefilledImageStagingOffset + prefilledClaim.offset,
                prefilledClaim.claim.getByteSize()
            )
            stagingFillTasks.add(scope.async { prefilledClaim.claim.prefill(claimedStagingPlace) })
        }

        stagingOffset += claims.tempStagingSize
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
