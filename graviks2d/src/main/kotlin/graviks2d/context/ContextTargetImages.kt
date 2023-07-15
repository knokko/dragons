package graviks2d.context

import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.vulkan.VK10.*
import troll.images.VmaImage

internal const val TARGET_COLOR_FORMAT = VK_FORMAT_R8G8B8A8_UNORM

internal class ContextTargetImages(
    val context: GraviksContext
) {
    val colorImage: VmaImage

    val framebuffer: Long

    init {
        val troll = context.instance.troll
        stackPush().use { stack ->
            this.colorImage = troll.images.createSimple(
                stack, context.width, context.height, TARGET_COLOR_FORMAT, VK_SAMPLE_COUNT_1_BIT,
                VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT or VK_IMAGE_USAGE_TRANSFER_SRC_BIT,
                VK_IMAGE_ASPECT_COLOR_BIT, "GraviksTarget"
            )

            this.framebuffer = troll.images.createFramebuffer(
                stack, context.instance.pipeline.vkRenderPass,
                context.width, context.height, "GraviksTarget", colorImage.vkImageView
            )
        }
    }

    fun destroy() {
        val vkDevice = this.context.instance.troll.vkDevice()
        val vmaAllocator = this.context.instance.troll.vmaAllocator()

        vkDestroyFramebuffer(vkDevice, this.framebuffer, null)
        vkDestroyImageView(vkDevice, this.colorImage.vkImageView, null)
        vmaDestroyImage(vmaAllocator, this.colorImage.vkImage, this.colorImage.vmaAllocation)
    }
}
