package graviks2d.pipeline

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

internal fun createGraviksPipelineRasterization(
    stack: MemoryStack
): VkPipelineRasterizationStateCreateInfo {

    val ciRasterization = VkPipelineRasterizationStateCreateInfo.calloc(stack)
    ciRasterization.`sType$Default`()
    ciRasterization.depthClampEnable(false)
    ciRasterization.rasterizerDiscardEnable(false)
    ciRasterization.polygonMode(VK_POLYGON_MODE_FILL)
    ciRasterization.cullMode(VK_CULL_MODE_NONE)
    ciRasterization.frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE)
    ciRasterization.depthBiasEnable(false)
    ciRasterization.lineWidth(1f)

    return ciRasterization
}

internal fun createGraviksPipelineColorBlend(
    stack: MemoryStack
): VkPipelineColorBlendStateCreateInfo {

    val attachments = VkPipelineColorBlendAttachmentState.calloc(1, stack)
    val attachment = attachments[0]
    attachment.blendEnable(true)
    attachment.srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA)
    attachment.dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
    attachment.colorBlendOp(VK_BLEND_OP_ADD)
    attachment.srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE)
    attachment.dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO)
    attachment.alphaBlendOp(VK_BLEND_OP_ADD)
    attachment.colorWriteMask(
        VK_COLOR_COMPONENT_R_BIT or
                VK_COLOR_COMPONENT_G_BIT or
                VK_COLOR_COMPONENT_B_BIT or
                VK_COLOR_COMPONENT_A_BIT
    )

    val ciColorBlend = VkPipelineColorBlendStateCreateInfo.calloc(stack)
    ciColorBlend.`sType$Default`()
    ciColorBlend.logicOpEnable(false)
    ciColorBlend.pAttachments(attachments)

    return ciColorBlend
}

internal fun createGraviksPipelineMultisampleState(
    stack: MemoryStack
): VkPipelineMultisampleStateCreateInfo {
    val ciMultisample = VkPipelineMultisampleStateCreateInfo.calloc(stack)
    ciMultisample.`sType$Default`()
    ciMultisample.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)
    ciMultisample.sampleShadingEnable(false)

    return ciMultisample
}
