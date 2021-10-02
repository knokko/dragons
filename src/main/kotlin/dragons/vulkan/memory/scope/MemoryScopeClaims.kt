package dragons.vulkan.memory.scope

import dragons.vulkan.memory.claim.*

class MemoryScopeClaims(
    val prefilledImages: MutableCollection<PrefilledImageMemoryClaim> = mutableListOf(),
    val uninitializedImages: MutableCollection<UninitializedImageMemoryClaim> = mutableListOf(),
    val prefilledBuffers: MutableCollection<PrefilledBufferMemoryClaim> = mutableListOf(),
    val uninitializedBuffers: MutableCollection<UninitializedBufferMemoryClaim> = mutableListOf(),
    val stagingBuffers: MutableCollection<StagingBufferMemoryClaim> = mutableListOf()
)

class CombinedMemoryScopeClaims(
    val prefilledBufferClaims: Collection<PrefilledBufferMemoryClaim>,
    val uninitializedBufferClaims: Collection<UninitializedBufferMemoryClaim>,
    val stagingBufferClaims: Collection<StagingBufferMemoryClaim>,
    val prefilledImageClaims: Collection<PrefilledImageMemoryClaim>,
    val uninitializedImageClaims: Collection<UninitializedImageMemoryClaim>
) {
    override fun equals(other: Any?): Boolean {
        return if (other is CombinedMemoryScopeClaims) {
            prefilledBufferClaims == other.prefilledBufferClaims
                    && uninitializedBufferClaims == other.uninitializedBufferClaims
                    && stagingBufferClaims == other.stagingBufferClaims
                    && prefilledImageClaims == other.prefilledImageClaims
                    && uninitializedImageClaims == other.uninitializedImageClaims
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        var result = prefilledBufferClaims.hashCode()
        result = 31 * result + uninitializedBufferClaims.hashCode()
        result = 31 * result + stagingBufferClaims.hashCode()
        result = 31 * result + prefilledImageClaims.hashCode()
        result = 31 * result + uninitializedImageClaims.hashCode()
        return result
    }

    override fun toString(): String {
        return "prefilled: $prefilledBufferClaims \n uninitialized: $uninitializedBufferClaims \n staging: $stagingBufferClaims"
    }
}
