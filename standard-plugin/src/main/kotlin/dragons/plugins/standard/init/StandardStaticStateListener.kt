package dragons.plugins.standard.init

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.general.StaticStateListener
import dragons.plugins.standard.state.StandardGraphicsBuffers
import dragons.plugins.standard.state.StandardGraphicsState
import dragons.plugins.standard.state.StandardPluginState
import dragons.plugins.standard.vulkan.panel.Panel
import dragons.vulkan.util.assertVkSuccess
import kotlinx.coroutines.runBlocking
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK12.vkCreateFramebuffer
import org.lwjgl.vulkan.VkFramebufferCreateInfo

class StandardStaticStateListener: StaticStateListener {
    override fun afterStaticStateCreation(pluginInstance: PluginInstance, agent: StaticStateListener.Agent) {
        val state = pluginInstance.state as StandardPluginState
        val (basicRenderPass, basicGraphicsPipeline) = runBlocking {
            Pair(state.preGraphics.basicRenderPass.await(), state.preGraphics.basicGraphicsPipeline.await())
        }
        stackPush().use { stack ->
            // TODO Try the multiview extension once basic rendering works

            val coreGraphics = agent.gameState.graphics
            val ciFramebuffer = VkFramebufferCreateInfo.calloc(stack)
            ciFramebuffer.`sType$Default`()
            ciFramebuffer.renderPass(basicRenderPass)
            ciFramebuffer.attachmentCount(2)
            // pAttachment will be set later
            ciFramebuffer.width(agent.gameState.vrManager.getWidth())
            ciFramebuffer.height(agent.gameState.vrManager.getHeight())
            ciFramebuffer.layers(1)

            val pFramebuffer = stack.callocLong(1)

            ciFramebuffer.pAttachments(stack.longs(
                coreGraphics.coreMemory.leftColorImage.fullView!!, coreGraphics.coreMemory.leftDepthImage.fullView!!
            ))
            assertVkSuccess(
                vkCreateFramebuffer(coreGraphics.vkDevice, ciFramebuffer, null, pFramebuffer),
                "CreateFramebuffer", "standard plug-in: basic left eye"
            )
            val leftFramebuffer = pFramebuffer[0]

            ciFramebuffer.pAttachments(stack.longs(
                coreGraphics.coreMemory.rightColorImage.fullView!!, coreGraphics.coreMemory.rightDepthImage.fullView!!
            ))
            assertVkSuccess(
                vkCreateFramebuffer(coreGraphics.vkDevice, ciFramebuffer, null, pFramebuffer),
                "CreateFramebuffer", "standard plug-in: basic right eye"
            )
            val rightFramebuffer = pFramebuffer[0]

            val (transformationMatrixHostBuffer, transformationMatrixStagingBuffer) = runBlocking {
                state.preGraphics.transformationMatrixStagingBuffer.await()
            }
            val (cameraHostBuffer, cameraStagingBuffer) = runBlocking {
                state.preGraphics.cameraStagingBuffer.await()
            }
            val (indirectDrawHostBuffer, indirectDrawDeviceBuffer) = runBlocking {
                state.preGraphics.indirectDrawingBuffer.await()
            }
            val (indirectDrawCountHostBuffer, indirectDrawCountDeviceBuffer) = runBlocking {
                state.preGraphics.indirectDrawCountBuffer.await()
            }

            state.graphics = runBlocking { StandardGraphicsState(
                basicGraphicsPipeline = basicGraphicsPipeline,
                basicRenderPass = basicRenderPass,
                basicLeftFramebuffer = leftFramebuffer,
                basicRightFramebuffer = rightFramebuffer,
                basicStaticDescriptorPool = state.preGraphics.basicStaticDescriptorPool.await(),
                basicDynamicDescriptorPool = state.preGraphics.basicDynamicDescriptorPool.await(),
                basicStaticDescriptorSet = state.preGraphics.basicStaticDescriptorSet.await(),
                basicDynamicDescriptorSet = state.preGraphics.basicDynamicDescriptorSet.await(),
                debugPanel = Panel(agent.gameState.graphics.graviksInstance, state.preGraphics.mainMenu.debugPanelTexture.image.await()),
                basicSampler = state.preGraphics.basicSampler.await(),

                buffers = StandardGraphicsBuffers(
                    transformationMatrixDevice = state.preGraphics.transformationMatrixDeviceBuffer.await(),
                    transformationMatrixStaging = transformationMatrixStagingBuffer,
                    transformationMatrixHost = transformationMatrixHostBuffer,
                    cameraDevice = state.preGraphics.cameraDeviceBuffer.await(),
                    cameraStaging = cameraStagingBuffer,
                    cameraHost = cameraHostBuffer,
                    indirectDrawDevice = indirectDrawDeviceBuffer,
                    indirectDrawHost = indirectDrawHostBuffer,
                    indirectDrawCountDevice = indirectDrawCountDeviceBuffer,
                    indirectDrawCountHost = indirectDrawCountHostBuffer,
                ),
                mainMenu = state.preGraphics.mainMenu.await(),
            )}
        }
    }
}
