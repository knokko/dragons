package dragons.plugins.standard.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.vulkan.VulkanDeviceRater
import org.lwjgl.vulkan.VK12.*

class StandardVulkanDeviceRater: VulkanDeviceRater {
    override fun ratePhysicalDevice(pluginInstance: PluginInstance, agent: VulkanDeviceRater.Agent) {

        fun missingFeatureRating(feature: String) = VulkanDeviceRater.Rating.insufficient(
            VulkanDeviceRater.InsufficientReason.missesRequiredFeature(feature)
        )

        if (!agent.availableFeatures12.drawIndirectCount()) {
            agent.rating = missingFeatureRating("drawIndirectCount")
            return
        }

        if (!agent.availableFeatures11.shaderDrawParameters()) {
            agent.rating = missingFeatureRating("shaderDrawParameters")
            return
        }

        if (!agent.availableFeatures10.drawIndirectFirstInstance()) {
            agent.rating = missingFeatureRating("drawIndirectFirstInstance")
            return
        }
        if (!agent.availableFeatures10.tessellationShader()) {
            agent.rating = missingFeatureRating("tessellationShader")
            return
        }
        if (!agent.availableFeatures10.samplerAnisotropy()) {
            agent.rating = missingFeatureRating("samplerAnisotropy")
            return
        }

        var score = 0u
        if (agent.properties10.deviceType() == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) {
            score += 2000u
        }
        if (agent.properties10.deviceType() == VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU) {
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
