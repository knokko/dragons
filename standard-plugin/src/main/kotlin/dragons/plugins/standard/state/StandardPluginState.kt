package dragons.plugins.standard.state

import dragons.plugins.standard.vulkan.pipeline.BasicGraphicsPipeline

class StandardPluginState {

    lateinit var basicGraphicsPipeline: BasicGraphicsPipeline

    fun hasBasicGraphicsPipeline() = this::basicGraphicsPipeline.isInitialized
}
