package dragons.vulkan.memory.claim

import dragons.plugin.interfaces.vulkan.VulkanStaticMemoryUser
import dragons.vulkan.queue.QueueFamily

// TODO Test all methods in this class

internal fun getUsedQueueFamilies(agents: Collection<VulkanStaticMemoryUser.Agent>): Set<QueueFamily?> {
    val usedQueueFamilies = mutableSetOf<QueueFamily?>()
    for (agent in agents) {
        for (bufferClaim in agent.prefilledBuffers) {
            usedQueueFamilies.add(bufferClaim.queueFamily)
        }
        for (bufferClaim in agent.uninitializedBuffers) {
            usedQueueFamilies.add(bufferClaim.queueFamily)
        }
        // TODO Also consider the images
    }

    return usedQueueFamilies
}

internal class QueueFamilyClaims(
    val prefilledBufferClaims: Collection<PrefilledBufferMemoryClaim>,
    val uninitializedBufferClaims: Collection<UninitializedBufferMemoryClaim>
) {
    val stagingSize: Long
    val deviceBufferSize: Long

    init {
        var computeStagingSize = 0L
        for (claim in prefilledBufferClaims) {
            computeStagingSize += claim.size
        }
        stagingSize = computeStagingSize

        var computeDeviceSize = computeStagingSize
        for (claim in uninitializedBufferClaims) {
            computeDeviceSize += claim.size
        }
        deviceBufferSize = computeDeviceSize
    }

    override fun equals(other: Any?): Boolean {
        return if (other is QueueFamilyClaims) {
            prefilledBufferClaims == other.prefilledBufferClaims && uninitializedBufferClaims == other.uninitializedBufferClaims
        } else {
            false
        }
    }
}

internal fun groupMemoryClaims(agents: Collection<VulkanStaticMemoryUser.Agent>): Map<QueueFamily?, QueueFamilyClaims> {
    val usedQueueFamilies = getUsedQueueFamilies(agents)

    val queueFamilyMap = mutableMapOf<QueueFamily?, QueueFamilyClaims>()
    for (queueFamily in usedQueueFamilies) {

        val prefilledBufferClaims = mutableListOf<PrefilledBufferMemoryClaim>()
        val uninitializedBufferClaims = mutableListOf<UninitializedBufferMemoryClaim>()
        for (agent in agents) {
            prefilledBufferClaims.addAll(agent.prefilledBuffers.filter { it.queueFamily == queueFamily })
            uninitializedBufferClaims.addAll(agent.uninitializedBuffers.filter { it.queueFamily == queueFamily })
            // TODO Also add the image claims
        }

        queueFamilyMap[queueFamily] = QueueFamilyClaims(prefilledBufferClaims, uninitializedBufferClaims)
    }

    return queueFamilyMap
}

internal class Placed<T>(val claim: T, val offset: Long)

internal class PlacedQueueFamilyClaims(
    val prefilledBufferClaims: Collection<Placed<PrefilledBufferMemoryClaim>>,
    val prefilledBufferStagingOffset: Long,
    val prefilledBufferDeviceOffset: Long,

    val uninitializedBufferClaims: Collection<Placed<UninitializedBufferMemoryClaim>>,
    val uninitializedBufferDeviceOffset: Long
)

private fun <T> placeClaims(claims: Collection<T>, getSize: (T) -> Int): Pair<Collection<Placed<T>>, Long> {
    var currentOffset = 0L
    val placedClaims = claims.map { claim ->
        val claimOffset = currentOffset
        currentOffset += getSize(claim)
        Placed(claim, claimOffset)
    }
    return Pair(placedClaims, currentOffset)
}

internal fun placeMemoryClaims(claims: QueueFamilyClaims): PlacedQueueFamilyClaims {
    val (placedPrefilledBufferClaims, totalPrefilledBufferSize) = placeClaims(claims.prefilledBufferClaims) { it.size }
    val (placedUninitializedBufferClaims, _) = placeClaims(claims.uninitializedBufferClaims) { it.size }
    // TODO Also place the images

    return PlacedQueueFamilyClaims(
        prefilledBufferClaims = placedPrefilledBufferClaims,
        prefilledBufferStagingOffset = 0,
        prefilledBufferDeviceOffset = 0,
        uninitializedBufferClaims = placedUninitializedBufferClaims,
        uninitializedBufferDeviceOffset = totalPrefilledBufferSize
    )
}
