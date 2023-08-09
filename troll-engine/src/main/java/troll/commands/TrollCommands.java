package troll.commands;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import troll.instance.TrollInstance;
import troll.sync.ResourceUsage;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;
import static troll.exceptions.VulkanFailureException.assertVkSuccess;

public class TrollCommands {

    private final TrollInstance instance;

    public TrollCommands(TrollInstance instance) {
        this.instance = instance;
    }

    public long createPool(int flags, int queueFamilyIndex, String name) {
        try (var stack = stackPush()) {
            var ciCommandPool = VkCommandPoolCreateInfo.calloc(stack);
            ciCommandPool.sType$Default();
            ciCommandPool.flags(flags);
            ciCommandPool.queueFamilyIndex(queueFamilyIndex);

            var pCommandPool = stack.callocLong(1);
            assertVkSuccess(vkCreateCommandPool(
                    instance.vkDevice(), ciCommandPool, null, pCommandPool
            ), "CreateCommandPool", name);
            instance.debug.name(stack, pCommandPool.get(0), VK_OBJECT_TYPE_COMMAND_POOL, name);
            return pCommandPool.get(0);
        }
    }

    public VkCommandBuffer[] createPrimaryBuffers(long commandPool, int amount, String name) {
        try (var stack = stackPush()) {
            var aiCommandBuffer = VkCommandBufferAllocateInfo.calloc(stack);
            aiCommandBuffer.sType$Default();
            aiCommandBuffer.commandPool(commandPool);
            aiCommandBuffer.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            aiCommandBuffer.commandBufferCount(amount);

            var pCommandBuffer = stack.callocPointer(amount);
            assertVkSuccess(vkAllocateCommandBuffers(
                    instance.vkDevice(), aiCommandBuffer, pCommandBuffer
            ), "AllocateCommandBuffers", name);
            var result = new VkCommandBuffer[amount];
            for (int index = 0; index < amount; index++) {
                result[index] = new VkCommandBuffer(pCommandBuffer.get(index), instance.vkDevice());
                instance.debug.name(stack, result[index].address(), VK_OBJECT_TYPE_COMMAND_BUFFER, name);
            }
            return result;
        }
    }

    public void begin(VkCommandBuffer commandBuffer, MemoryStack stack, String context) {
        var biCommands = VkCommandBufferBeginInfo.calloc(stack);
        biCommands.sType$Default();

        assertVkSuccess(vkBeginCommandBuffer(
                commandBuffer, biCommands
        ), "BeginCommandBuffer", context);
    }

    public void copyBuffer(
            VkCommandBuffer commandBuffer, MemoryStack stack, long size,
            long vkSourceBuffer, long sourceOffset,
            long vkDestBuffer, long destOffset
    ) {
        var copyRegion = VkBufferCopy.calloc(1, stack);
        copyRegion.srcOffset(sourceOffset);
        copyRegion.dstOffset(destOffset);
        copyRegion.size(size);

        vkCmdCopyBuffer(commandBuffer, vkSourceBuffer, vkDestBuffer, copyRegion);
    }

    public void copyImage(
            VkCommandBuffer commandBuffer, MemoryStack stack, int width, int height, int aspectMask,
            long vkSourceImage, long vkDestImage
    ) {
        var imageCopyRegions = VkImageCopy.calloc(1, stack);
        var copyRegion = imageCopyRegions.get(0);
        instance.images.subresourceLayers(stack, copyRegion.srcSubresource(), aspectMask);
        copyRegion.srcOffset().set(0, 0, 0);
        instance.images.subresourceLayers(stack, copyRegion.dstSubresource(), aspectMask);
        copyRegion.dstOffset().set(0, 0, 0);
        copyRegion.extent().set(width, height, 1);

        vkCmdCopyImage(
                commandBuffer, vkSourceImage, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                vkDestImage, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, imageCopyRegions
        );
    }

    public void blitImage(
            VkCommandBuffer commandBuffer, MemoryStack stack, int aspectMask, int filter,
            long vkSourceImage, int sourceWidth, int sourceHeight,
            long vkDestImage, int destWidth, int destHeight
    ) {
        var imageBlitRegions = VkImageBlit.calloc(1, stack);
        var blitRegion = imageBlitRegions.get(0);
        instance.images.subresourceLayers(stack, blitRegion.srcSubresource(), aspectMask);
        blitRegion.srcOffsets().get(0).set(0, 0, 0);
        blitRegion.srcOffsets().get(1).set(sourceWidth, sourceHeight, 1);
        instance.images.subresourceLayers(stack, blitRegion.dstSubresource(), aspectMask);
        blitRegion.dstOffsets().get(0).set(0, 0, 0);
        blitRegion.dstOffsets().get(1).set(destWidth, destHeight, 1);

        vkCmdBlitImage(
                commandBuffer, vkSourceImage, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                vkDestImage, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, imageBlitRegions, filter
        );
    }

