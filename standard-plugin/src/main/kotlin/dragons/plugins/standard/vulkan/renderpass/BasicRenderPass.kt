package dragons.plugins.standard.vulkan.renderpass

import dragons.vulkan.RenderImageInfo
import dragons.vulkan.util.assertVkSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.*

fun createBasicRenderPass(vkDevice: VkDevice, renderImageInfo: RenderImageInfo): Long {
    MemoryStack.stackPush().use { stack ->
        val attachments = VkAttachmentDescription.calloc(3, stack)

        val colorAttachment = attachments[0]
        colorAttachment.format(renderImageInfo.colorFormat)
        colorAttachment.samples(renderImageInfo.sampleCountBit)
        colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
        colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE)
        colorAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
        colorAttachment.finalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)

        val depthAttachment = attachments[1]
        depthAttachment.format(renderImageInfo.depthStencilFormat)
        depthAttachment.samples(renderImageInfo.sampleCountBit)
        depthAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
        depthAttachment.storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
        depthAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
        depthAttachment.finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)

        val resolveAttachment = attachments[2]
        resolveAttachment.format(renderImageInfo.colorFormat)
        resolveAttachment.samples(VK_SAMPLE_COUNT_1_BIT)
        resolveAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
        resolveAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE)
        resolveAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
        resolveAttachment.finalLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)

        val refColorAttachments = VkAttachmentReference.calloc(1, stack)
        val refColorAttachment = refColorAttachments[0]
        refColorAttachment.attachment(0)
        refColorAttachment.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)

        val refDepthAttachment = VkAttachmentReference.calloc(stack)
        refDepthAttachment.attachment(1)
        refDepthAttachment.layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)

        val refResolveAttachments = VkAttachmentReference.calloc(1, stack)
        val refResolveAttachment = refResolveAttachments[0]
        refResolveAttachment.attachment(2)
        refResolveAttachment.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)

        val subpasses = VkSubpassDescription.calloc(1, stack)
        // Just 1 subpass
        val subpass = subpasses[0]
        subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
        subpass.colorAttachmentCount(1)
        subpass.pColorAttachments(refColorAttachments)
        subpass.pDepthStencilAttachment(refDepthAttachment)
        subpass.pResolveAttachments(refResolveAttachments)
        // No input, resolve, and preserve attachments

        val ciRenderPass = VkRenderPassCreateInfo.calloc(stack)
        ciRenderPass.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
        ciRenderPass.pAttachments(attachments)
        ciRenderPass.pSubpasses(subpasses)
        // No dependencies

        val pRenderPass = stack.callocLong(1)
        assertVkSuccess(
            vkCreateRenderPass(vkDevice, ciRenderPass, null, pRenderPass),
            "CreateRenderPass", "standard plugin: basic"
        )
        return pRenderPass[0]
    }
}