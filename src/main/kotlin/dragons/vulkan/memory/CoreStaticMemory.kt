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
    val rightColorImage = CompletableDeferred<VulkanImage>()
    val rightDepthImage = CompletableDeferred<VulkanImage>()
    val screenshotBufferLeft = CompletableDeferred<Pair<ByteBuffer, VulkanBufferRange>>()
    val screenshotBufferRight = CompletableDeferred<Pair<ByteBuffer, VulkanBufferRange>>()

    val eyeColorFormat = renderImageInfo.colorFormat
    val eyeDepthFormat = renderImageInfo.depthStencilFormat
    val sampleCount = renderImageInfo.sampleCountBit

    for ((colorImage, depthImage) in arrayOf(
        Pair(leftColorImage, leftDepthImage), Pair(rightColorImage, rightDepthImage)
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
        leftColorImage = leftColorImage, leftDepthImage = leftDepthImage,
        rightColorImage = rightColorImage, rightDepthImage = rightDepthImage,
        screenshotBufferLeft = screenshotBufferLeft, screenshotBufferRight = screenshotBufferRight
    )
}

internal class CoreStaticMemoryPending(
    val leftColorImage: Deferred<VulkanImage>,
    val leftDepthImage: Deferred<VulkanImage>,
    val rightColorImage: Deferred<VulkanImage>,
    val rightDepthImage: Deferred<VulkanImage>,
    val screenshotBufferLeft: Deferred<Pair<ByteBuffer, VulkanBufferRange>>,
    val screenshotBufferRight: Deferred<Pair<ByteBuffer, VulkanBufferRange>>
) {
    suspend fun awaitCompletely() = CoreStaticMemory(
        leftColorImage = leftColorImage.await(),
        leftDepthImage = leftDepthImage.await(),
        rightColorImage = rightColorImage.await(),
        rightDepthImage = rightDepthImage.await(),
        leftScreenshotBuffer = screenshotBufferLeft.await(),
        rightScreenshotBuffer = screenshotBufferRight.await()
    )
}

class CoreStaticMemory(
    val leftColorImage: VulkanImage,
    val leftDepthImage: VulkanImage,
    val rightColorImage: VulkanImage,
    val rightDepthImage: VulkanImage,
    val leftScreenshotBuffer: Pair<ByteBuffer, VulkanBufferRange>,
    val rightScreenshotBuffer: Pair<ByteBuffer, VulkanBufferRange>
)
