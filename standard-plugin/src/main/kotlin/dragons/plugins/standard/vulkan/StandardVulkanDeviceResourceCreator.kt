package dragons.plugins.standard.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.vulkan.VulkanDeviceCreationListener
import dragons.plugins.standard.state.StandardPluginState
import dragons.plugins.standard.state.StandardPreGraphicsState
import dragons.plugins.standard.vulkan.pipeline.createBasicDescriptorPool
import dragons.plugins.standard.vulkan.pipeline.createBasicDescriptorSet
import dragons.plugins.standard.vulkan.pipeline.createBasicGraphicsPipeline
import dragons.plugins.standard.vulkan.pipeline.createBasicSampler
import dragons.plugins.standard.vulkan.renderpass.createBasicRenderPass
import kotlinx.coroutines.async
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

            val descriptorPool = createBasicDescriptorPool(agent.vulkanDevice)
            state.preGraphics.basicDescriptorPool.complete(descriptorPool)

            val descriptorSetLayout = state.preGraphics.basicGraphicsPipeline.await().descriptorSetLayout

            val cameraDeviceBuffer = state.preGraphics.cameraDeviceBuffer.await()
            val transformationMatrixDeviceBuffer = state.preGraphics.transformationMatrixDeviceBuffer.await()

            // TODO Move this to a distinct descriptor set that is filled later
            val testColorImage = state.preGraphics.testColorImage.await()
            val testHeightImage = state.preGraphics.testHeightImage.await()

            state.preGraphics.basicDescriptorSet.complete(createBasicDescriptorSet(
                agent.vulkanDevice, descriptorPool, descriptorSetLayout, cameraDeviceBuffer, transformationMatrixDeviceBuffer,
                basicSampler, listOf(testColorImage), listOf(testHeightImage)
            ))
        }
    }
}
