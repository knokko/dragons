package dragons.plugins.standard.vulkan.pipeline

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo
import org.lwjgl.vulkan.VkPipelineDepthStencilStateCreateInfo

fun createBasicDepthStencilState(stack: MemoryStack): VkPipelineDepthStencilStateCreateInfo {
    val ciDepthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack)
    ciDepthStencil.sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
    ciDepthStencil.depthTestEnable(true)
    ciDepthStencil.depthWriteEnable(true)
    ciDepthStencil.depthCompareOp(VK_COMPARE_OP_LESS)
    ciDepthStencil.depthBoundsTestEnable(false)
    ciDepthStencil.stencilTestEnable(false)

    return ciDepthStencil
}

fun createBasicColorBlendState(stack: MemoryStack): VkPipelineColorBlendStateCreateInfo {
    val attachments = VkPipelineColorBlendAttachmentState.calloc(1, stack)
    val attachment = attachments[0]
    attachment.blendEnable(false)
    attachment.colorWriteMask(
        VK_COLOR_COMPONENT_R_BIT or VK_COLOR_COMPONENT_G_BIT or VK_COLOR_COMPONENT_B_BIT or VK_COLOR_COMPONENT_A_BIT
    )

    val ciBlend = VkPipelineColorBlendStateCreateInfo.calloc(stack)
    ciBlend.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
    ciBlend.logicOpEnable(false)
    ciBlend.pAttachments(attachments)

    return ciBlend
}
