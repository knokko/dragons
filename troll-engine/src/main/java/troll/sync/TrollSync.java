package troll.sync;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferMemoryBarrier;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import troll.instance.TrollInstance;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;
import static troll.exceptions.VulkanFailureException.assertVkSuccess;

public class TrollSync {

    private final TrollInstance instance;

    public TrollSync(TrollInstance instance) {
        this.instance = instance;
    }

    public long createFence(boolean startSignaled, String name) {
        try (var stack = stackPush()) {
            var ciFence = VkFenceCreateInfo.calloc(stack);
            ciFence.sType$Default();
            ciFence.flags(startSignaled ? VK_FENCE_CREATE_SIGNALED_BIT : 0);

            var pFence = stack.callocLong(1);
            assertVkSuccess(vkCreateFence(
                    instance.vkDevice(), ciFence, null, pFence
            ), "CreateFence", name);
            instance.debug.name(stack, pFence.get(0), VK_OBJECT_TYPE_FENCE, name);
            return pFence.get(0);
        }
    }


}
