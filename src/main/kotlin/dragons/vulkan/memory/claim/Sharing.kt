package dragons.vulkan.memory.claim

import dragons.vulkan.memory.scope.CombinedMemoryScopeClaims
import dragons.vulkan.queue.QueueFamily
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking

internal fun shareMemoryClaims(claimsMap: Map<QueueFamily?, QueueFamilyClaims>) = claimsMap.mapValues { entry -> shareMemoryClaims(entry.value) }

private fun <T> createCompositeCompletableDeferred(storeResults: Collection<CompletableDeferred<T>>): CompletableDeferred<T> {
    val compositeResult = CompletableDeferred<T>()
    compositeResult.invokeOnCompletion { result ->
        if (result == null) {
            runBlocking {
                for (storeResult in storeResults) {
                    storeResult.complete(compositeResult.await())
                }
            }
        } else if (result is CancellationException) {
            for (storeResult in storeResults) {
                storeResult.cancel(result)
            }
        } else {
            throw RuntimeException("Sharing ID failed", result)
        }
    }

    return compositeResult
}

private fun shareMemoryClaims(queueFamilyClaims: QueueFamilyClaims): QueueFamilyClaims {
    val claims = queueFamilyClaims.claims

    val bufferMap = mutableMapOf<String, MutableList<BufferMemoryClaim>>()
    val imageMap = mutableMapOf<String, MutableList<ImageMemoryClaim>>()
    val resultBufferClaims = mutableListOf<BufferMemoryClaim>()
    val resultImageClaims = mutableListOf<ImageMemoryClaim>()

    for (bufferClaim in claims.allBufferClaims) {
        if (bufferClaim.sharingID != null) {
            bufferMap.getOrPut(bufferClaim.sharingID, ::mutableListOf).add(bufferClaim)
        } else {
            resultBufferClaims.add(bufferClaim)
        }
    }

    for (imageClaim in claims.allImageClaims) {
        if (imageClaim.sharingID != null) {
            imageMap.getOrPut(imageClaim.sharingID, ::mutableListOf).add(imageClaim)
        } else {
            resultImageClaims.add(imageClaim)
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

        val storeResult = createCompositeCompletableDeferred(sharedClaims.map { it.storeResult })

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

    for (sharedClaims in imageMap.values) {
        val firstClaim = sharedClaims[0]

        // Sanity check: only identical claims should be shared!
        for (claim in sharedClaims) {
            if (claim.width != firstClaim.width) throw IllegalArgumentException("Shared claims have different widths")
            if (claim.height != firstClaim.height) throw IllegalArgumentException("Shared claims have different heights")
            if (claim.imageUsage != firstClaim.imageUsage) throw IllegalArgumentException("Shared claims have different image usages")
            if (claim.dstPipelineStageMask != firstClaim.dstPipelineStageMask) {
                throw IllegalArgumentException("Shared claims have different dest pipeline stage masks")
            }
        }

        val storeResult = createCompositeCompletableDeferred(sharedClaims.map { it.storeResult })

        val combinedClaim = ImageMemoryClaim(
            width = firstClaim.width, height = firstClaim.height, queueFamily = firstClaim.queueFamily,
            imageCreateFlags = firstClaim.imageCreateFlags, imageViewFlags = firstClaim.imageViewFlags,
            bytesPerPixel = firstClaim.bytesPerPixel, imageFormat = firstClaim.imageFormat, tiling = firstClaim.tiling,
            samples = firstClaim.samples, imageUsage = firstClaim.imageUsage, initialLayout = firstClaim.initialLayout,
            aspectMask = firstClaim.aspectMask, accessMask = firstClaim.accessMask,
            dstPipelineStageMask = firstClaim.dstPipelineStageMask, storeResult = storeResult,
            sharingID = firstClaim.sharingID, prefill = firstClaim.prefill
        )

        resultImageClaims.add(combinedClaim)
    }

    return QueueFamilyClaims(CombinedMemoryScopeClaims(
        resultBufferClaims, claims.stagingBufferClaims, resultImageClaims
    ))
}
