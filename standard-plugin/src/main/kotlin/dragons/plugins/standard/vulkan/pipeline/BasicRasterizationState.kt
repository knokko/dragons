package dragons.plugins.standard.vulkan.pipeline

import dragons.vulkan.RenderImageInfo
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.*

fun createBasicViewportState(stack: MemoryStack, width: Int, height: Int): VkPipelineViewportStateCreateInfo {
    val viewports = VkViewport.calloc(1, stack)
    val viewport = viewports[0]
    viewport.x(0f)
    viewport.y(0f)
    viewport.width(width.toFloat())
    viewport.height(height.toFloat())
    viewport.minDepth(0f)
    viewport.maxDepth(1f)

    val scissors = VkRect2D.calloc(1, stack)
    val scissor = scissors[0]
    scissor.offset { it.set(0, 0) }
    scissor.extent { it.set(width, height) }

    val ciViewport = VkPipelineViewportStateCreateInfo.calloc(stack)
    ciViewport.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
    ciViewport.viewportCount(1)
    ciViewport.pViewports(viewports)
    ciViewport.scissorCount(1)
    ciViewport.pScissors(scissors)

    return ciViewport
}

fun createBasicRasterizationState(stack: MemoryStack): VkPipelineRasterizationStateCreateInfo {
    val ciRasterization = VkPipelineRasterizationStateCreateInfo.calloc(stack)
    ciRasterization.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
    ciRasterization.depthClampEnable(false)
    ciRasterization.rasterizerDiscardEnable(false)
    ciRasterization.polygonMode(VK_POLYGON_MODE_FILL)
    ciRasterization.cullMode(VK_CULL_MODE_BACK_BIT)
    ciRasterization.frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE)
    ciRasterization.depthBiasEnable(false)
    ciRasterization.lineWidth(1f)

    return ciRasterization
}

fun createBasicMultisampleState(renderImageInfo: RenderImageInfo, stack: MemoryStack): VkPipelineMultisampleStateCreateInfo {
    val ciMultisample = VkPipelineMultisampleStateCreateInfo.calloc(stack)
    ciMultisample.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
    ciMultisample.rasterizationSamples(renderImageInfo.sampleCountBit)
    // TODO Check out sample shading
    ciMultisample.sampleShadingEnable(false)
    ciMultisample.pSampleMask(null)
    ciMultisample.alphaToCoverageEnable(false)
    ciMultisample.alphaToOneEnable(false)

    return ciMultisample
}
