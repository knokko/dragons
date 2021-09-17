package dragons.plugins.standard.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.vulkan.VulkanDeviceDestructionListener
import dragons.plugins.standard.state.StandardPluginState
import dragons.plugins.standard.vulkan.pipeline.destroyBasicGraphicsPipeline
import kotlinx.coroutines.runBlocking
import org.lwjgl.vulkan.VK12.vkDestroyRenderPass
import org.slf4j.LoggerFactory.getLogger

class StandardVulkanDeviceCleanUp: VulkanDeviceDestructionListener {

    override fun beforeDeviceDestruction(
        pluginInstance: PluginInstance,
        agent: VulkanDeviceDestructionListener.BeforeAgent
    ) {
        runBlocking {
            val state = pluginInstance.state as StandardPluginState
            val logger = getLogger("Vulkan")
            if (state.hasBasicGraphicsPipeline()) {
                logger.info("Destroying basic graphics pipeline...")
                destroyBasicGraphicsPipeline(agent.vulkanDevice, state.basicGraphicsPipeline.await())
                logger.info("Destroyed basic graphics pipeline")
            }
            if (state.hasBasicRenderPass()) {
                logger.info("Destroying basic render pass...")
                vkDestroyRenderPass(agent.vulkanDevice, state.basicRenderPass.await(), null)
                logger.info("Destroyed basic render pass")
            }
        }
    }
}
