package dragons.vulkan.memory.claim

import dragons.plugin.interfaces.vulkan.VulkanStaticMemoryUser
import dragons.vulkan.queue.QueueFamily

// TODO Update the unit tests to take the staging into account

internal fun getUsedQueueFamilies(agents: Collection<VulkanStaticMemoryUser.Agent>): Set<QueueFamily?> {
    val usedQueueFamilies = mutableSetOf<QueueFamily?>()
    for (agent in agents) {
        for (bufferClaim in agent.prefilledBuffers) {
            usedQueueFamilies.add(bufferClaim.queueFamily)
        }
        for (bufferClaim in agent.uninitializedBuffers) {
            usedQueueFamilies.add(bufferClaim.queueFamily)
        }
        for (bufferClaim in agent.stagingBuffers) {
            usedQueueFamilies.add(bufferClaim.queueFamily)
        }
        // TODO Also consider the images
    }

    return usedQueueFamilies
}

internal class QueueFamilyClaims(
    val prefilledBufferClaims: Collection<PrefilledBufferMemoryClaim>,
    val uninitializedBufferClaims: Collection<UninitializedBufferMemoryClaim>,
    val stagingBufferClaims: Collection<StagingBufferMemoryClaim>
) {
    val tempStagingSize: Long = prefilledBufferClaims.sumOf { it.size.toLong() }
    val deviceBufferSize: Long = tempStagingSize + uninitializedBufferClaims.sumOf { it.size.toLong() }
    val persistentStagingSize: Long = stagingBufferClaims.sumOf { it.size.toLong() }

    override fun equals(other: Any?): Boolean {
        return if (other is QueueFamilyClaims) {
            prefilledBufferClaims == other.prefilledBufferClaims
                    && uninitializedBufferClaims == other.uninitializedBufferClaims
                    && stagingBufferClaims == other.stagingBufferClaims
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        var result = prefilledBufferClaims.hashCode()
        result = 31 * result + uninitializedBufferClaims.hashCode()
        result = 31 * result + stagingBufferClaims.hashCode()
        return result
    }

    override fun toString(): String {
        return "prefilled: $prefilledBufferClaims \n uninitialized: $uninitializedBufferClaims \n staging: $stagingBufferClaims"
    }
}

internal fun groupMemoryClaims(agents: Collection<VulkanStaticMemoryUser.Agent>): Map<QueueFamily?, QueueFamilyClaims> {
    val usedQueueFamilies = getUsedQueueFamilies(agents)

    val queueFamilyMap = mutableMapOf<QueueFamily?, QueueFamilyClaims>()
    for (queueFamily in usedQueueFamilies) {

        val prefilledBufferClaims = mutableListOf<PrefilledBufferMemoryClaim>()
        val uninitializedBufferClaims = mutableListOf<UninitializedBufferMemoryClaim>()
        val stagingBufferClaims = mutableListOf<StagingBufferMemoryClaim>()
        for (agent in agents) {
            prefilledBufferClaims.addAll(agent.prefilledBuffers.filter { it.queueFamily == queueFamily })
            uninitializedBufferClaims.addAll(agent.uninitializedBuffers.filter { it.queueFamily == queueFamily })
            stagingBufferClaims.addAll(agent.stagingBuffers.filter { it.queueFamily == queueFamily })
            // TODO Also add the image claims
        }

        queueFamilyMap[queueFamily] = QueueFamilyClaims(
            prefilledBufferClaims,
            uninitializedBufferClaims,
            stagingBufferClaims
        )
    }

    return queueFamilyMap
}

internal class Placed<T>(val claim: T, val offset: Long) {
    override fun equals(other: Any?): Boolean {
        return if (other is Placed<*>) {
            claim == other.claim && offset == other.offset
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        var result = claim?.hashCode() ?: 0
        result = 31 * result + offset.hashCode()
        return result
    }
}

internal class PlacedQueueFamilyClaims(
    val prefilledBufferClaims: Collection<Placed<PrefilledBufferMemoryClaim>>,
    val prefilledBufferStagingOffset: Long,
    val prefilledBufferDeviceOffset: Long,

    val uninitializedBufferClaims: Collection<Placed<UninitializedBufferMemoryClaim>>,
    val uninitializedBufferDeviceOffset: Long,

    val stagingBufferClaims: Collection<Placed<StagingBufferMemoryClaim>>,
    val stagingBufferOffset: Long
) {
    override fun equals(other: Any?): Boolean {
        return if (other is PlacedQueueFamilyClaims) {
            (prefilledBufferClaims == other.prefilledBufferClaims
                    && prefilledBufferStagingOffset == other.prefilledBufferStagingOffset
                    && prefilledBufferDeviceOffset == other.prefilledBufferDeviceOffset
                    && uninitializedBufferClaims == other.uninitializedBufferClaims
                    && uninitializedBufferDeviceOffset == other.uninitializedBufferDeviceOffset
                    && stagingBufferClaims == other.stagingBufferClaims
                    && stagingBufferOffset == other.stagingBufferOffset)
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        var result = prefilledBufferClaims.hashCode()
        result = 31 * result + prefilledBufferStagingOffset.hashCode()
        result = 31 * result + prefilledBufferDeviceOffset.hashCode()
        result = 31 * result + uninitializedBufferClaims.hashCode()
        result = 31 * result + uninitializedBufferDeviceOffset.hashCode()
        result = 31 * result + stagingBufferClaims.hashCode()
        result = 31 * result + stagingBufferOffset.hashCode()
        return result
    }
}

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
    val (placedStagingBufferClaims, _) = placeClaims(claims.stagingBufferClaims) { it.size }
    // TODO Also place the images

    return PlacedQueueFamilyClaims(
        prefilledBufferClaims = placedPrefilledBufferClaims,
        prefilledBufferStagingOffset = 0,
        prefilledBufferDeviceOffset = 0,
        uninitializedBufferClaims = placedUninitializedBufferClaims,
        uninitializedBufferDeviceOffset = totalPrefilledBufferSize,
        stagingBufferClaims = placedStagingBufferClaims,
        stagingBufferOffset = 0
    )
}
