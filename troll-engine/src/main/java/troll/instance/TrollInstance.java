package troll.instance;

import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;
import troll.queue.QueueFamilies;

import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.vulkan.VK10.vkDestroyDevice;
import static org.lwjgl.vulkan.VK10.vkDestroyInstance;

public class TrollInstance {

    private final long glfwWindow;
    private final long glfwWindowSurface;
    private final VkInstance vkInstance;
    private final VkPhysicalDevice vkPhysicalDevice;
    private final VkDevice vkDevice;
    private final QueueFamilies queueFamilies;

    private boolean destroyed = false;

    public TrollInstance(
            long glfwWindow, long glfwWindowSurface,
            VkInstance vkInstance, VkPhysicalDevice vkPhysicalDevice, VkDevice vkDevice, QueueFamilies queueFamilies
    ) {
        this.glfwWindow = glfwWindow;
        this.glfwWindowSurface = glfwWindowSurface;
        this.vkInstance = vkInstance;
        this.vkPhysicalDevice = vkPhysicalDevice;
        this.vkDevice = vkDevice;
        this.queueFamilies = queueFamilies;
    }

    private void checkDestroyed() {
        if (destroyed) throw new IllegalStateException("This instance has already been destroyed");
    }

    private void checkWindow() {
        if (glfwWindow == 0L) throw new UnsupportedOperationException("This instance doesn't have a window");
    }

    public long glfwWindow() {
        checkDestroyed();
        checkWindow();
        return glfwWindow;
    }

    public long glfwWindowSurface() {
        checkDestroyed();
        checkWindow();
        return glfwWindowSurface;
    }

    public VkInstance vkInstance() {
        checkDestroyed();
        return vkInstance;
    }

    public VkPhysicalDevice vkPhysicalDevice() {
        checkDestroyed();
        return vkPhysicalDevice;
    }

    public VkDevice vkDevice() {
        checkDestroyed();
        return vkDevice;
    }

    public QueueFamilies queueFamilies() {
        checkDestroyed();
        return queueFamilies;
    }

    public void destroy() {
        checkDestroyed();

        vkDestroyDevice(vkDevice, null);
        vkDestroyInstance(vkInstance, null);
        if (glfwWindow != 0L) glfwDestroyWindow(glfwWindow);

        destroyed = true;
    }
}
