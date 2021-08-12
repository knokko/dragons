package dragons.plugins.standard.vulkan

import dragons.plugin.interfaces.vulkan.VulkanInstanceActor

class StandardVulkanInstanceExtensions: VulkanInstanceActor {
    override fun manipulateVulkanInstance(agent: VulkanInstanceActor.Agent) {
        // TODO Remove this after testing
        agent.requiredExtensions.add("adfjasdf")
        agent.requiredExtensions.add("VK_KHR_surface")
    }
}
