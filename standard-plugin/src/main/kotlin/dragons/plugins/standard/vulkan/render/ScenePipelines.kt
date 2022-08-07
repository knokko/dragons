package dragons.plugins.standard.vulkan.render

import dragons.plugins.standard.vulkan.pipeline.BasicGraphicsPipeline

class ScenePipelines(
    val basicRenderPass: Long, val basicPipeline: BasicGraphicsPipeline, val staticDescriptorSet: Long
)
