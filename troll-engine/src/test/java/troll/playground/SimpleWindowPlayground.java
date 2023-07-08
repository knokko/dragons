package troll.playground;

import org.lwjgl.vulkan.VkClearColorValue;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import troll.builder.TrollBuilder;
import troll.builder.TrollSwapchainBuilder;
import troll.builder.instance.ValidationFeatures;
import troll.sync.BufferUsage;
import troll.sync.WaitSemaphore;

import java.util.Objects;

import static java.lang.Thread.sleep;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.VK_API_VERSION_1_2;
import static troll.exceptions.VulkanFailureException.assertVkSuccess;

public class SimpleWindowPlayground {

    public static void main(String[] args) throws InterruptedException {
        glfwInit();
        glfwWindowHint(GLFW_TRANSPARENT_FRAMEBUFFER, GLFW_TRUE);

        var instance = new TrollBuilder(
                VK_API_VERSION_1_2, "SimpleWindowPlayground", VK_MAKE_VERSION(1, 0, 0)
        )
                .validation(new ValidationFeatures(
                        false, false, false, true, true
                ))
                .dontInitGLFW()
                .window(0, 800, 600, new TrollSwapchainBuilder(
                        VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT
                ).compositeAlphaPicker(supportedMask -> {
                    if ((supportedMask & VK_COMPOSITE_ALPHA_PRE_MULTIPLIED_BIT_KHR) != 0) {
                        return VK_COMPOSITE_ALPHA_PRE_MULTIPLIED_BIT_KHR;
                    } else {
                        System.out.println("No fun: mask is " + supportedMask);
                        return VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
                    }
                }))
                .build();

        var semaphore1 = instance.sync.createSemaphore("AcquireToFill");
        var semaphore2 = instance.sync.createSemaphore("FillToPresent");
        var commandPool = instance.commands.createPool(0, instance.queueFamilies().graphics().index(), "Fill");
        var commandBuffer = instance.commands.createPrimaryBuffer(commandPool, 1, "Fill")[0];

        var swapchain = instance.swapchains.create(VK_NULL_HANDLE, VK_PRESENT_MODE_FIFO_KHR);

        try (var stack = stackPush()) {
            var pImageIndex = stack.callocInt(1);
            assertVkSuccess(vkAcquireNextImageKHR(
                    instance.vkDevice(), swapchain.vkSwapchain(), 100_000_000, semaphore1, VK_NULL_HANDLE, pImageIndex
            ), "AcquireNextImageKHR", "Fill");
            int swapchainImageIndex = pImageIndex.get(0);
            long swapchainImage = swapchain.vkImages()[swapchainImageIndex];

            instance.commands.begin(commandBuffer, stack, "Fill");

            instance.commands.transitionLayout(
                    stack, commandBuffer, swapchainImage,
                    VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    new BufferUsage(0, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT),
                    new BufferUsage(VK_ACCESS_TRANSFER_WRITE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT)
            );

            float alpha = 0.821f;
            var pClearColor = VkClearColorValue.calloc(stack);
            pClearColor.float32(stack.floats(0f, alpha * 0.6f, alpha, alpha));

            var pRanges = instance.images.subresourceRange(stack, null, VK_IMAGE_ASPECT_COLOR_BIT);

            vkCmdClearColorImage(commandBuffer, swapchainImage, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, pClearColor, pRanges);

            instance.commands.transitionLayout(
                    stack, commandBuffer, swapchainImage,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_PRESENT_SRC_KHR,
                    new BufferUsage(VK_ACCESS_TRANSFER_WRITE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT),
                    new BufferUsage(0, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
            );

            assertVkSuccess(vkEndCommandBuffer(commandBuffer), "EndCommandBuffer", "Fill");
            instance.queueFamilies().graphics().queues().get(0).submit(
                    commandBuffer, "Fill", new WaitSemaphore[] {
                            new WaitSemaphore(semaphore1, VK_PIPELINE_STAGE_TRANSFER_BIT)
                    }, VK_NULL_HANDLE, semaphore2
            );

            var presentInfo = VkPresentInfoKHR.calloc(stack);
            presentInfo.sType$Default();
            presentInfo.pWaitSemaphores(stack.longs(semaphore2));
            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(stack.longs(swapchain.vkSwapchain()));
            presentInfo.pImageIndices(stack.ints(swapchainImageIndex));
            presentInfo.pResults(stack.callocInt(1));

            assertVkSuccess(vkQueuePresentKHR(
                    instance.queueFamilies().present().queues().get(0).vkQueue(), presentInfo
            ), "QueuePresentKHR", "main");
            assertVkSuccess(Objects.requireNonNull(presentInfo.pResults()).get(0), "QueuePresentKHR", "image0");
        }

        sleep(1000);
        assertVkSuccess(vkDeviceWaitIdle(instance.vkDevice()), "DeviceWaitIdle", null);

        vkDestroyCommandPool(instance.vkDevice(), commandPool, null);
        vkDestroySemaphore(instance.vkDevice(), semaphore1, null);
        vkDestroySemaphore(instance.vkDevice(), semaphore2, null);
        vkDestroySwapchainKHR(instance.vkDevice(), swapchain.vkSwapchain(), null);

        instance.destroy();
    }
}
