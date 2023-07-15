package graviks2d.pipeline.text

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo

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
