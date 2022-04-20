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

internal fun createTextCountPipelineColorBlend(
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

internal fun createTextOddPipelineColorBlend(
    stack: MemoryStack
): VkPipelineColorBlendStateCreateInfo {

    val attachments = VkPipelineColorBlendAttachmentState.calloc(1, stack)
    attachments[0].blendEnable(false)
    attachments[0].colorWriteMask(VK_COLOR_COMPONENT_R_BIT)

    val ciColorBlend = VkPipelineColorBlendStateCreateInfo.calloc(stack)
    ciColorBlend.`sType$Default`()
    ciColorBlend.logicOpEnable(false)
    ciColorBlend.pAttachments(attachments)

    return ciColorBlend
}

internal fun createTextPipelineMultisampleState(
    stack: MemoryStack
): VkPipelineMultisampleStateCreateInfo {
    // Note: instead of using multisampling, the text renderer simply claims bigger space for some characters and
    // downscales them when drawing
    val ciMultisample = VkPipelineMultisampleStateCreateInfo.calloc(stack)
    ciMultisample.`sType$Default`()
    ciMultisample.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)
    ciMultisample.sampleShadingEnable(false)

    return ciMultisample
}
