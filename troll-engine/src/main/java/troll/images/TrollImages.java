package troll.images;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.*;
import troll.instance.TrollInstance;

import static org.lwjgl.util.vma.Vma.VMA_MEMORY_USAGE_AUTO;
import static org.lwjgl.util.vma.Vma.vmaCreateImage;
import static org.lwjgl.vulkan.VK10.*;
import static troll.exceptions.VulkanFailureException.assertVkSuccess;
import static troll.exceptions.VulkanFailureException.assertVmaSuccess;

public class TrollImages {

    private final TrollInstance instance;

    public TrollImages(TrollInstance instance) {
        this.instance = instance;
    }

    public VkImageSubresourceRange subresourceRange(MemoryStack stack, VkImageSubresourceRange range, int aspectMask) {
        if (range == null) range = VkImageSubresourceRange.calloc(stack);
        range.aspectMask(aspectMask);
        range.baseMipLevel(0);
        range.levelCount(1);
        range.baseArrayLayer(0);
        range.layerCount(1);
        return range;
    }

    public VkImageSubresourceLayers subresourceLayers(MemoryStack stack, VkImageSubresourceLayers range, int aspectMask) {
        if (range == null) range = VkImageSubresourceLayers.calloc(stack);
        range.aspectMask(aspectMask);
        range.mipLevel(0);
        range.baseArrayLayer(0);
        range.layerCount(1);
        return range;
    }

    public VmaImage createSimple(
            MemoryStack stack, int width, int height,
            int format, int samples, int usage, int aspectMask, String name
    ) {
        return createSimple(stack, width, height, format, samples, usage, aspectMask, true, name);
    }

    public VmaImage createSimple(
            MemoryStack stack, int width, int height,
            int format, int samples, int usage, int aspectMask, boolean createView, String name
    ) {
        var ciImage = VkImageCreateInfo.calloc(stack);
        ciImage.sType$Default();
        ciImage.imageType(VK_IMAGE_TYPE_2D);
        ciImage.format(format);
        ciImage.extent().set(width, height, 1);
        ciImage.mipLevels(1);
        ciImage.arrayLayers(1);
        ciImage.samples(samples);
        ciImage.tiling(VK_IMAGE_TILING_OPTIMAL);
        ciImage.usage(usage);
        ciImage.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
        ciImage.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);

        var ciAllocation = VmaAllocationCreateInfo.calloc(stack);
        ciAllocation.usage(VMA_MEMORY_USAGE_AUTO);

        var pImage = stack.callocLong(1);
        var pAllocation = stack.callocPointer(1);
        assertVmaSuccess(vmaCreateImage(
                instance.vmaAllocator(), ciImage, ciAllocation, pImage, pAllocation, null
        ), "CreateImage", name);
        long image = pImage.get(0);
        long allocation = pAllocation.get(0);
        instance.debug.name(stack, image, VK_OBJECT_TYPE_IMAGE, name);

        long view = createView ? createView(stack, image, format, aspectMask, name) : 0L;
        return new VmaImage(image, view, allocation, width, height);
    }

    public long createView(MemoryStack stack, long image, int format, int aspectMask, String name) {
        var ciImageView = VkImageViewCreateInfo.calloc(stack);
        ciImageView.sType$Default();
        ciImageView.image(image);
        ciImageView.viewType(VK_IMAGE_VIEW_TYPE_2D);
        ciImageView.format(format);
        ciImageView.components().set(
                VK_COMPONENT_SWIZZLE_IDENTITY, VK_COMPONENT_SWIZZLE_IDENTITY,
                VK_COMPONENT_SWIZZLE_IDENTITY, VK_COMPONENT_SWIZZLE_IDENTITY
        );
        instance.images.subresourceRange(stack, ciImageView.subresourceRange(), aspectMask);

        var pImageView = stack.callocLong(1);
        assertVkSuccess(vkCreateImageView(
                instance.vkDevice(), ciImageView, null, pImageView
        ), "CreateImageView", name);
        long imageView = pImageView.get(0);
        instance.debug.name(stack, imageView, VK_OBJECT_TYPE_IMAGE_VIEW, name);
        return imageView;
    }

    public long createFramebuffer(MemoryStack stack, long renderPass, int width, int height, String name, long... imageViews) {
        var ciFramebuffer = VkFramebufferCreateInfo.calloc(stack);
        ciFramebuffer.sType$Default();
        ciFramebuffer.flags(0);
        ciFramebuffer.renderPass(renderPass);
        ciFramebuffer.attachmentCount(1);
        ciFramebuffer.pAttachments(stack.longs(imageViews));
        ciFramebuffer.width(width);
        ciFramebuffer.height(height);
        ciFramebuffer.layers(1);

        var pFramebuffer = stack.callocLong(1);
        assertVkSuccess(vkCreateFramebuffer(
                instance.vkDevice(), ciFramebuffer, null, pFramebuffer
        ), "CreateFramebuffer", name);
        long framebuffer = pFramebuffer.get(0);
        instance.debug.name(stack, framebuffer, VK_OBJECT_TYPE_FRAMEBUFFER, name);
        return framebuffer;
    }

    public long simpleSampler(
            MemoryStack stack, int magMinFilter, int mipMapMode, int addressMode, String name
    ) {
        return createSampler(stack, magMinFilter, mipMapMode, addressMode, 0f, 0f, true, name);
    }

    public long createSampler(
            MemoryStack stack, int magMinFilter, int mipMapMode, int addressMode,
            float minLod, float maxLod, boolean normalized, String name
    ) {
        var ciSampler = VkSamplerCreateInfo.calloc(stack);
        ciSampler.sType$Default();
        ciSampler.magFilter(magMinFilter);
        ciSampler.minFilter(magMinFilter);
        ciSampler.mipmapMode(mipMapMode);
        ciSampler.addressModeU(addressMode);
        ciSampler.addressModeV(addressMode);
        ciSampler.addressModeW(addressMode);
        ciSampler.mipLodBias(0f);
        ciSampler.anisotropyEnable(false);
        ciSampler.compareEnable(false);
        ciSampler.compareOp(VK_COMPARE_OP_ALWAYS);
        ciSampler.minLod(minLod);
        ciSampler.maxLod(maxLod);
        ciSampler.borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK);
        ciSampler.unnormalizedCoordinates(!normalized);

        var pSampler = stack.callocLong(1);
        assertVkSuccess(vkCreateSampler(
                instance.vkDevice(), ciSampler, null, pSampler),
                "CreateSampler", name
        );
        long sampler = pSampler.get(0);
        instance.debug.name(stack, sampler, VK_OBJECT_TYPE_SAMPLER, name);
        return sampler;
    }

    public int chooseDepthStencilFormat(MemoryStack stack, int... preferredFormats) {
        var formatProps = VkFormatProperties.calloc(stack);
        for (int candidateFormat : preferredFormats) {
            vkGetPhysicalDeviceFormatProperties(instance.vkPhysicalDevice(), candidateFormat, formatProps);

            if ((formatProps.optimalTilingFeatures() & VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT) != 0) {
                return candidateFormat;
            }
        }

        throw new IllegalArgumentException("None of the preferred formats supports depth-stencil operations");
    }
}
