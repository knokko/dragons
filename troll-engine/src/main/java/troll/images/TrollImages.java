package troll.images;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import org.lwjgl.vulkan.VkImageSubresourceRange;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import troll.instance.TrollInstance;

import static org.lwjgl.vulkan.VK10.*;
import static troll.exceptions.VulkanFailureException.assertVkSuccess;

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
}
