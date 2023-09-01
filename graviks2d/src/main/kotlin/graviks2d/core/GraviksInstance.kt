package graviks2d.core

import com.github.knokko.boiler.instance.BoilerInstance
import graviks2d.pipeline.GraviksPipeline
import graviks2d.pipeline.text.TextPipeline
import graviks2d.resource.image.ImageCache
import graviks2d.resource.image.createDummyImage
import graviks2d.resource.text.FontManager
import graviks2d.resource.text.FontReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.util.vma.Vma.vmaDestroyImage
import org.lwjgl.vulkan.VK10.*

class GraviksInstance(
    val boiler: BoilerInstance,
    defaultFont: FontReference = FontReference.fromClassLoaderPath("graviks2d/fonts/default.ttf"),

    val maxNumDescriptorImages: Int = 100,
    softImageLimit: Int = 1000
) {

    internal val fontManager = FontManager(defaultFont)
    internal val textureSampler: Long
    internal val smoothTextureSampler: Long
    internal val pipeline = GraviksPipeline(this)
    internal val textPipelines = TextPipeline(boiler)
    internal val coroutineScope = CoroutineScope(Dispatchers.IO)
    internal val imageCache = ImageCache(this, softImageLimit)
    internal var dummyImage = createDummyImage(this)

    init {
        stackPush().use { stack ->
            textureSampler = boiler.images.simpleSampler(
                stack, VK_FILTER_NEAREST, VK_SAMPLER_MIPMAP_MODE_NEAREST, VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE,
                "GraviksPixelatedSampler"
            )
            smoothTextureSampler = boiler.images.simpleSampler(
                stack, VK_FILTER_LINEAR, VK_SAMPLER_MIPMAP_MODE_NEAREST, VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE,
                "GraviksSmoothSampler"
            )
        }
    }

    /**
     * Note: you must destroy all contexts **before** destroying this instance.
     */
    fun destroy() {
        vkDestroyImageView(boiler.vkDevice(), dummyImage.vkImageView, null)
        vmaDestroyImage(boiler.vmaAllocator(), dummyImage.vkImage, dummyImage.vmaAllocation)
        imageCache.destroy()
        coroutineScope.cancel()
        textPipelines.destroy()
        pipeline.destroy()
        vkDestroySampler(boiler.vkDevice(), textureSampler, null)
        vkDestroySampler(boiler.vkDevice(), smoothTextureSampler, null)
    }
}
