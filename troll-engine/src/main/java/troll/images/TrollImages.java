package troll.images;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkImageSubresourceRange;
import troll.instance.TrollInstance;

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
}
