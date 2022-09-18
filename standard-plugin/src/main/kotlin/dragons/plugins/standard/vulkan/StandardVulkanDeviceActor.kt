package dragons.plugins.standard.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.vulkan.VulkanDeviceActor
import org.lwjgl.vulkan.EXTMemoryBudget.VK_EXT_MEMORY_BUDGET_EXTENSION_NAME
import org.lwjgl.vulkan.KHRBindMemory2.VK_KHR_BIND_MEMORY_2_EXTENSION_NAME
import org.lwjgl.vulkan.KHRDedicatedAllocation.VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME
import org.lwjgl.vulkan.KHRDrawIndirectCount.VK_KHR_DRAW_INDIRECT_COUNT_EXTENSION_NAME
import org.lwjgl.vulkan.KHRGetMemoryRequirements2.VK_KHR_GET_MEMORY_REQUIREMENTS_2_EXTENSION_NAME
import org.lwjgl.vulkan.KHRShaderDrawParameters.VK_KHR_SHADER_DRAW_PARAMETERS_EXTENSION_NAME

@Suppress("unused")
class StandardVulkanDeviceActor: VulkanDeviceActor {
    override fun manipulateVulkanDevice(pluginInstance: PluginInstance, agent: VulkanDeviceActor.Agent) {

        // Required Vulkan 1.0 features
        agent.requiredFeatures.samplerAnisotropy(true)
        agent.requiredFeatures.tessellationShader(true)
        agent.requiredFeatures.drawIndirectFirstInstance(true)

        // Required Vulkan 1.0 extensions
        agent.requiredExtensions.add(VK_KHR_SHADER_DRAW_PARAMETERS_EXTENSION_NAME)
        agent.requiredExtensions.add(VK_KHR_DRAW_INDIRECT_COUNT_EXTENSION_NAME)

        // These extensions improve the performance of the Vulkan Memory Allocator (VMA) library
        // The game core will check whether these extensions are enabled, and inform the VMA about this
        if (
            agent.availableExtensions.contains(VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME)
        ) {
            agent.requestedExtensions.add(VK_KHR_GET_MEMORY_REQUIREMENTS_2_EXTENSION_NAME)
            agent.requestedExtensions.add(VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME)
        }
        agent.requestedExtensions.add(VK_KHR_BIND_MEMORY_2_EXTENSION_NAME)

        // This extension depends on VK_KHR_get_physical_device_properties2, which is requested by StandardVulkanInstanceActor
        // If that extension is not available, VK_EXT_memory_budget must not be available either, so this is safe
        agent.requestedExtensions.add(VK_EXT_MEMORY_BUDGET_EXTENSION_NAME)
    }
}
