package dragons.plugins.standard.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.vulkan.VulkanDeviceCreationListener
import dragons.plugins.standard.state.StandardPluginState
import dragons.plugins.standard.vulkan.pipeline.createBasicGraphicsPipeline
import dragons.plugins.standard.vulkan.renderpass.createBasicRenderPass
import kotlinx.coroutines.async

class StandardVulkanDeviceResourceCreator: VulkanDeviceCreationListener {
    override fun afterVulkanDeviceCreation(pluginInstance: PluginInstance, agent: VulkanDeviceCreationListener.Agent) {
        val state = pluginInstance.state as StandardPluginState
        val basicRenderPassTask = agent.gameInitScope.async {
            createBasicRenderPass(agent.vulkanDevice, agent.renderImageInfo)
        }
        state.basicRenderPass = basicRenderPassTask

        val basicGraphicsPipelineTask = agent.gameInitScope.async {
            createBasicGraphicsPipeline(agent.vulkanDevice, basicRenderPassTask.await(), agent.renderImageInfo)
        }

        state.basicGraphicsPipeline = basicGraphicsPipelineTask
    }
}
