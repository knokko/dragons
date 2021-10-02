package dragons.vulkan.memory.claim

import dragons.vulkan.memory.scope.CombinedMemoryScopeClaims
import dragons.vulkan.memory.scope.MemoryScopeClaims
import dragons.vulkan.queue.QueueFamily

// TODO Update the unit tests to take the images into account

internal fun getUsedQueueFamilies(allClaims: Collection<MemoryScopeClaims>): Set<QueueFamily?> {
    val usedQueueFamilies = mutableSetOf<QueueFamily?>()
    for (claims in allClaims) {
        for (bufferClaim in claims.prefilledBuffers) {
            usedQueueFamilies.add(bufferClaim.queueFamily)
        }
        for (bufferClaim in claims.uninitializedBuffers) {
            usedQueueFamilies.add(bufferClaim.queueFamily)
        }
        for (bufferClaim in claims.stagingBuffers) {
            usedQueueFamilies.add(bufferClaim.queueFamily)
        }
        for (imageClaim in claims.prefilledImages) {
            usedQueueFamilies.add(imageClaim.queueFamily)
        }
        for (imageClaim in claims.uninitializedImages) {
            usedQueueFamilies.add(imageClaim.queueFamily)
        }
    }

    return usedQueueFamilies
}

internal class QueueFamilyClaims(val claims: CombinedMemoryScopeClaims) {
    val tempStagingSize: Long
    val deviceBufferSize: Long
    val persistentStagingSize: Long = claims.stagingBufferClaims.sumOf { it.size.toLong() }
    val deviceImageSize: Long

    init {
        val prefilledBufferSize = claims.prefilledBufferClaims.sumOf { it.size.toLong() }
        deviceBufferSize = prefilledBufferSize + claims.uninitializedBufferClaims.sumOf { it.size.toLong() }

        val prefilledImageSize = claims.prefilledImageClaims.sumOf { it.getByteSize().toLong() }
        deviceImageSize = prefilledImageSize + claims.uninitializedImageClaims.sumOf { it.getByteSize().toLong() }

        tempStagingSize = prefilledBufferSize + prefilledImageSize
    }

