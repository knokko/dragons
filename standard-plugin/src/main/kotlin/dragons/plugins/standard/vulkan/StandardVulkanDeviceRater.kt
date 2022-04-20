package dragons.plugins.standard.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.vulkan.VulkanDeviceRater
import org.lwjgl.vulkan.KHRDrawIndirectCount.VK_KHR_DRAW_INDIRECT_COUNT_EXTENSION_NAME
import org.lwjgl.vulkan.KHRShaderDrawParameters.VK_KHR_SHADER_DRAW_PARAMETERS_EXTENSION_NAME
import org.lwjgl.vulkan.VK12.*

class StandardVulkanDeviceRater: VulkanDeviceRater {
    override fun ratePhysicalDevice(pluginInstance: PluginInstance, agent: VulkanDeviceRater.Agent) {

        fun missingFeatureRating(feature: String) = VulkanDeviceRater.Rating.insufficient(
            VulkanDeviceRater.InsufficientReason.missesRequiredFeature(feature)
        )

        fun missingExtensionRating(extension: String) = VulkanDeviceRater.Rating.insufficient(
            VulkanDeviceRater.InsufficientReason.missesRequiredExtension(extension)
        )

        if (!agent.availableExtensions.contains(VK_KHR_DRAW_INDIRECT_COUNT_EXTENSION_NAME)) {
            agent.rating = missingExtensionRating(VK_KHR_DRAW_INDIRECT_COUNT_EXTENSION_NAME)
            return
        }

        if (!agent.availableExtensions.contains(VK_KHR_SHADER_DRAW_PARAMETERS_EXTENSION_NAME)) {
            agent.rating = missingExtensionRating(VK_KHR_SHADER_DRAW_PARAMETERS_EXTENSION_NAME)
            return
        }

        if (!agent.availableFeatures.drawIndirectFirstInstance()) {
            agent.rating = missingFeatureRating("drawIndirectFirstInstance")
            return
        }
        if (!agent.availableFeatures.tessellationShader()) {
            agent.rating = missingFeatureRating("tessellationShader")
            return
        }
        if (!agent.availableFeatures.samplerAnisotropy()) {
            agent.rating = missingFeatureRating("samplerAnisotropy")
            return
        }

        var score = 0u
        if (agent.properties.deviceType() == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) {
            score += 2000u
        }
        if (agent.properties.deviceType() == VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU) {
            score += 1000u
        }

        var deviceMemory = 0L
        for (heap in agent.memoryProperties.memoryHeaps()) {
            if ((heap.flags() and VK_MEMORY_HEAP_DEVICE_LOCAL_BIT) != 0) {
                deviceMemory += heap.size()
            }
        }

        score += (deviceMemory / 1_000_000_000L).toUInt()
        agent.rating = VulkanDeviceRater.Rating.sufficient(score)
    }
}
