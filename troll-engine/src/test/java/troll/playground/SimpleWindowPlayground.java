package troll.playground;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VkClearColorValue;
import troll.builder.TrollBuilder;
import troll.builder.TrollSwapchainBuilder;
import troll.builder.instance.ValidationFeatures;
import troll.builder.swapchain.SimpleCompositeAlphaPicker;
import troll.sync.ResourceUsage;
import troll.sync.WaitSemaphore;

import static java.lang.Math.*;
import static java.lang.Thread.sleep;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;
import static troll.exceptions.VulkanFailureException.assertVkSuccess;
import static troll.util.ReflectionHelper.getIntConstantName;

public class SimpleWindowPlayground {

    public static void main(String[] args) throws InterruptedException {
        // Wayland's windows have cool transparency support
        if (glfwPlatformSupported(GLFW_PLATFORM_WAYLAND)) glfwInitHint(GLFW_PLATFORM, GLFW_PLATFORM_WAYLAND);
        glfwInit();
        glfwWindowHint(GLFW_TRANSPARENT_FRAMEBUFFER, GLFW_TRUE);

        var instance = new TrollBuilder(
                VK_API_VERSION_1_0, "SimpleWindowPlayground", VK_MAKE_VERSION(1, 0, 0)
        )
                .validation(new ValidationFeatures(
                        false, false, false, true, true
                ))
                .dontInitGLFW()
                .window(0, 800, 600, new TrollSwapchainBuilder(
                        VK_IMAGE_USAGE_TRANSFER_DST_BIT
                ).compositeAlphaPicker(new SimpleCompositeAlphaPicker(
                        VK_COMPOSITE_ALPHA_PRE_MULTIPLIED_BIT_KHR,
                        VK_COMPOSITE_ALPHA_POST_MULTIPLIED_BIT_KHR,
                        VK_COMPOSITE_ALPHA_INHERIT_BIT_KHR,
                        VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR
                )))
                .build();

        System.out.printf(
                "GLFW platform is %s and GLFW transparent framebuffer is %b and composite alpha mode is %s\n",
                getIntConstantName(GLFW.class, glfwGetPlatform(), "GLFW_PLATFORM", "", "unknown"),
                glfwGetWindowAttrib(instance.glfwWindow(), GLFW_TRANSPARENT_FRAMEBUFFER),
                getIntConstantName(KHRSurface.class, instance.swapchainSettings.compositeAlpha(), "VK_COMPOSITE_ALPHA", "BIT_KHR", "unknown")
        );

        var commandPool = instance.commands.createPool(
                VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT,
                instance.queueFamilies().graphics().index(), "Fill"
        );
        var commandBuffers = instance.commands.createPrimaryBuffer(commandPool, 5, "Fill");

        long[] commandFences = new long[commandBuffers.length];
        for (int index = 0; index < commandFences.length; index++) {
            commandFences[index] = instance.sync.createFence(true, "Acquire" + index);
        }

        long oldSwapchainID = -1;

        int counter = 0;
        while(!glfwWindowShouldClose(instance.glfwWindow())) {
            glfwPollEvents();
            try (var stack = stackPush()) {
                int commandIndex = counter % commandFences.length;

                var acquired = instance.swapchains.acquireNextImage(VK_PRESENT_MODE_FIFO_KHR);
                if (acquired == null) {
                    //noinspection BusyWait
                    sleep(100);
                    continue;
                }

                if (acquired.swapchainID() != oldSwapchainID) {
                    oldSwapchainID = acquired.swapchainID();
                }

                long fence = commandFences[commandIndex];
                assertVkSuccess(vkWaitForFences(
                        instance.vkDevice(), stack.longs(fence), true, 100_000_000
                ), "WaitForFences", "Acquire" + counter);
                assertVkSuccess(vkResetFences(instance.vkDevice(), stack.longs(fence)), "ResetFences", "Acquire" + counter);

                var commandBuffer = commandBuffers[commandIndex];
                instance.commands.begin(commandBuffer, stack, "Fill");

                instance.commands.transitionLayout(
                        stack, commandBuffer, acquired.vkImage(),
                        VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                        new ResourceUsage(0, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT),
                        new ResourceUsage(VK_ACCESS_TRANSFER_WRITE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT)
                );

                float alpha = 0.1f + 0.9f * (float) (abs(sin(System.currentTimeMillis() / 1000.0)));
                float colorScale = instance.swapchainSettings.compositeAlpha() == VK_COMPOSITE_ALPHA_POST_MULTIPLIED_BIT_KHR ? 1f : alpha;
                var pClearColor = VkClearColorValue.calloc(stack);
                pClearColor.float32(stack.floats(0f, 0.6f * colorScale, colorScale, alpha));

                var pRanges = instance.images.subresourceRange(stack, null, VK_IMAGE_ASPECT_COLOR_BIT);

                vkCmdClearColorImage(commandBuffer, acquired.vkImage(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, pClearColor, pRanges);

                instance.commands.transitionLayout(
                        stack, commandBuffer, acquired.vkImage(),
                        VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_PRESENT_SRC_KHR,
                        new ResourceUsage(VK_ACCESS_TRANSFER_WRITE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT),
                        new ResourceUsage(0, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
                );

                assertVkSuccess(vkEndCommandBuffer(commandBuffer), "EndCommandBuffer", "Fill");
                instance.queueFamilies().graphics().queues().get(0).submit(
                        commandBuffer, "Fill", new WaitSemaphore[] { new WaitSemaphore(
                                acquired.acquireSemaphore(), VK_PIPELINE_STAGE_TRANSFER_BIT
                        ) }, fence, acquired.presentSemaphore()
                );

                instance.swapchains.presentImage(acquired);
            }

            counter += 1;
        }

        assertVkSuccess(vkDeviceWaitIdle(instance.vkDevice()), "DeviceWaitIdle", null);

        vkDestroyCommandPool(instance.vkDevice(), commandPool, null);
        for (long fence : commandFences) vkDestroyFence(instance.vkDevice(), fence, null);

        instance.destroy();
    }
}
