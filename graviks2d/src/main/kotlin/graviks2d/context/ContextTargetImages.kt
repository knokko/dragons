package graviks2d.context

import graviks2d.util.assertSuccess
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.util.vma.VmaAllocationCreateInfo
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkFramebufferCreateInfo
import org.lwjgl.vulkan.VkImageCreateInfo
import org.lwjgl.vulkan.VkImageViewCreateInfo

internal const val TARGET_COLOR_FORMAT = VK_FORMAT_R8G8B8A8_UNORM

internal class ContextTargetImages(
    val context: GraviksContext
) {
    val colorImage: Long
    val colorImageView: Long
    val colorImageAllocation: Long

    val framebuffer: Long

    init {
        stackPush().use { stack ->
            val ciColorImage = VkImageCreateInfo.calloc(stack)
            ciColorImage.`sType$Default`()
            ciColorImage.imageType(VK_IMAGE_TYPE_2D)
            ciColorImage.format(TARGET_COLOR_FORMAT)
            ciColorImage.extent { it.set(this.context.width, this.context.height, 1) }
            ciColorImage.mipLevels(1)
            ciColorImage.arrayLayers(1)
            ciColorImage.samples(VK_SAMPLE_COUNT_1_BIT)
            ciColorImage.tiling(VK_IMAGE_TILING_OPTIMAL)
            ciColorImage.usage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT or VK_IMAGE_USAGE_TRANSFER_SRC_BIT)
            ciColorImage.sharingMode(VK_SHARING_MODE_EXCLUSIVE)
            ciColorImage.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)

            val ciColorAllocation = VmaAllocationCreateInfo.calloc(stack)
            ciColorAllocation.flags(VMA_ALLOCATION_CREATE_DEDICATED_MEMORY_BIT)
            ciColorAllocation.usage(VMA_MEMORY_USAGE_AUTO)

            val pColorImage = stack.callocLong(1)
            val pColorAllocation = stack.callocPointer(1)
            assertSuccess(
                vmaCreateImage(
                    this.context.instance.vmaAllocator,
                    ciColorImage, ciColorAllocation,
                    pColorImage, pColorAllocation, null
                ), "vmaCreateImage"
            )
            this.colorImage = pColorImage[0]
            this.colorImageAllocation = pColorAllocation[0]

            val ciColorView = VkImageViewCreateInfo.calloc(stack)
            ciColorView.`sType$Default`()
            ciColorView.image(this.colorImage)
            ciColorView.viewType(VK_IMAGE_VIEW_TYPE_2D)
            ciColorView.format(TARGET_COLOR_FORMAT)
            ciColorView.components {
                it.r(VK_COMPONENT_SWIZZLE_IDENTITY)
                it.g(VK_COMPONENT_SWIZZLE_IDENTITY)
                it.b(VK_COMPONENT_SWIZZLE_IDENTITY)
                it.a(VK_COMPONENT_SWIZZLE_IDENTITY)
            }
            ciColorView.subresourceRange {
                it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                it.baseMipLevel(0)
                it.baseArrayLayer(0)
                it.levelCount(1)
                it.layerCount(1)
            }

            val pColorView = stack.callocLong(1)
            assertSuccess(
                vkCreateImageView(this.context.instance.device, ciColorView, null, pColorView),
                "vkCreateImageView"
            )
            this.colorImageView = pColorView[0]

            val ciFramebuffer = VkFramebufferCreateInfo.calloc(stack)
            ciFramebuffer.`sType$Default`()
            ciFramebuffer.renderPass(context.instance.pipeline.vkRenderPass)
            ciFramebuffer.attachmentCount(1)
            ciFramebuffer.pAttachments(stack.longs(this.colorImageView))
            ciFramebuffer.width(context.width)
            ciFramebuffer.height(context.height)
            ciFramebuffer.layers(1)

            val pFramebuffer = stack.callocLong(1)
            assertSuccess(
                vkCreateFramebuffer(context.instance.device, ciFramebuffer, null, pFramebuffer),
                "vkCreateFramebuffer"
            )
            this.framebuffer = pFramebuffer[0]
        }
    }

    fun destroy() {
        val vkDevice = this.context.instance.device
        val vmaAllocator = this.context.instance.vmaAllocator

        vkDestroyFramebuffer(vkDevice, this.framebuffer, null)
        vkDestroyImageView(vkDevice, this.colorImageView, null)
        vmaDestroyImage(vmaAllocator, this.colorImage, this.colorImageAllocation)
    }
}
