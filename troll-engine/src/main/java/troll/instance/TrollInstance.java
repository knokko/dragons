package troll.instance;

import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;

import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.vulkan.VK10.vkDestroyInstance;

public class TrollInstance {

    private final long glfwWindow;
    private final VkInstance vkInstance;
//    private final VkPhysicalDevice vkPhysicalDevice;
//    private final VkDevice vkDevice;

    private boolean destroyed = false;

    TrollInstance(long glfwWindow, VkInstance vkInstance) {
        this.glfwWindow = glfwWindow;
        this.vkInstance = vkInstance;
    }

    private void checkDestroyed() {
        if (destroyed) throw new IllegalStateException("This instance has already been destroyed");
    }

    public long glfwWindow() {
        checkDestroyed();
        if (glfwWindow == 0L) throw new UnsupportedOperationException("This instance doesn't have a window");
        return glfwWindow;
    }

    public VkInstance vkInstance() {
        checkDestroyed();
        return vkInstance;
    }

    public void destroy() {
        checkDestroyed();

        vkDestroyInstance(vkInstance, null);
        if (glfwWindow != 0L) glfwDestroyWindow(glfwWindow);

        destroyed = true;
    }
}
