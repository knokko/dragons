package troll.swapchain;

import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;
import troll.instance.TrollInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;

import static java.lang.Math.max;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;
import static troll.exceptions.VulkanFailureException.assertVkSuccess;

public class TrollSwapchains {

    private final TrollInstance instance;

    private final Collection<Swapchain> oldSwapchains = new ArrayList<>();
    private Swapchain currentSwapchain;
    private long currentSwapchainID;
    private boolean isOutOfDate;

    public TrollSwapchains(TrollInstance instance) {
        this.instance = instance;
    }

    private void recreateSwapchain(int presentMode) {
        var newSwapchain = create(currentSwapchain.vkSwapchain, presentMode);
        if (newSwapchain != null) {
            oldSwapchains.add(currentSwapchain);
            currentSwapchain = newSwapchain;
            isOutOfDate = false;
        } else isOutOfDate = true;
    }

    public void presentImage(AcquireResult acquired) {
        presentImage(acquired, null);
    }

    public void presentImage(AcquireResult acquired, Consumer<VkPresentInfoKHR> beforePresentCallback) {
        if (isOutOfDate) return;
        try (var stack = stackPush()) {
            var presentInfo = VkPresentInfoKHR.calloc(stack);
            presentInfo.sType$Default();
            presentInfo.pWaitSemaphores(stack.longs(acquired.presentSemaphore()));
            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(stack.longs(acquired.vkSwapchain()));
            presentInfo.pImageIndices(stack.ints(acquired.imageIndex()));
            presentInfo.pResults(stack.callocInt(1));
            if (beforePresentCallback != null) beforePresentCallback.accept(presentInfo);

            int presentResult = vkQueuePresentKHR(
                    instance.queueFamilies().present().queues().get(0).vkQueue(), presentInfo
            );
            if (presentResult == VK_ERROR_OUT_OF_DATE_KHR || presentResult == VK_SUBOPTIMAL_KHR) return;
            assertVkSuccess(presentResult, "QueuePresentKHR", null);
            assertVkSuccess(Objects.requireNonNull(presentInfo.pResults()).get(0), "QueuePresentKHR", null);
        }
    }

    public AcquireResult acquireNextImage(int presentMode) {
        if (currentSwapchain != null && instance.windowSurface().capabilities().currentExtent().width() == -1) {
            try (var stack = stackPush()) {
                var pWidth = stack.callocInt(1);
                var pHeight = stack.callocInt(1);
                glfwGetFramebufferSize(instance.glfwWindow(), pWidth, pHeight);
                int width = pWidth.get(0);
                int height = pHeight.get(0);
                if (width != currentSwapchain.width || height != currentSwapchain.height) isOutOfDate = true;
            }
        }

        if (isOutOfDate) {
            recreateSwapchain(presentMode);
            if (isOutOfDate) return null;
        }

        if (currentSwapchain == null) {
            currentSwapchain = create(0L, presentMode);
            if (currentSwapchain == null) return null;
        }

        if (currentSwapchain.presentMode != presentMode) {
            recreateSwapchain(presentMode);
        }

        var swapchainImage = currentSwapchain.acquire();
        if (swapchainImage == null) {
            recreateSwapchain(presentMode);
            return acquireNextImage(presentMode);
        }

        // Prevent the number of swapchains from escalating when the window is being resized quickly
        if (oldSwapchains.size() > 20) {
            assertVkSuccess(vkDeviceWaitIdle(instance.vkDevice()), "DeviceWaitIdle", "SwapchainGarbage");
            destroyOldSwapchains();
        }

        if (currentSwapchain.canDestroyOldSwapchains) destroyOldSwapchains();

        return new AcquireResult(
                currentSwapchain.vkSwapchain,
                swapchainImage.vkImage,
                swapchainImage.index,
                currentSwapchain.images.length,
                swapchainImage.acquireSemaphore,
                swapchainImage.presentSemaphore,
                currentSwapchain.width,
                currentSwapchain.height,
                currentSwapchainID,
                currentSwapchain.destructionCallbacks::add
        );
    }

    private Swapchain create(long oldSwapchain, int presentMode) {
        try (var stack = stackPush()) {
            var caps = instance.windowSurface().capabilities();
            assertVkSuccess(vkGetPhysicalDeviceSurfaceCapabilitiesKHR(
                    instance.vkPhysicalDevice(), instance.windowSurface().vkSurface(), caps
            ), "GetPhysicalDeviceSurfaceCapabilitiesKHR", "SwapchainCreation");
            int width = caps.currentExtent().width();
            int height = caps.currentExtent().height();

            if (width == -1 || height == -1) {
                var pWidth = stack.callocInt(1);
                var pHeight = stack.callocInt(1);
                glfwGetFramebufferSize(instance.glfwWindow(), pWidth, pHeight);
                width = pWidth.get(0);
                height = pHeight.get(0);
            }

            if (width == 0 || height == 0) return null;

            int desiredImageCount = presentMode == VK_PRESENT_MODE_MAILBOX_KHR ? 3 : 2;
            var ciSwapchain = VkSwapchainCreateInfoKHR.calloc(stack);
            ciSwapchain.sType$Default();
            ciSwapchain.flags(0);
            ciSwapchain.surface(instance.windowSurface().vkSurface());
            ciSwapchain.minImageCount(max(desiredImageCount, caps.minImageCount()));
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

            ciSwapchain.preTransform(caps.currentTransform());
            ciSwapchain.compositeAlpha(instance.swapchainSettings.compositeAlpha());
            ciSwapchain.presentMode(presentMode);
            ciSwapchain.clipped(true);
            ciSwapchain.oldSwapchain(oldSwapchain);

            var pSwapchain = stack.callocLong(1);
            assertVkSuccess(vkCreateSwapchainKHR(
                    instance.vkDevice(), ciSwapchain, null, pSwapchain
            ), "CreateSwapchainKHR", null);
            long swapchain = pSwapchain.get(0);
            currentSwapchainID += 1;

            return new Swapchain(instance, swapchain, width, height, presentMode);
        }
    }

    private void destroyOldSwapchains() {
        for (var oldSwapchain : oldSwapchains) {
            oldSwapchain.destroy();
        }
        oldSwapchains.clear();
    }

    public void destroy() {
        if (currentSwapchain != null) {
            assertVkSuccess(vkDeviceWaitIdle(instance.vkDevice()), "DeviceWaitIdle", "SwapchainDestruction");
            destroyOldSwapchains();
            currentSwapchain.destroy();
            currentSwapchain = null;
        }
    }
}
