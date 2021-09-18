package dragons.vulkan.memory

import kotlinx.coroutines.Deferred

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
