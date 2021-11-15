package dragons.vulkan.memory

import dragons.plugin.interfaces.vulkan.VulkanStaticMemoryUser
import dragons.vr.VrManager
import dragons.vulkan.RenderImageInfo
import dragons.vulkan.memory.claim.ImageMemoryClaim
import dragons.vulkan.memory.claim.StagingBufferMemoryClaim
import dragons.vulkan.queue.QueueManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.lwjgl.vulkan.VK12.*
import java.nio.ByteBuffer

internal fun claimStaticCoreMemory(
    agent: VulkanStaticMemoryUser.Agent, vrManager: VrManager, queueManager: QueueManager, renderImageInfo: RenderImageInfo
): CoreStaticMemoryPending {
    val width = vrManager.getWidth()
    val height = vrManager.getHeight()

    val leftColorImage = CompletableDeferred<VulkanImage>()
    val leftDepthImage = CompletableDeferred<VulkanImage>()
    val leftResolveImage = CompletableDeferred<VulkanImage>()
    val rightColorImage = CompletableDeferred<VulkanImage>()
    val rightDepthImage = CompletableDeferred<VulkanImage>()
    val rightResolveImage = CompletableDeferred<VulkanImage>()
    val screenshotBufferLeft = CompletableDeferred<Pair<ByteBuffer, VulkanBufferRange>>()
    val screenshotBufferRight = CompletableDeferred<Pair<ByteBuffer, VulkanBufferRange>>()

    val eyeColorFormat = renderImageInfo.colorFormat
    val eyeDepthFormat = renderImageInfo.depthStencilFormat
    val sampleCount = renderImageInfo.sampleCountBit

    for ((colorImage, depthImage, resolveImage) in arrayOf(
        Triple(leftColorImage, leftDepthImage, leftResolveImage), Triple(rightColorImage, rightDepthImage, rightResolveImage)
    )) {
        agent.claims.images.add(ImageMemoryClaim(
            width = width, height = height,
            queueFamily = queueManager.generalQueueFamily,
            imageFormat = eyeColorFormat,
            tiling = VK_IMAGE_TILING_OPTIMAL, samples = sampleCount,
            // transfer_src is required for resolving
            imageUsage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT or VK_IMAGE_USAGE_TRANSFER_SRC_BIT,
            initialLayout = VK_IMAGE_LAYOUT_UNDEFINED,
            aspectMask = VK_IMAGE_ASPECT_COLOR_BIT, prefill = null,
            storeResult = colorImage
        ))
        agent.claims.images.add(ImageMemoryClaim(
            width = width, height = height,
            queueFamily = queueManager.generalQueueFamily,
            imageFormat = eyeDepthFormat,
            tiling = VK_IMAGE_TILING_OPTIMAL, samples = sampleCount,
            imageUsage = VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT, initialLayout = VK_IMAGE_LAYOUT_UNDEFINED,
            aspectMask = VK_IMAGE_ASPECT_DEPTH_BIT or VK_IMAGE_ASPECT_STENCIL_BIT, prefill = null,
            storeResult = depthImage
        ))
        agent.claims.images.add(ImageMemoryClaim(
            width = width, height = height,
            queueFamily = queueManager.generalQueueFamily,
            imageFormat = eyeColorFormat,
            tiling = VK_IMAGE_TILING_OPTIMAL, samples = VK_SAMPLE_COUNT_1_BIT,
            // Note: transfer_src and sampled are required by OpenVR; transfer_dst is required for resolving itself
            // TODO Create a proper abstraction for this in the VrManager
            imageUsage = VK_IMAGE_USAGE_TRANSFER_SRC_BIT or VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT,
            initialLayout = VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
            aspectMask = VK_IMAGE_ASPECT_COLOR_BIT, accessMask = VK_ACCESS_TRANSFER_WRITE_BIT,
            dstPipelineStageMask = VK_PIPELINE_STAGE_TRANSFER_BIT, prefill = null, storeResult = resolveImage
        ))
    }

    for (screenshotBuffer in arrayOf(screenshotBufferLeft, screenshotBufferRight)) {
        agent.claims.stagingBuffers.add(
            StagingBufferMemoryClaim(
                size = 4 * width * height,
                usageFlags = VK_BUFFER_USAGE_TRANSFER_DST_BIT,
                queueFamily = queueManager.generalQueueFamily,
                storeResult = screenshotBuffer
            )
        )
    }

    return CoreStaticMemoryPending(
        leftColorImage = leftColorImage, leftDepthImage = leftDepthImage, leftResolveImage = leftResolveImage,
        rightColorImage = rightColorImage, rightDepthImage = rightDepthImage, rightResolveImage = rightResolveImage,
        screenshotBufferLeft = screenshotBufferLeft, screenshotBufferRight = screenshotBufferRight
    )
}

internal class CoreStaticMemoryPending(
    val leftColorImage: Deferred<VulkanImage>,
    val leftDepthImage: Deferred<VulkanImage>,
    private val leftResolveImage: Deferred<VulkanImage>,
    val rightColorImage: Deferred<VulkanImage>,
    val rightDepthImage: Deferred<VulkanImage>,
    private val rightResolveImage: Deferred<VulkanImage>,
    val screenshotBufferLeft: Deferred<Pair<ByteBuffer, VulkanBufferRange>>,
    val screenshotBufferRight: Deferred<Pair<ByteBuffer, VulkanBufferRange>>
) {
    suspend fun awaitCompletely() = CoreStaticMemory(
        leftColorImage = leftColorImage.await(),
        leftDepthImage = leftDepthImage.await(),
        leftResolveImage = leftResolveImage.await(),
        rightColorImage = rightColorImage.await(),
        rightDepthImage = rightDepthImage.await(),
        rightResolveImage = rightResolveImage.await(),
        leftScreenshotBuffer = screenshotBufferLeft.await(),
        rightScreenshotBuffer = screenshotBufferRight.await()
    )
}

class CoreStaticMemory(
    val leftColorImage: VulkanImage,
    val leftDepthImage: VulkanImage,
    internal val leftResolveImage: VulkanImage,
    val rightColorImage: VulkanImage,
    val rightDepthImage: VulkanImage,
    internal val rightResolveImage: VulkanImage,
    val leftScreenshotBuffer: Pair<ByteBuffer, VulkanBufferRange>,
    val rightScreenshotBuffer: Pair<ByteBuffer, VulkanBufferRange>
)
