package graviks2d.core

import graviks2d.pipeline.GraviksPipeline
import graviks2d.pipeline.text.TextPipeline
import graviks2d.resource.image.ImageCache
import graviks2d.resource.image.createDummyImage
import graviks2d.resource.text.FontManager
import graviks2d.resource.text.FontReference
import graviks2d.resource.text.StbTrueTypeFont
import graviks2d.util.assertSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.util.vma.Vma.vmaDestroyImage
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

class GraviksInstance(
    val instance: VkInstance,
    val physicalDevice: VkPhysicalDevice,
    val device: VkDevice,
    val vmaAllocator: Long,
    val queueFamilyIndex: Int,
    /**
     * This instance will use `queueSubmit` instead of `vkQueueSubmit`. This method is expected to call `vkQueueSubmit`,
     * but possibly in a synchronized manner. This method has 2 purposes:
     * - The user can choose which `VkQueue` will be used by this `Graviks2dInstance`.
     * - If the user also needs this `VkQueue` for other purposes, the user can put synchronization logic inside this
     * method (since the `queue` used in `vkQueueSubmit` **must** be externally synchronized). Note that this
     * `Graviks2dInstance` will synchronize all calls to `queueSubmit`, so user synchronization is only needed if the
     * used `VkQueue` is also used for other purposes.
     */
    private val queueSubmit: (VkSubmitInfo.Buffer, Long) -> Int,

    defaultFont: FontReference = FontReference.fromClassLoaderPath("graviks2d/fonts/default.ttf"),

    val maxNumDescriptorImages: Int = 100,
    softImageLimit: Int = 1000
) {

    internal val fontManager = FontManager(defaultFont)
    internal val textureSampler = createTextureSampler(this.device)
    internal val pipeline = GraviksPipeline(this)
    internal val textPipeline = TextPipeline(this.device)
    internal val coroutineScope = CoroutineScope(Dispatchers.IO)
    internal val imageCache = ImageCache(this, softImageLimit)
    internal var dummyImage = createDummyImage(this)

    fun synchronizedQueueSubmit(pSubmitInfo: VkSubmitInfo.Buffer, fence: Long): Int {
        return synchronized(queueSubmit) {
            queueSubmit(pSubmitInfo, fence)
        }
    }

    /**
     * Note: you must destroy all contexts **before** destroying this instance.
     */
    fun destroy() {
        vkDestroyImageView(device, dummyImage.vkImageView, null)
        vmaDestroyImage(vmaAllocator, dummyImage.vkImage, dummyImage.vmaAllocation)
        imageCache.destroy()
        coroutineScope.cancel()
        textPipeline.destroy()
        pipeline.destroy()
        vkDestroySampler(device, textureSampler, null)
    }
}

private fun createTextureSampler(vkDevice: VkDevice): Long {
    return stackPush().use { stack ->
        val ciSampler = VkSamplerCreateInfo.calloc(stack)
        ciSampler.`sType$Default`()
        ciSampler.magFilter(VK_FILTER_NEAREST)
        ciSampler.minFilter(VK_FILTER_NEAREST)
        ciSampler.mipmapMode(VK_SAMPLER_MIPMAP_MODE_NEAREST)
        ciSampler.addressModeU(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
        ciSampler.addressModeV(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
        ciSampler.addressModeW(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
        ciSampler.mipLodBias(0f)
        ciSampler.anisotropyEnable(false)
        ciSampler.compareEnable(false)
        ciSampler.compareOp(VK_COMPARE_OP_ALWAYS)
        ciSampler.minLod(0f)
        ciSampler.maxLod(0f)
        ciSampler.borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK)
        ciSampler.unnormalizedCoordinates(false)

        val pSampler = stack.callocLong(1)
        assertSuccess(
            vkCreateSampler(vkDevice, ciSampler, null, pSampler),
            "vkCreateSampler"
        )
        pSampler[0]
    }
}