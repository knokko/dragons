package troll.sync;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
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

    public long createSemaphore(String name) {
        try (var stack = stackPush()) {
            var ciSemaphore = VkSemaphoreCreateInfo.calloc(stack);
            ciSemaphore.sType$Default();
            ciSemaphore.flags(0);

            var pSemaphore = stack.callocLong(1);
            assertVkSuccess(vkCreateSemaphore(
                    instance.vkDevice(), ciSemaphore, null, pSemaphore
            ), "CreateSemaphore", name);
            long semaphore = pSemaphore.get(0);
            instance.debug.name(stack, semaphore, VK_OBJECT_TYPE_SEMAPHORE, name);
            return semaphore;
        }
    }

    public void waitAndReset(MemoryStack stack, long fence, long timeout) {
        assertVkSuccess(vkWaitForFences(
                instance.vkDevice(), stack.longs(fence), true, timeout
        ), "WaitForFences", "SwapchainAcquire");
        assertVkSuccess(vkResetFences(
                instance.vkDevice(), stack.longs(fence)
        ), "ResetFences", "SwapchainAcquire");
    }
}
