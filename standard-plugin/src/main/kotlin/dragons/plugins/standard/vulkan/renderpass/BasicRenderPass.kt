package dragons.plugins.standard.vulkan.renderpass

import dragons.vulkan.RenderImageInfo
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.*
import org.slf4j.LoggerFactory.getLogger
import troll.exceptions.VulkanFailureException.assertVkSuccess

fun createBasicRenderPass(vkDevice: VkDevice, renderImageInfo: RenderImageInfo): Long {
    stackPush().use { stack ->
        val attachments = VkAttachmentDescription.calloc(2, stack)

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
        depthAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
        depthAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
        depthAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
        depthAttachment.finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)

        val refColorAttachments = VkAttachmentReference.calloc(1, stack)
        val refColorAttachment = refColorAttachments[0]
        refColorAttachment.attachment(0)
        refColorAttachment.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)

        val refDepthAttachment = VkAttachmentReference.calloc(stack)
        refDepthAttachment.attachment(1)
        refDepthAttachment.layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)

        val subpasses = VkSubpassDescription.calloc(1, stack)
        // Just 1 subpass
        val subpass = subpasses[0]
        subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
        subpass.colorAttachmentCount(1)
        subpass.pColorAttachments(refColorAttachments)
        subpass.pDepthStencilAttachment(refDepthAttachment)
        // No input, resolve, and preserve attachments

        val ciRenderPass = VkRenderPassCreateInfo.calloc(stack)
        ciRenderPass.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
        ciRenderPass.pAttachments(attachments)
        ciRenderPass.pSubpasses(subpasses)
        // No dependencies

        val logger = getLogger("Vulkan")
        val pRenderPass = stack.callocLong(1)
        logger.info("Creating basic render pass...")
        assertVkSuccess(
            vkCreateRenderPass(vkDevice, ciRenderPass, null, pRenderPass),
            "CreateRenderPass", "standard plugin: basic"
        )
        logger.info("Created basic render pass")
        return pRenderPass[0]
    }
}