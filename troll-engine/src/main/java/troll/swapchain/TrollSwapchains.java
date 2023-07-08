package troll.swapchain;

import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;
import troll.instance.TrollInstance;

import static java.lang.Math.max;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkCreateSwapchainKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkGetSwapchainImagesKHR;
import static org.lwjgl.vulkan.VK10.*;
import static troll.exceptions.VulkanFailureException.assertVkSuccess;

public class TrollSwapchains {

    private final TrollInstance instance;

    public TrollSwapchains(TrollInstance instance) {
        this.instance = instance;
    }

    public TrollSwapchain create(long oldSwapchain, int presentMode) {
        try (var stack = stackPush()) {
            var pWidth = stack.callocInt(1);
            var pHeight = stack.callocInt(1);
            glfwGetFramebufferSize(instance.glfwWindow(), pWidth, pHeight);
            int width = pWidth.get(0);
            int height = pHeight.get(0);

            int desiredImageCount = presentMode == VK_PRESENT_MODE_MAILBOX_KHR ? 3 : 2;
            var ciSwapchain = VkSwapchainCreateInfoKHR.calloc(stack);
            ciSwapchain.sType$Default();
            ciSwapchain.flags(0);
            ciSwapchain.surface(instance.windowSurface().vkSurface());
            ciSwapchain.minImageCount(max(desiredImageCount, instance.windowSurface().initialCapabilities().minImageCount()));
            ciSwapchain.imageFormat(instance.swapchainSettings.surfaceFormat().format());
            ciSwapchain.imageColorSpace(instance.swapchainSettings.surfaceFormat().colorSpace());
            ciSwapchain.imageExtent().set(width, height);
            ciSwapchain.imageArrayLayers(1);
            ciSwapchain.imageUsage(instance.swapchainSettings.imageUsage());

            if (instance.queueFamilies().graphics() == instance.queueFamilies().present()) {
                ciSwapchain.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
            } else {
                ciSwapchain.imageSharingMode(VK_SHARING_MODE_CONCURRENT);
                ciSwapchain.queueFamilyIndexCount(2);
                ciSwapchain.pQueueFamilyIndices(stack.ints(
                        instance.queueFamilies().graphics().index(), instance.queueFamilies().present().index()
                ));
            }

            ciSwapchain.preTransform(instance.windowSurface().initialCapabilities().currentTransform());
            ciSwapchain.compositeAlpha(instance.swapchainSettings.compositeAlpha());
            ciSwapchain.presentMode(presentMode);
            ciSwapchain.clipped(true);
            ciSwapchain.oldSwapchain(oldSwapchain);

            var pSwapchain = stack.callocLong(1);
            assertVkSuccess(vkCreateSwapchainKHR(
                    instance.vkDevice(), ciSwapchain, null, pSwapchain
            ), "CreateSwapchainKHR", null);
            long swapchain = pSwapchain.get(0);

            var pNumImages = stack.callocInt(1);
            assertVkSuccess(vkGetSwapchainImagesKHR(
                    instance.vkDevice(), swapchain, pNumImages, null
            ), "GetSwapchainImagesKHR", "count");
            int numImages = pNumImages.get(0);

            var pImages = stack.callocLong(numImages);
            assertVkSuccess(vkGetSwapchainImagesKHR(
                    instance.vkDevice(), swapchain, pNumImages, pImages
            ), "GetSwapchainImagesKHR", "images");
            long[] images = new long[numImages];
            for (int index = 0; index < numImages; index++) {
                long image = pImages.get(0);
                images[index] = image;
                instance.debug.name(stack, image, VK_OBJECT_TYPE_IMAGE, "SwapchainImage" + index);
            }

            return new TrollSwapchain(swapchain, images, width, height, presentMode);
        }
    }
}
