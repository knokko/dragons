package dragons.plugins.standard.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.vulkan.VulkanDeviceCreationListener
import dragons.plugins.standard.state.StandardPluginState
import dragons.plugins.standard.vulkan.pipeline.createBasicGraphicsPipeline

class StandardVulkanDeviceResourceCreator: VulkanDeviceCreationListener {
    override fun afterVulkanDeviceCreation(pluginInstance: PluginInstance, agent: VulkanDeviceCreationListener.Agent) {
        val state = pluginInstance.state as StandardPluginState
        // TODO Launch this in a coroutine instead and create the basic render pass
        state.basicGraphicsPipeline = createBasicGraphicsPipeline(agent.vulkanDevice, basicRenderPass, renderImageInfo)
    }
}
