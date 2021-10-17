package dragons.plugins.standard.state

import dragons.plugins.standard.vulkan.pipeline.BasicGraphicsPipeline
import kotlinx.coroutines.Deferred

class StandardPluginState {

    lateinit var basicGraphicsPipeline: Deferred<BasicGraphicsPipeline>

    fun hasBasicGraphicsPipeline() = this::basicGraphicsPipeline.isInitialized

    lateinit var basicRenderPass: Deferred<Long>

    fun hasBasicRenderPass() = this::basicRenderPass.isInitialized
}