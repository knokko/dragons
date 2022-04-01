package graviks2d.pipeline.text

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo
import org.lwjgl.vulkan.VkPipelineDepthStencilStateCreateInfo
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo

internal fun createTextPipelineDepthState(
    stack: MemoryStack
): VkPipelineDepthStencilStateCreateInfo {

    val ciDepth = VkPipelineDepthStencilStateCreateInfo.calloc(stack)
    ciDepth.`sType$Default`()
    ciDepth.depthTestEnable(false)
    ciDepth.depthWriteEnable(false)
    ciDepth.depthBoundsTestEnable(false)
    ciDepth.stencilTestEnable(false)

    return ciDepth
}

internal fun createTextPipelineColorBlend(
    stack: MemoryStack
): VkPipelineColorBlendStateCreateInfo {

    val attachments = VkPipelineColorBlendAttachmentState.calloc(1, stack)
    val attachment = attachments[0]
    attachment.blendEnable(true)
    attachment.srcColorBlendFactor(VK_BLEND_FACTOR_ONE)
    attachment.dstColorBlendFactor(VK_BLEND_FACTOR_ONE)
    attachment.colorBlendOp(VK_BLEND_OP_ADD)
    attachment.colorWriteMask(VK_COLOR_COMPONENT_R_BIT)

    val ciColorBlend = VkPipelineColorBlendStateCreateInfo.calloc(stack)
    ciColorBlend.`sType$Default`()
    ciColorBlend.logicOpEnable(false)
    ciColorBlend.pAttachments(attachments)

    return ciColorBlend
}

internal fun createTextPipelineMultisampleState(
    stack: MemoryStack
): VkPipelineMultisampleStateCreateInfo {
    val ciMultisample = VkPipelineMultisampleStateCreateInfo.calloc(stack)
    ciMultisample.`sType$Default`()
    // TODO Use more samples
    ciMultisample.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)
    ciMultisample.sampleShadingEnable(false)

    return ciMultisample
}
