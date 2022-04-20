package dragons.plugins.standard.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.vulkan.VulkanDeviceActor
import org.lwjgl.vulkan.KHRDrawIndirectCount.VK_KHR_DRAW_INDIRECT_COUNT_EXTENSION_NAME
import org.lwjgl.vulkan.KHRShaderDrawParameters.VK_KHR_SHADER_DRAW_PARAMETERS_EXTENSION_NAME

class StandardVulkanDeviceActor: VulkanDeviceActor {
    override fun manipulateVulkanDevice(pluginInstance: PluginInstance, agent: VulkanDeviceActor.Agent) {

        // Required Vulkan 1.0 features
        agent.requiredFeatures.samplerAnisotropy(true)
        agent.requiredFeatures.tessellationShader(true)
        agent.requiredFeatures.drawIndirectFirstInstance(true)

        // Required Vulkan 1.0 extensions
        agent.requiredExtensions.add(VK_KHR_SHADER_DRAW_PARAMETERS_EXTENSION_NAME)
        agent.requiredExtensions.add(VK_KHR_DRAW_INDIRECT_COUNT_EXTENSION_NAME)
    }
}
