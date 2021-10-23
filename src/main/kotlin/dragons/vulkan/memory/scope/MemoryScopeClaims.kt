package dragons.vulkan.memory.scope

import dragons.vulkan.memory.claim.*

class MemoryScopeClaims(
    val images: MutableCollection<ImageMemoryClaim> = mutableListOf(),
    val buffers: MutableCollection<BufferMemoryClaim> = mutableListOf(),
    val stagingBuffers: MutableCollection<StagingBufferMemoryClaim> = mutableListOf()
) {
    override fun toString() = "buffers: $buffers, images: $images, stagingBuffers: $stagingBuffers"
}

class CombinedMemoryScopeClaims(
    val allBufferClaims: Collection<BufferMemoryClaim>,
    val stagingBufferClaims: Collection<StagingBufferMemoryClaim>,
    val allImageClaims: Collection<ImageMemoryClaim>,
) {
    val prefilledBufferClaims = allBufferClaims.filter { it.prefill != null }
    val uninitializedBufferClaims = allBufferClaims.filter { it.prefill == null }

    val prefilledImageClaims = allImageClaims.filter { it.prefill != null }
    val uninitializedImageClaims = allImageClaims.filter { it.prefill == null }

    override fun equals(other: Any?): Boolean {
        return if (other is CombinedMemoryScopeClaims) {
            allBufferClaims == other.allBufferClaims
                    && stagingBufferClaims == other.stagingBufferClaims
                    && allImageClaims == other.allImageClaims
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        var result = allBufferClaims.hashCode()
        result = 31 * result + stagingBufferClaims.hashCode()
        result = 31 * result + allImageClaims.hashCode()
        return result
    }

    override fun toString(): String {
        return "buffers: $allBufferClaims \n staging: $stagingBufferClaims \n images: $allImageClaims"
    }
}
