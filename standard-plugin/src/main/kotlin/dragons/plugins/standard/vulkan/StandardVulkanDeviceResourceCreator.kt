package dragons.plugins.standard.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.vulkan.VulkanDeviceCreationListener
import dragons.plugins.standard.state.StandardPluginState
import dragons.plugins.standard.vulkan.pipeline.*
import dragons.plugins.standard.vulkan.renderpass.createBasicRenderPass
import kotlinx.coroutines.launch

class StandardVulkanDeviceResourceCreator: VulkanDeviceCreationListener {
    override fun afterVulkanDeviceCreation(pluginInstance: PluginInstance, agent: VulkanDeviceCreationListener.Agent) {
        val state = pluginInstance.state as StandardPluginState
        agent.gameInitScope.launch {
            val basicRenderPass = createBasicRenderPass(agent.vulkanDevice, agent.renderImageInfo)
            state.preGraphics.basicRenderPass.complete(basicRenderPass)
            state.preGraphics.basicGraphicsPipeline.complete(createBasicGraphicsPipeline(
                agent.vulkanDevice, basicRenderPass, agent.renderImageInfo,
                agent.vrManager.getWidth(), agent.vrManager.getHeight()
            ))
        }

        agent.gameInitScope.launch {
            val basicSampler = createBasicSampler(agent.vulkanDevice)
            state.preGraphics.basicSampler.complete(basicSampler)

            val staticDescriptorPool = createBasicStaticDescriptorPool(agent.vulkanDevice)
            state.preGraphics.basicStaticDescriptorPool.complete(staticDescriptorPool)

            // TODO Remove this after refactoring
            val dynamicDescriptorPool = createBasicDynamicDescriptorPool(agent.vulkanDevice, 1)
            state.preGraphics.basicDynamicDescriptorPool.complete(dynamicDescriptorPool)

            val basicPipeline = state.preGraphics.basicGraphicsPipeline.await()
            val staticDescriptorSetLayout = basicPipeline.staticDescriptorSetLayout
            val dynamicDescriptorSetLayout = basicPipeline.dynamicDescriptorSetLayout

            val cameraDeviceBuffer = state.preGraphics.cameraDeviceBuffer.await()
            val transformationMatrixDeviceBuffer = state.preGraphics.transformationMatrixDeviceBuffer.await()

            state.preGraphics.basicStaticDescriptorSet.complete(createBasicStaticDescriptorSet(
                agent.vulkanDevice, staticDescriptorPool, staticDescriptorSetLayout,
                cameraDeviceBuffer, transformationMatrixDeviceBuffer, basicSampler
            ))

            val basicDynamicDescriptorSet = createBasicDynamicDescriptorSet(
                agent.vulkanDevice, dynamicDescriptorPool, dynamicDescriptorSetLayout
            )
            state.preGraphics.basicDynamicDescriptorSet.complete(basicDynamicDescriptorSet)
        }
    }
}