    public void copyImageToBuffer(
            VkCommandBuffer commandBuffer, MemoryStack stack, int aspectMask,
            long vkImage, int width, int height, long vkBuffer
    ) {
        var bufferCopyRegions = VkBufferImageCopy.calloc(1, stack);
        var copyRegion = bufferCopyRegions.get(0);
        copyRegion.bufferOffset(0);
        copyRegion.bufferRowLength(width);
        copyRegion.bufferImageHeight(height);
        instance.images.subresourceLayers(stack, copyRegion.imageSubresource(), aspectMask);
        copyRegion.imageOffset().set(0, 0, 0);
        copyRegion.imageExtent().set(width, height, 1);

        vkCmdCopyImageToBuffer(
                commandBuffer, vkImage, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, vkBuffer, bufferCopyRegions
        );
    }

    public void copyBufferToImage(
            VkCommandBuffer commandBuffer, MemoryStack stack, int aspectMask,
            long vkImage, int width, int height, long vkBuffer
    ) {
        var bufferCopyRegions = VkBufferImageCopy.calloc(1, stack);
        var copyRegion = bufferCopyRegions.get(0);
        copyRegion.bufferOffset(0);
        copyRegion.bufferRowLength(width);
        copyRegion.bufferImageHeight(height);
        instance.images.subresourceLayers(stack, copyRegion.imageSubresource(), aspectMask);
        copyRegion.imageOffset().set(0, 0, 0);
        copyRegion.imageExtent().set(width, height, 1);

        vkCmdCopyBufferToImage(
                commandBuffer, vkBuffer, vkImage, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, bufferCopyRegions
        );
    }

    public void bufferBarrier(
            MemoryStack stack, VkCommandBuffer commandBuffer,
            long vkBuffer, long offset, long size,
            ResourceUsage srcUsage, ResourceUsage dstUsage
    ) {
        var bufferBarrier = VkBufferMemoryBarrier.calloc(1, stack);
        bufferBarrier.sType$Default();
        bufferBarrier.srcAccessMask(srcUsage.accessMask());
        bufferBarrier.dstAccessMask(dstUsage.accessMask());
        bufferBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        bufferBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        bufferBarrier.buffer(vkBuffer);
        bufferBarrier.offset(offset);
        bufferBarrier.size(size);

        vkCmdPipelineBarrier(
                commandBuffer, srcUsage.stageMask(), dstUsage.stageMask(),
                0, null, bufferBarrier, null
        );
    }

    public void transitionColorLayout(
            MemoryStack stack, VkCommandBuffer commandBuffer, long vkImage, int oldLayout, int newLayout,
            ResourceUsage oldUsage, ResourceUsage newUsage
    ) {
        transitionLayout(stack, commandBuffer, vkImage, oldLayout, newLayout, oldUsage, newUsage, VK_IMAGE_ASPECT_COLOR_BIT);
    }

    public void transitionDepthLayout(
            MemoryStack stack, VkCommandBuffer commandBuffer, long vkImage, int oldLayout, int newLayout,
            ResourceUsage oldUsage, ResourceUsage newUsage
    ) {
        transitionLayout(stack, commandBuffer, vkImage, oldLayout, newLayout, oldUsage, newUsage, VK_IMAGE_ASPECT_DEPTH_BIT);
    }

    public void transitionLayout(
            MemoryStack stack, VkCommandBuffer commandBuffer, long vkImage, int oldLayout, int newLayout,
            ResourceUsage oldUsage, ResourceUsage newUsage, int aspectMask
    ) {
        var pImageBarrier = VkImageMemoryBarrier.calloc(1, stack);
        pImageBarrier.sType$Default();
        pImageBarrier.srcAccessMask(oldUsage != null ? oldUsage.accessMask() : 0);
        pImageBarrier.dstAccessMask(newUsage != null ? newUsage.accessMask() : 0);
        pImageBarrier.oldLayout(oldLayout);
        pImageBarrier.newLayout(newLayout);
        pImageBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        pImageBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        pImageBarrier.image(vkImage);
        instance.images.subresourceRange(stack, pImageBarrier.subresourceRange(), aspectMask);

        vkCmdPipelineBarrier(
                commandBuffer, oldUsage != null ? oldUsage.stageMask() : VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                newUsage != null ? newUsage.stageMask() : VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
                0, null, null, pImageBarrier
        );
    }

    public void dynamicViewportAndScissor(MemoryStack stack, VkCommandBuffer commandBuffer, int width, int height) {
        var pViewport = VkViewport.calloc(1, stack);
        pViewport.x(0f);
        pViewport.y(0f);
        pViewport.width((float) width);
        pViewport.height((float) height);
        pViewport.minDepth(0f);
        pViewport.maxDepth(1f);

        var pScissor = VkRect2D.calloc(1, stack);
        pScissor.offset().set(0, 0);
        pScissor.extent().set(width, height);

        vkCmdSetViewport(commandBuffer, 0, pViewport);
        vkCmdSetScissor(commandBuffer, 0, pScissor);
    }
}
