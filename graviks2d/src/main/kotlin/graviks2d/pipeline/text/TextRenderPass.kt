package graviks2d.pipeline.text

import graviks2d.util.assertSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

internal fun createTextRenderPass(
    vkDevice: VkDevice, stack: MemoryStack
): Long {

    // TODO Handle multisampling
    val attachments = VkAttachmentDescription.calloc(2, stack)
    val countAttachment = attachments[0]
    countAttachment.format(TEXT_COLOR_FORMAT)
    countAttachment.samples(VK_SAMPLE_COUNT_1_BIT)
    countAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
    countAttachment.storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
    countAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
    countAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
    countAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
    countAttachment.finalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
    val oddAttachment = attachments[1]
    oddAttachment.format(TEXT_COLOR_FORMAT)
    oddAttachment.samples(VK_SAMPLE_COUNT_1_BIT)
    oddAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
    oddAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE)
    oddAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
    oddAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
    oddAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
    oddAttachment.finalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)

    val colorReferences1 = VkAttachmentReference.calloc(1, stack)
    val countReference1 = colorReferences1[0]
    countReference1.attachment(0)
    countReference1.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)

    val colorReferences2 = VkAttachmentReference.calloc(1, stack)
    val oddReference2 = colorReferences2[0]
    oddReference2.attachment(1)
    oddReference2.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)

    val inputReferences2 = VkAttachmentReference.calloc(1, stack)
    val countReference2 = inputReferences2[0]
    countReference2.attachment(0)
    countReference2.layout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)

    val subpasses = VkSubpassDescription.calloc(2, stack)
    val subpass1 = subpasses[0]
    subpass1.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
    subpass1.colorAttachmentCount(1)
    subpass1.pColorAttachments(colorReferences1)
    subpass1.pDepthStencilAttachment(null)
    val subpass2 = subpasses[1]
    subpass2.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
    subpass2.pInputAttachments(inputReferences2)
    subpass2.colorAttachmentCount(1)
    subpass2.pColorAttachments(colorReferences2)
    subpass2.pDepthStencilAttachment(null)

    val subpassDependencies = VkSubpassDependency.calloc(1, stack)
    val subpassDependency = subpassDependencies[0]
    subpassDependency.srcSubpass(0)
    subpassDependency.dstSubpass(1)
    subpassDependency.srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
    subpassDependency.dstStageMask(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT)
    subpassDependency.srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
    subpassDependency.dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
    subpassDependency.dependencyFlags(VK_DEPENDENCY_BY_REGION_BIT)

    val ciRenderPass = VkRenderPassCreateInfo.calloc(stack)
    ciRenderPass.`sType$Default`()
    ciRenderPass.pAttachments(attachments)
    ciRenderPass.pSubpasses(subpasses)
    ciRenderPass.pDependencies(subpassDependencies)

    val pRenderPass = stack.callocLong(1)
    assertSuccess(
        vkCreateRenderPass(vkDevice, ciRenderPass, null, pRenderPass),
        "vkCreateRenderPass"
    )
    return pRenderPass[0]
}
