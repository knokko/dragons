package dragons.vulkan.memory

import dragons.plugin.interfaces.vulkan.VulkanStaticMemoryUser
import dragons.vr.VrManager
import dragons.vulkan.memory.claim.UninitializedImageMemoryClaim
import dragons.vulkan.queue.QueueManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

internal fun claimStaticCoreMemory(
    agent: VulkanStaticMemoryUser.Agent, vrManager: VrManager, queueManager: QueueManager
): CoreStaticMemoryPending {
    val width = vrManager.getWidth()
    val height = vrManager.getHeight()

    val leftColorImage = CompletableDeferred<VulkanImage>()
    val leftDepthImage = CompletableDeferred<VulkanImage>()

    agent.claims.uninitializedImages.add(
        UninitializedImageMemoryClaim(
        width, height, 4, queueManager.generalQueueFamily, leftColorImage
    )
    )
    agent.claims.uninitializedImages.add(
        UninitializedImageMemoryClaim(
        width, height, 4, queueManager.generalQueueFamily, leftDepthImage
    )
    )

    val rightColorImage = CompletableDeferred<VulkanImage>()
    val rightDepthImage = CompletableDeferred<VulkanImage>()

    agent.claims.uninitializedImages.add(
        UninitializedImageMemoryClaim(
        width, height, 4, queueManager.generalQueueFamily, rightColorImage
    )
    )
    agent.claims.uninitializedImages.add(
        UninitializedImageMemoryClaim(
        width, height, 4, queueManager.generalQueueFamily, rightDepthImage
    )
    )

    return CoreStaticMemoryPending(
        leftColorImage = leftColorImage, leftDepthImage = leftDepthImage,
        rightColorImage = rightColorImage, rightDepthImage = rightDepthImage
    )
}

internal class CoreStaticMemoryPending(
    val leftColorImage: Deferred<VulkanImage>,
    val leftDepthImage: Deferred<VulkanImage>,
    val rightColorImage: Deferred<VulkanImage>,
    val rightDepthImage: Deferred<VulkanImage>
) {
    suspend fun awaitCompletely() = CoreStaticMemory(
        leftColorImage = leftColorImage.await(),
        leftDepthImage = leftDepthImage.await(),
        rightColorImage = rightColorImage.await(),
        rightDepthImage = rightDepthImage.await()
    )
}

class CoreStaticMemory(
    val leftColorImage: VulkanImage,
    val leftDepthImage: VulkanImage,
    val rightColorImage: VulkanImage,
    val rightDepthImage: VulkanImage
)
