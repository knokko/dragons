package graviks2d.pipeline.text

import graviks2d.util.assertSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

internal fun createTextRenderPass(
    vkDevice: VkDevice, stack: MemoryStack
): Long {

    // TODO Handle multisampling
    val attachments = VkAttachmentDescription.calloc(1, stack)
    val colorAttachment = attachments[0]
    colorAttachment.format(TEXT_COLOR_FORMAT)
    colorAttachment.samples(VK_SAMPLE_COUNT_1_BIT)
    colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
    colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE)
    colorAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
    colorAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
    colorAttachment.initialLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
    colorAttachment.finalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)

    val colorReferences = VkAttachmentReference.calloc(1, stack)
    val colorReference = colorReferences[0]
    colorReference.attachment(0)
    colorReference.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)

    val subpasses = VkSubpassDescription.calloc(1, stack)
    val subpass = subpasses[0]
    subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
    subpass.colorAttachmentCount(1)
    subpass.pColorAttachments(colorReferences)
    subpass.pDepthStencilAttachment(null)

    val ciRenderPass = VkRenderPassCreateInfo.calloc(stack)
    ciRenderPass.`sType$Default`()
    ciRenderPass.pAttachments(attachments)
    ciRenderPass.pSubpasses(subpasses)

    val pRenderPass = stack.callocLong(1)
    assertSuccess(
        vkCreateRenderPass(vkDevice, ciRenderPass, null, pRenderPass),
        "vkCreateRenderPass"
    )
    return pRenderPass[0]
}
