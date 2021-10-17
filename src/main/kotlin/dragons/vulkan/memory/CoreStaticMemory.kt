package dragons.vulkan.memory

import dragons.plugin.interfaces.vulkan.VulkanStaticMemoryUser
import dragons.vr.VrManager
import dragons.vulkan.memory.claim.ImageMemoryClaim
import dragons.vulkan.queue.QueueManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkFormatProperties
import org.lwjgl.vulkan.VkPhysicalDevice

internal fun claimStaticCoreMemory(
    vkPhysicalDevice: VkPhysicalDevice, agent: VulkanStaticMemoryUser.Agent, vrManager: VrManager, queueManager: QueueManager
): CoreStaticMemoryPending {
    val width = vrManager.getWidth()
    val height = vrManager.getHeight()

    val leftColorImage = CompletableDeferred<VulkanImage>()
    val leftDepthImage = CompletableDeferred<VulkanImage>()
    val rightColorImage = CompletableDeferred<VulkanImage>()
    val rightDepthImage = CompletableDeferred<VulkanImage>()

    val eyeColorFormat = VK_FORMAT_R8G8B8A8_SRGB

    /*
     * Quoting from the Vulkan specification:
     * "VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT feature must be supported for at least one of
     * VK_FORMAT_X8_D24_UNORM_PACK32 and VK_FORMAT_D32_SFLOAT, and must be supported for at least one of
     * VK_FORMAT_D24_UNORM_S8_UINT and VK_FORMAT_D32_SFLOAT_S8_UINT."
     */
    val eyeDepthFormat = stackPush().use { stack ->
        val formatProps = VkFormatProperties.calloc(stack)
        vkGetPhysicalDeviceFormatProperties(vkPhysicalDevice, VK_FORMAT_D24_UNORM_S8_UINT, formatProps)

        if ((formatProps.optimalTilingFeatures() and VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT) != 0) {
            VK_FORMAT_D24_UNORM_S8_UINT
        } else {
            VK_FORMAT_D32_SFLOAT_S8_UINT
        }
    }

    // TODO Experiment with other sample counts
    val sampleCount = VK_SAMPLE_COUNT_1_BIT

    // Note: transfer_src and sampled are required by OpenVR
    // TODO Create a proper abstraction for this in the VrManager
    val eyeColorImageUsage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT or VK_IMAGE_USAGE_TRANSFER_SRC_BIT or VK_IMAGE_USAGE_SAMPLED_BIT

    for ((colorImage, depthImage) in arrayOf(Pair(leftColorImage, leftDepthImage), Pair(rightColorImage, rightDepthImage))) {
        agent.claims.images.add(ImageMemoryClaim(
            width = width, height = height,
            queueFamily = queueManager.generalQueueFamily,
            imageFormat = eyeColorFormat,
            tiling = VK_IMAGE_TILING_OPTIMAL, samples = sampleCount,
            imageUsage = eyeColorImageUsage, initialLayout = VK_IMAGE_LAYOUT_UNDEFINED,
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
