package dragons.vulkan

import dragons.init.trouble.SimpleStartupException
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkFormatProperties
import org.lwjgl.vulkan.VkPhysicalDevice
import org.lwjgl.vulkan.VkPhysicalDeviceLimits
import org.lwjgl.vulkan.VkPhysicalDeviceProperties

private fun getLimits(physicalDevice: VkPhysicalDevice): VkPhysicalDeviceLimits {
    val properties = VkPhysicalDeviceProperties.create()
    vkGetPhysicalDeviceProperties(physicalDevice, properties)
    return properties.limits()
}

/**
 * This class captures the information about the formats and sample counts used by the *primary render targets* of the
 * game (currently a color buffer and a depth-stencil buffer). The game core is responsible for creating these render
 * targets.
 *
 * ## Motivation
 * Since multiple plug-ins could be rendering the same scene, it is important that they use the same color buffer and
 * depth-stencil buffer to ensure that the content drawn by all plug-ins is visible and that the objects closer to the
 * player are drawn in front of the objects further away from the player, even if they were drawn by different plug-ins.
 */
class RenderImageInfo(
    supportsDepthStencilFormat: (Int) -> Boolean, limits: VkPhysicalDeviceLimits
) {

    constructor(physicalDevice: VkPhysicalDevice) : this({ candidateFormat ->
        stackPush().use { stack ->
            val formatProps = VkFormatProperties.calloc(stack)
            vkGetPhysicalDeviceFormatProperties(physicalDevice, candidateFormat, formatProps)

            (formatProps.optimalTilingFeatures() and VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT) != 0
        }
    }, getLimits(physicalDevice))

    // The Vulkan specification guarantees that this format is supported for color attachment images
    /**
     * The format of the core color buffer. This buffer will be copied to the VR HMD every frame and plug-ins are
     * expected to do their primary rendering on this buffer.
     */
    val colorFormat = VK_FORMAT_R8G8B8A8_SRGB

    /**
     * The format of the core depth-stencil buffer. Plug-ins are expected to use this buffer as depth-stencil buffer of
     * their primary rendering.
     */
    val depthStencilFormat: Int

    val sampleCountBit: Int

    init {
        // The Vulkan specification guarantees that at least 1 of d24_s8 or d32_s8 is supported for depth-stencil attachments
        depthStencilFormat = if (supportsDepthStencilFormat(VK_FORMAT_D24_UNORM_S8_UINT)) {
            VK_FORMAT_D24_UNORM_S8_UINT
        } else {
            VK_FORMAT_D32_SFLOAT_S8_UINT
        }

        val supportedSampleCounts = limits.sampledImageColorSampleCounts() and limits.sampledImageDepthSampleCounts() and
                limits.sampledImageStencilSampleCounts()

        // TODO I might want to fetch these from some config file
        val desiredSampleCounts = arrayOf(VK_SAMPLE_COUNT_8_BIT, VK_SAMPLE_COUNT_4_BIT, VK_SAMPLE_COUNT_2_BIT)

        sampleCountBit = run {
            for (candidate in desiredSampleCounts) {
                if ((supportedSampleCounts and candidate) != 0) {
                    return@run candidate
                }
            }

            throw SimpleStartupException("Insufficient sample count support", listOf(
                "This game requires your graphics card to support at least one of these sampleCountBits: ${desiredSampleCounts.contentToString()}",
                "but it only supports $supportedSampleCounts"
            ))
        }
    }
}
