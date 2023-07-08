package troll.commands;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import troll.instance.TrollInstance;
import troll.sync.BufferUsage;

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

    public VkCommandBuffer[] createPrimaryBuffer(long commandPool, int amount, String name) {
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

    public void bufferBarrier(
            MemoryStack stack, VkCommandBuffer commandBuffer,
            long vkBuffer, long offset, long size,
            BufferUsage srcUsage, BufferUsage dstUsage
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
}
