package dragons.vulkan.memory.claim

import dragons.vulkan.memory.scope.CombinedMemoryScopeClaims
import dragons.vulkan.memory.scope.MemoryScopeClaims
import dragons.vulkan.queue.QueueFamily

internal fun getUsedQueueFamilies(allClaims: Collection<MemoryScopeClaims>): Set<QueueFamily?> {
    val usedQueueFamilies = mutableSetOf<QueueFamily?>()
    for (claims in allClaims) {
        for (bufferClaim in claims.buffers) {
            usedQueueFamilies.add(bufferClaim.queueFamily)
        }
        for (bufferClaim in claims.stagingBuffers) {
            usedQueueFamilies.add(bufferClaim.queueFamily)
        }
        for (imageClaim in claims.images) {
            usedQueueFamilies.add(imageClaim.queueFamily)
        }
    }

    return usedQueueFamilies
}

internal class QueueFamilyClaims(val claims: CombinedMemoryScopeClaims) {
    val persistentStagingSize: Long = claims.stagingBufferClaims.sumOf { it.size.toLong() }

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

        val allBufferClaims = mutableListOf<BufferMemoryClaim>()
        val stagingBufferClaims = mutableListOf<StagingBufferMemoryClaim>()
        val allImageClaims = mutableListOf<ImageMemoryClaim>()

        for (claims in allClaims) {
            allBufferClaims.addAll(claims.buffers.filter { it.queueFamily == queueFamily })
            stagingBufferClaims.addAll(claims.stagingBuffers.filter { it.queueFamily == queueFamily })
            allImageClaims.addAll(claims.images.filter { it.queueFamily == queueFamily })
        }

        queueFamilyMap[queueFamily] = QueueFamilyClaims(CombinedMemoryScopeClaims(
            allBufferClaims,
            stagingBufferClaims,
            allImageClaims
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

    override fun toString() = "Placed($claim, offset = $offset)"
}

/**
 * The **internal** placements of the buffer and image claims of a queue family. **This is completely independent of the
 * placements of any other queue family.**
 */
internal class PlacedQueueFamilyClaims(
    val prefilledBufferClaims: Collection<Placed<BufferMemoryClaim>>,
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

    val uninitializedBufferClaims: Collection<Placed<BufferMemoryClaim>>,
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

    val prefilledImageClaims: Collection<Placed<ImageMemoryClaim>>,
    /**
     * The number of bytes between the start of the temporary staging buffer domain **of this queue family** and the
     * first byte of the prefilled image content. (This is currently the sum of the sizes of all prefilled buffers of
     * this queue family.)
     */
    val prefilledImageStagingOffset: Long,

    val uninitializedImageClaims: Collection<ImageMemoryClaim>,
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

    override fun toString() = "prefilled buffers: $prefilledBufferClaims, prefilled images: $prefilledImageClaims, uninit buffers: $uninitializedBufferClaims"
}