    override fun equals(other: Any?): Boolean {
        return if (other is QueueFamilyClaims) {
            claims == other.claims
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return claims.hashCode()
    }

    override fun toString(): String {
        return claims.toString()
    }
}

internal fun groupMemoryClaims(allClaims: Collection<MemoryScopeClaims>): Map<QueueFamily?, QueueFamilyClaims> {
    val usedQueueFamilies = getUsedQueueFamilies(allClaims)

    val queueFamilyMap = mutableMapOf<QueueFamily?, QueueFamilyClaims>()
    for (queueFamily in usedQueueFamilies) {

        val prefilledBufferClaims = mutableListOf<PrefilledBufferMemoryClaim>()
        val uninitializedBufferClaims = mutableListOf<UninitializedBufferMemoryClaim>()
        val stagingBufferClaims = mutableListOf<StagingBufferMemoryClaim>()
        val prefilledImageClaims = mutableListOf<PrefilledImageMemoryClaim>()
        val uninitializedImageClaims = mutableListOf<UninitializedImageMemoryClaim>()

        for (claims in allClaims) {
            prefilledBufferClaims.addAll(claims.prefilledBuffers.filter { it.queueFamily == queueFamily })
            uninitializedBufferClaims.addAll(claims.uninitializedBuffers.filter { it.queueFamily == queueFamily })
            stagingBufferClaims.addAll(claims.stagingBuffers.filter { it.queueFamily == queueFamily })
            prefilledImageClaims.addAll(claims.prefilledImages.filter { it.queueFamily == queueFamily })
            uninitializedImageClaims.addAll(claims.uninitializedImages.filter { it.queueFamily == queueFamily })
        }

        queueFamilyMap[queueFamily] = QueueFamilyClaims(CombinedMemoryScopeClaims(
            prefilledBufferClaims,
            uninitializedBufferClaims,
            stagingBufferClaims,
            prefilledImageClaims,
            uninitializedImageClaims
        ))
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

/**
 * The **internal** placements of the buffer and image claims of a queue family. **This is completely independent of the
 * placements of any other queue family.**
 */
internal class PlacedQueueFamilyClaims(
    val prefilledBufferClaims: Collection<Placed<PrefilledBufferMemoryClaim>>,
    /**
     * The number of bytes between the start of the temporary staging buffer domain **of this queue family** and the
     * first byte of the prefilled buffer content. (This is currently always 0.)
     */
    val prefilledBufferStagingOffset: Long,
    /**
     * The number of bytes between the start of the device buffer domain **of this queue family** and the first byte of
     * the prefilled buffer content. (This is currently always 0.)
     */
    val prefilledBufferDeviceOffset: Long,

    val uninitializedBufferClaims: Collection<Placed<UninitializedBufferMemoryClaim>>,
    /**
     * The number of bytes between the start of the device buffer domain **of this queue family** and the first byte of
     * the first uninitialized buffer. (This is currently the sum of the sizes of all prefilled buffers of this queue
     * family.)
     */
    val uninitializedBufferDeviceOffset: Long,

    val stagingBufferClaims: Collection<Placed<StagingBufferMemoryClaim>>,
    /**
     * The number of bytes between the start of the persistent staging buffer domain **of this queue family** and the
     * first byte of the first persistent staging buffer. (This is currently always 0.)
     */
    val stagingBufferOffset: Long,

    val prefilledImageClaims: Collection<Placed<PrefilledImageMemoryClaim>>,
    /**
     * The number of bytes between the start of the temporary staging buffer domain **of this queue family** and the
     * first byte of the prefilled image content. (This is currently the sum of the sizes of all prefilled buffers of
     * this queue family.)
     */
    val prefilledImageStagingOffset: Long,

    val uninitializedImageClaims: Collection<UninitializedImageMemoryClaim>,
) {
    override fun equals(other: Any?): Boolean {
        return if (other is PlacedQueueFamilyClaims) {
            (prefilledBufferClaims == other.prefilledBufferClaims
                    && prefilledBufferStagingOffset == other.prefilledBufferStagingOffset
                    && prefilledBufferDeviceOffset == other.prefilledBufferDeviceOffset
                    && uninitializedBufferClaims == other.uninitializedBufferClaims
                    && uninitializedBufferDeviceOffset == other.uninitializedBufferDeviceOffset
                    && stagingBufferClaims == other.stagingBufferClaims
                    && stagingBufferOffset == other.stagingBufferOffset
                    && prefilledImageClaims == other.prefilledImageClaims
                    && prefilledImageStagingOffset == other.prefilledImageStagingOffset
                    && uninitializedImageClaims == other.uninitializedImageClaims
                    )
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
        result = 31 * result + prefilledImageClaims.hashCode()
        result = 31 * result + prefilledImageStagingOffset.hashCode()
        result = 31 * result + uninitializedImageClaims.hashCode()
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
    val (placedPrefilledBufferClaims, totalPrefilledBufferSize) = placeClaims(claims.claims.prefilledBufferClaims) { it.size }
    val (placedUninitializedBufferClaims, _) = placeClaims(claims.claims.uninitializedBufferClaims) { it.size }
    val (placedStagingBufferClaims, _) = placeClaims(claims.claims.stagingBufferClaims) { it.size }
    val (placedPrefilledImageClaims, _) = placeClaims(claims.claims.prefilledImageClaims) { it.getByteSize() }

    return PlacedQueueFamilyClaims(
        prefilledBufferClaims = placedPrefilledBufferClaims,
        prefilledBufferStagingOffset = 0,
        prefilledBufferDeviceOffset = 0,
        uninitializedBufferClaims = placedUninitializedBufferClaims,
        uninitializedBufferDeviceOffset = totalPrefilledBufferSize,
        stagingBufferClaims = placedStagingBufferClaims,
        stagingBufferOffset = 0,
        prefilledImageClaims = placedPrefilledImageClaims,
        prefilledImageStagingOffset = totalPrefilledBufferSize,
        uninitializedImageClaims = claims.claims.uninitializedImageClaims
    )
}
