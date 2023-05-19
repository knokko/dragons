package dsl.pm2.renderer.pipeline

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo
import org.lwjgl.vulkan.VkPipelineDepthStencilStateCreateInfo
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo

internal fun createViewportState(stack: MemoryStack): VkPipelineViewportStateCreateInfo {
    val ciViewport = VkPipelineViewportStateCreateInfo.calloc(stack)
    ciViewport.`sType$Default`()
    ciViewport.flags(0)
    ciViewport.viewportCount(1)
    ciViewport.pViewports(null)
    ciViewport.scissorCount(1)
    ciViewport.pScissors(null)

    return ciViewport
}

internal fun createRasterizationState(stack: MemoryStack): VkPipelineRasterizationStateCreateInfo {
    val ciRaster = VkPipelineRasterizationStateCreateInfo.calloc(stack)
    ciRaster.`sType$Default`()
    ciRaster.flags(0)
    ciRaster.depthClampEnable(false)
    ciRaster.rasterizerDiscardEnable(false)
    ciRaster.polygonMode(VK_POLYGON_MODE_FILL)
    ciRaster.cullMode(VK_CULL_MODE_NONE) // TODO Should really enable this when upgrading to 3d
    ciRaster.frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE)
    ciRaster.depthBiasEnable(false)
    ciRaster.lineWidth(1f)

    return ciRaster
}

internal fun createDepthStencilState(stack: MemoryStack): VkPipelineDepthStencilStateCreateInfo {
    val depthState = VkPipelineDepthStencilStateCreateInfo.calloc(stack)
    depthState.`sType$Default`()
    depthState.flags(0)
    depthState.depthTestEnable(false) // TODO Should really enable these when moving to 3d
    depthState.depthWriteEnable(false)
    depthState.depthCompareOp(VK_COMPARE_OP_LESS)
    depthState.depthBoundsTestEnable(false)
    depthState.stencilTestEnable(false)

    return depthState
}
