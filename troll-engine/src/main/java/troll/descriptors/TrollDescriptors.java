package troll.descriptors;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import troll.buffer.VmaBuffer;
import troll.instance.TrollInstance;

import static org.lwjgl.vulkan.VK10.*;
import static troll.exceptions.VulkanFailureException.assertVkSuccess;

public class TrollDescriptors {

    private final TrollInstance instance;

    public TrollDescriptors(TrollInstance instance) {
        this.instance = instance;
    }

    public long createLayout(MemoryStack stack, VkDescriptorSetLayoutBinding.Buffer bindings, String name) {
        var ciLayout = VkDescriptorSetLayoutCreateInfo.calloc(stack);
        ciLayout.sType$Default();
        ciLayout.flags(0);
        ciLayout.pBindings(bindings);

        var pLayout = stack.callocLong(1);
        assertVkSuccess(vkCreateDescriptorSetLayout(
                instance.vkDevice(), ciLayout, null, pLayout
        ), "CreateDescriptorSetLayout", name);
        long layout = pLayout.get(0);

        instance.debug.name(stack, layout, VK_OBJECT_TYPE_DESCRIPTOR_SET_LAYOUT, name);
        return layout;
    }

    public long[] allocate(MemoryStack stack, int amount, long descriptorPool, String name, long... layouts) {
        var aiSets = VkDescriptorSetAllocateInfo.calloc(stack);
        aiSets.sType$Default();
        aiSets.descriptorPool(descriptorPool);
        aiSets.pSetLayouts(stack.longs(layouts));

        var pSets = stack.callocLong(amount);
        assertVkSuccess(vkAllocateDescriptorSets(
                instance.vkDevice(), aiSets, pSets
        ), "AllocateDescriptorSets", name);

        long[] results = new long[amount];
        for (int index = 0; index < amount; index++) {
            long set = pSets.get(index);
            instance.debug.name(stack, set, VK_OBJECT_TYPE_DESCRIPTOR_SET, name);
            results[index] = set;
        }
        return results;
    }

    public VkDescriptorBufferInfo.Buffer bufferInfo(MemoryStack stack, VmaBuffer... buffers) {
        var descriptorBufferInfo = VkDescriptorBufferInfo.calloc(buffers.length, stack);
        for (int index = 0; index < buffers.length; index++) {
            descriptorBufferInfo.get(index).buffer(buffers[index].vkBuffer());
            descriptorBufferInfo.get(index).offset(0);
            descriptorBufferInfo.get(index).range(buffers[index].size());
        }

        return descriptorBufferInfo;
    }
}
