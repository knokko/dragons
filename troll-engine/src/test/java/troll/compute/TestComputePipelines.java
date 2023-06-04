package troll.compute;

import org.junit.jupiter.api.Test;
import org.lwjgl.vulkan.*;
import troll.builder.TrollBuilder;
import troll.builder.instance.ValidationFeatures;
import troll.sync.WaitSemaphore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.vma.Vma.vmaDestroyBuffer;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.VK_API_VERSION_1_2;
import static troll.exceptions.VulkanFailureException.assertVkSuccess;

public class TestComputePipelines {

    @Test
    public void testSimpleComputeShader() {
        var instance = new TrollBuilder(
                VK_API_VERSION_1_2, "TestSimpleComputeShader", VK_MAKE_VERSION(0, 1, 0)
        )
                .validation(new ValidationFeatures(true, true, false, true, true))
                .build();

        try (var stack = stackPush()) {
            int valuesPerInvocation = 16;
            int invocationsPerGroup = 128;
            int groupCount = 1024 * 2;

            var buffer = instance.buffers.createMapped(
                    4 * valuesPerInvocation * invocationsPerGroup * groupCount,
                    VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "Filled"
            );
            var hostBuffer = memIntBuffer(buffer.hostAddress(), valuesPerInvocation * invocationsPerGroup * groupCount);

            var fillLayoutBindings = VkDescriptorSetLayoutBinding.calloc(1, stack);
            var fillBufferLayoutBinding = fillLayoutBindings.get(0);
            fillBufferLayoutBinding.binding(0);
            fillBufferLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER);
            fillBufferLayoutBinding.descriptorCount(1);
            fillBufferLayoutBinding.stageFlags(VK_SHADER_STAGE_COMPUTE_BIT);

            long descriptorSetLayout = instance.descriptors.createLayout(
                    stack, fillLayoutBindings, "FillBuffer-DescriptorSetLayout"
            );

            var pushConstants = VkPushConstantRange.calloc(1, stack);
            var sizePushConstant = pushConstants.get(0);
            sizePushConstant.stageFlags(VK_SHADER_STAGE_COMPUTE_BIT);
            sizePushConstant.offset(0);
            sizePushConstant.size(8);

            long pipelineLayout = instance.pipelines.createLayout(
                    stack, descriptorSetLayout, pushConstants, "FillBuffer-PipelineLayout"
            );
            long computePipeline = instance.pipelines.createComputePipeline(
                    stack, pipelineLayout, "troll/compute/fill.comp.spv", "FillBuffer"
            );

            var descriptorPoolSizes = VkDescriptorPoolSize.calloc(1, stack);
            descriptorPoolSizes.type(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER);
            descriptorPoolSizes.descriptorCount(1);

            var ciDescriptorPool = VkDescriptorPoolCreateInfo.calloc(stack);
            ciDescriptorPool.sType$Default();
            ciDescriptorPool.flags(0);
            ciDescriptorPool.maxSets(1);
            ciDescriptorPool.pPoolSizes(descriptorPoolSizes);

            var pDescriptorPool = stack.callocLong(1);
            assertVkSuccess(vkCreateDescriptorPool(
                    instance.vkDevice(), ciDescriptorPool, null, pDescriptorPool
            ), "CreateDescriptorPool", "Filling");
            long descriptorPool = pDescriptorPool.get(0);
            long descriptorSet = instance.descriptors.allocate(stack, 1, descriptorPool, "Filling", descriptorSetLayout)[0];

            var descriptorWrites = VkWriteDescriptorSet.calloc(1, stack);
            descriptorWrites.sType$Default();
            descriptorWrites.dstSet(descriptorSet);
            descriptorWrites.dstBinding(0);
            descriptorWrites.dstArrayElement(0);
            descriptorWrites.descriptorCount(1);
            descriptorWrites.descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER);
            descriptorWrites.pBufferInfo(instance.descriptors.bufferInfo(stack, buffer.buffer()));

            vkUpdateDescriptorSets(instance.vkDevice(), descriptorWrites, null);

            long fence = instance.sync.createFence(false, "Filling");

            long commandPool = instance.commands.createPool(
                    0, instance.queueFamilies().graphics().index(), "Filling"
            );
            var commandBuffer = instance.commands.createPrimaryBuffer(
                    commandPool, 1, "Filling"
            )[0];
            instance.commands.begin(commandBuffer, stack, "Filling");

            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, computePipeline);
            vkCmdBindDescriptorSets(
                    commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, pipelineLayout,
                    0, stack.longs(descriptorSet), null
            );
            vkCmdPushConstants(
                    commandBuffer, pipelineLayout, VK_SHADER_STAGE_COMPUTE_BIT, 0,
                    stack.ints(valuesPerInvocation)
            );
            vkCmdDispatch(commandBuffer, groupCount, 1, 1);

            assertVkSuccess(vkEndCommandBuffer(commandBuffer), "EndCommandBuffer", "Filling");
            long startTime = System.currentTimeMillis();
            instance.queueFamilies().graphics().queues().get(0).submit(
                    commandBuffer, "Filling", new WaitSemaphore[0], fence
            );

            assertVkSuccess(vkWaitForFences(
                    instance.vkDevice(), stack.longs(fence), true, 1_000_000_000L
            ), "WaitForFences", "Filling");
            System.out.println("Submission took " + (System.currentTimeMillis() - startTime) + " ms");

            for (int index = 0; index < hostBuffer.limit(); index++) {
                assertEquals(123456, hostBuffer.get(index));
            }

            vkDestroyFence(instance.vkDevice(), fence, null);
            vkDestroyDescriptorPool(instance.vkDevice(), descriptorPool, null);
            vkDestroyCommandPool(instance.vkDevice(), commandPool, null);
            vkDestroyPipeline(instance.vkDevice(), computePipeline, null);
            vkDestroyDescriptorSetLayout(instance.vkDevice(), descriptorSetLayout, null);
            vkDestroyPipelineLayout(instance.vkDevice(), pipelineLayout, null);
            vmaDestroyBuffer(instance.vmaAllocator(), buffer.buffer().vkBuffer(), buffer.buffer().vmaAllocation());
        }

        instance.destroy();
    }
}