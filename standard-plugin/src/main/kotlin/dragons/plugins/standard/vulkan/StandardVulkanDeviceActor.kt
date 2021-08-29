package dragons.plugins.standard.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.vulkan.VulkanDeviceActor

class StandardVulkanDeviceActor: VulkanDeviceActor {
    override fun manipulateVulkanDevice(pluginInstance: PluginInstance, agent: VulkanDeviceActor.Agent) {

        // Required Vulkan 1.0 features
        agent.requiredFeatures10.samplerAnisotropy(true)
        agent.requiredFeatures10.tessellationShader(true)
        agent.requiredFeatures10.drawIndirectFirstInstance(true)

        // Required Vulkan 1.1 features
        agent.requiredFeatures11.shaderDrawParameters(true)

        // Required Vulkan 1.2 features
        agent.requiredFeatures12.drawIndirectCount(true)
    }
}
