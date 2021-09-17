package dragons.plugins.standard.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.vulkan.VulkanDeviceDestructionListener
import dragons.plugins.standard.state.StandardPluginState
import dragons.plugins.standard.vulkan.pipeline.destroyBasicGraphicsPipeline

class StandardVulkanDeviceCleanUp: VulkanDeviceDestructionListener {

    override fun beforeDeviceDestruction(
        pluginInstance: PluginInstance,
        agent: VulkanDeviceDestructionListener.BeforeAgent
    ) {
        val state = pluginInstance.state as StandardPluginState
        if (state.hasBasicGraphicsPipeline()) {
            destroyBasicGraphicsPipeline(agent.vulkanDevice, state.basicGraphicsPipeline)
        }
        // TODO Destroy basic render pass
    }
}