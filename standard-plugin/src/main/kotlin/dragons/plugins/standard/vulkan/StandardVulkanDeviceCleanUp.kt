package dragons.plugins.standard.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.vulkan.VulkanDeviceDestructionListener
import dragons.plugins.standard.state.StandardPluginState
import dragons.plugins.standard.vulkan.pipeline.destroyBasicGraphicsPipeline
import kotlinx.coroutines.runBlocking
import org.lwjgl.vulkan.VK12.*
import org.slf4j.LoggerFactory.getLogger

class StandardVulkanDeviceCleanUp: VulkanDeviceDestructionListener {

    override fun beforeDeviceDestruction(
        pluginInstance: PluginInstance,
        agent: VulkanDeviceDestructionListener.BeforeAgent
    ) {
        runBlocking {
            val state = pluginInstance.state as StandardPluginState
            val logger = getLogger("Vulkan")
            if (state.hasGraphics()) {
                val graphicsState = state.graphics

                logger.info("Destroying basic graphics pipeline...")
                destroyBasicGraphicsPipeline(agent.vulkanDevice, graphicsState.basicGraphicsPipeline)
                logger.info("Destroyed basic graphics pipeline")

                logger.info("Destroying basic render pass...")
                vkDestroyRenderPass(agent.vulkanDevice, graphicsState.basicRenderPass, null)
                logger.info("Destroyed basic render pass")

                logger.info("Destroying basic framebuffers...")
                vkDestroyFramebuffer(agent.vulkanDevice, graphicsState.basicLeftFramebuffer, null)
                vkDestroyFramebuffer(agent.vulkanDevice, graphicsState.basicRightFramebuffer, null)
                logger.info("Destroyed basic framebuffers")

                logger.info("Destroying basic descriptor pools...")
                vkDestroyDescriptorPool(agent.vulkanDevice, graphicsState.basicStaticDescriptorPool, null)
                logger.info("Destroyed basic descriptor pools")

                logger.info("Destroying basic sampler...")
                vkDestroySampler(agent.vulkanDevice, graphicsState.basicSampler, null)
                logger.info("Destroyed basic sampler")
            }
        }
    }
}
