package graviks2d.pipeline

import graviks2d.util.assertSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

internal fun createGraviksRenderPass(
    physicalDevice: VkPhysicalDevice, vkDevice: VkDevice, colorFormat: Int, stack: MemoryStack
): Long {

    val attachments = VkAttachmentDescription.calloc(2, stack)
    val colorAttachment = attachments[0]
    colorAttachment.format(colorFormat)
    colorAttachment.samples(VK_SAMPLE_COUNT_1_BIT)
    colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_LOAD)
    colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE)
    colorAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
    colorAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
    colorAttachment.initialLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
    colorAttachment.finalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)

    val depthAttachment = attachments[1]
    depthAttachment.format(determineDepthFormat(physicalDevice, stack))
    depthAttachment.samples(VK_SAMPLE_COUNT_1_BIT)
    depthAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_LOAD)
    depthAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE)
    depthAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
    depthAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
    depthAttachment.initialLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)
    depthAttachment.finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)

    val colorReferences = VkAttachmentReference.calloc(1, stack)
    val colorReference = colorReferences[0]
    colorReference.attachment(0)
    colorReference.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)

    val depthReference = VkAttachmentReference.calloc(stack)
    depthReference.attachment(1)
    depthReference.layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)

    val subpasses = VkSubpassDescription.calloc(1, stack)
    val subpass = subpasses[0]
    subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
    subpass.colorAttachmentCount(1)
    subpass.pColorAttachments(colorReferences)
    subpass.pDepthStencilAttachment(depthReference)

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

private fun determineDepthFormat(
    physicalDevice: VkPhysicalDevice, stack: MemoryStack
): Int {

    // The more bytes are spent on the depth, the more accurate the results, but the
    // more expensive rendering becomes. 3 bytes should be enough and are therefor
    // preferred over 4-byte formats.
    val preferredFormats = intArrayOf(
        VK_FORMAT_X8_D24_UNORM_PACK32,
        VK_FORMAT_D24_UNORM_S8_UINT,
        VK_FORMAT_D32_SFLOAT,
        VK_FORMAT_D32_SFLOAT_S8_UINT
    )

    val formatProps = VkFormatProperties.calloc(stack)

    for (format in preferredFormats) {
        vkGetPhysicalDeviceFormatProperties(physicalDevice, format, formatProps)
        if ((formatProps.optimalTilingFeatures() and VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT) != 0) {
            return format
        }
    }

    throw Error("Vulkan implementation must support at least 2 of these formats, so this shouldn't happen")
}
