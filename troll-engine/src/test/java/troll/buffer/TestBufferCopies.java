package troll.buffer;

import org.junit.jupiter.api.Test;
import troll.builder.TrollBuilder;
import troll.builder.instance.ValidationFeatures;
import troll.sync.BufferUsage;
import troll.sync.WaitSemaphore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memByteBuffer;
import static org.lwjgl.vulkan.VK10.*;
import static troll.exceptions.VulkanFailureException.assertVkSuccess;

public class TestBufferCopies {

    @Test
    public void testBufferCopies() {
        var instance = new TrollBuilder(
                VK_API_VERSION_1_0, "Test buffer copies", VK_MAKE_VERSION(1, 0, 0)
        ).validation(new ValidationFeatures(false, false, true, true, true)
        ).build();

        var sourceBuffer = instance.buffers.createMapped(
                100, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, "source"
        );
        var sourceHostBuffer = memByteBuffer(sourceBuffer.hostAddress(), 100);
        var middleBuffer = instance.buffers.create(
                100, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_TRANSFER_SRC_BIT, "middle"
        );
        var destinationBuffer = instance.buffers.createMapped(
                100, VK_BUFFER_USAGE_TRANSFER_DST_BIT, "destination"
        );
        var destinationHostBuffer = memByteBuffer(destinationBuffer.hostAddress(), 100);

        for (int index = 0; index < 100; index++) {
            sourceHostBuffer.put((byte) index);
        }

        try (var stack = stackPush()) {
            var fence = instance.sync.createFence(false, "Copying");
            var commandPool = instance.commands.createPool(
                    0, instance.queueFamilies().graphics().index(), "Copy"
            );
            var commandBuffer = instance.commands.createPrimaryBuffer(
                    commandPool, 1, "Copy"
            )[0];

            instance.commands.begin(commandBuffer, stack, "Copying");

            instance.commands.copyBuffer(
                    commandBuffer, stack, 100, sourceBuffer.buffer().vkBuffer(), 0,
                    middleBuffer.vkBuffer(), 0
            );
            instance.commands.bufferBarrier(
                    stack, commandBuffer, middleBuffer.vkBuffer(), 0, 100,
                    new BufferUsage(VK_ACCESS_TRANSFER_WRITE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT),
                    new BufferUsage(VK_ACCESS_TRANSFER_READ_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT)
            );
            instance.commands.copyBuffer(
                    commandBuffer, stack, 100, middleBuffer.vkBuffer(), 0,
                    destinationBuffer.buffer().vkBuffer(), 0
            );

            assertVkSuccess(vkEndCommandBuffer(commandBuffer), "EndCommandBuffer", "Copying");

            instance.queueFamilies().graphics().queues().get(0).submit(
                    commandBuffer, "Copying", new WaitSemaphore[0], fence
            );
            assertVkSuccess(vkWaitForFences(
                    instance.vkDevice(), stack.longs(fence), true, 100_000_000
            ), "WaitForFences", "Copying");

            vkDestroyFence(instance.vkDevice(), fence, null);
            vkDestroyCommandPool(instance.vkDevice(), commandPool, null);
        }

        for (int index = 0; index < 100; index++) {
            assertEquals((byte) index, destinationHostBuffer.get());
        }

        sourceBuffer.buffer().destroy(instance.vmaAllocator());
        middleBuffer.destroy(instance.vmaAllocator());
        destinationBuffer.buffer().destroy(instance.vmaAllocator());
        instance.destroy();
    }
}
