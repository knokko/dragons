package dragons.vulkan.memory.claim

import dragons.vulkan.memory.VulkanBufferRange
import dragons.vulkan.memory.scope.CombinedMemoryScopeClaims
import dragons.vulkan.queue.QueueFamily
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking

// TODO Also share images and maybe staging buffers
internal fun shareMemoryClaims(claimsMap: Map<QueueFamily?, QueueFamilyClaims>) = claimsMap.mapValues { entry -> shareMemoryClaims(entry.value) }

private fun shareMemoryClaims(queueFamilyClaims: QueueFamilyClaims): QueueFamilyClaims {
    val claims = queueFamilyClaims.claims

    val bufferMap = mutableMapOf<String, MutableList<BufferMemoryClaim>>()
    val resultBufferClaims = mutableListOf<BufferMemoryClaim>()

    for (bufferClaim in claims.allBufferClaims) {
        if (bufferClaim.sharingID != null) {
            bufferMap.getOrPut(bufferClaim.sharingID, ::mutableListOf).add(bufferClaim)
        } else {
            resultBufferClaims.add(bufferClaim)
        }
    }

    for (sharedClaims in bufferMap.values) {
        val firstClaim = sharedClaims[0]

        // Sanity check: only identical claims should be shared!
        for (claim in sharedClaims) {
            if (claim.size != firstClaim.size) throw IllegalArgumentException("Shared claims have different sizes")
            if (claim.usageFlags != firstClaim.usageFlags) throw IllegalArgumentException("Shared claims have different usage flags")
            if (claim.dstAccessMask != firstClaim.dstAccessMask) throw IllegalArgumentException("Shared claims have different access masks")
            if (claim.dstPipelineStageMask != firstClaim.dstPipelineStageMask) throw IllegalArgumentException("Shared claims have different stage masks")
            if (claim.alignment != firstClaim.alignment) throw IllegalArgumentException("Shared claims have different alignment")
            if (claim.queueFamily != firstClaim.queueFamily) throw IllegalArgumentException("Shared claims have different queue families")
        }

        val storeResult = CompletableDeferred<VulkanBufferRange>()
        storeResult.invokeOnCompletion { result ->
            if (result == null) {
                runBlocking {
                    for (claim in sharedClaims) {
                        claim.storeResult.complete(storeResult.await())
                    }
                }
            } else if (result is CancellationException) {
                for (claim in sharedClaims) {
                    claim.storeResult.cancel(result)
                }
            } else {
                throw RuntimeException("Sharing ID failed", result)
            }
        }

        val combinedClaim = BufferMemoryClaim(
            size = firstClaim.size,
            usageFlags = firstClaim.usageFlags,
            dstAccessMask = firstClaim.dstAccessMask,
            dstPipelineStageMask = firstClaim.dstPipelineStageMask,
            alignment = firstClaim.alignment,
            queueFamily = firstClaim.queueFamily,
            storeResult = storeResult,
            sharingID = firstClaim.sharingID,
            prefill = firstClaim.prefill
        )

        resultBufferClaims.add(combinedClaim)
    }

    return QueueFamilyClaims(CombinedMemoryScopeClaims(
        resultBufferClaims, claims.stagingBufferClaims, claims.allImageClaims
    ))
}
