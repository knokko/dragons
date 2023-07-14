package graviks2d.core

import graviks2d.pipeline.GraviksPipeline
import graviks2d.pipeline.text.TextPipeline
import graviks2d.resource.image.ImageCache
import graviks2d.resource.image.createDummyImage
import graviks2d.resource.text.FontManager
import graviks2d.resource.text.FontReference
import graviks2d.util.assertSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.util.vma.Vma.vmaDestroyImage
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import troll.instance.TrollInstance

class GraviksInstance(
    val troll: TrollInstance,
    defaultFont: FontReference = FontReference.fromClassLoaderPath("graviks2d/fonts/default.ttf"),

    val maxNumDescriptorImages: Int = 100,
    softImageLimit: Int = 1000
) {

    internal val fontManager = FontManager(defaultFont)
    internal val textureSampler = createTextureSampler(troll.vkDevice(), VK_FILTER_NEAREST)
    internal val smoothTextureSampler = createTextureSampler(troll.vkDevice(), VK_FILTER_LINEAR)
    internal val pipeline = GraviksPipeline(this)
    internal val textPipelines = TextPipeline(troll.vkDevice())
    internal val coroutineScope = CoroutineScope(Dispatchers.IO)
    internal val imageCache = ImageCache(this, softImageLimit)
    internal var dummyImage = createDummyImage(this)

    /**
     * Note: you must destroy all contexts **before** destroying this instance.
     */
    fun destroy() {
        vkDestroyImageView(troll.vkDevice(), dummyImage.vkImageView, null)
        vmaDestroyImage(troll.vmaAllocator(), dummyImage.vkImage, dummyImage.vmaAllocation)
        imageCache.destroy()
        coroutineScope.cancel()
        textPipelines.destroy()
        pipeline.destroy()
        vkDestroySampler(troll.vkDevice(), textureSampler, null)
        vkDestroySampler(troll.vkDevice(), smoothTextureSampler, null)
    }
}

private fun createTextureSampler(vkDevice: VkDevice, magMinFilter: Int): Long {
    return stackPush().use { stack ->
        val ciSampler = VkSamplerCreateInfo.calloc(stack)
        ciSampler.`sType$Default`()
        ciSampler.magFilter(magMinFilter)
        ciSampler.minFilter(magMinFilter)
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