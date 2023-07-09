package troll.instance;

import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;
import troll.buffer.TrollBuffers;
import troll.commands.TrollCommands;
import troll.debug.TrollDebug;
import troll.descriptors.TrollDescriptors;
import troll.images.TrollImages;
import troll.pipelines.TrollPipelines;
import troll.queue.QueueFamilies;
import troll.surface.WindowSurface;
import troll.swapchain.SwapchainSettings;
import troll.swapchain.TrollSwapchains;
import troll.sync.TrollSync;

import java.util.Collections;
import java.util.Set;

import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.util.vma.Vma.vmaDestroyAllocator;
import static org.lwjgl.vulkan.VK10.vkDestroyDevice;
import static org.lwjgl.vulkan.VK10.vkDestroyInstance;

public class TrollInstance {

    private final long glfwWindow;
    private final WindowSurface windowSurface;
    public final SwapchainSettings swapchainSettings;

    private final VkInstance vkInstance;
    private final VkPhysicalDevice vkPhysicalDevice;
    private final VkDevice vkDevice;
    public final Set<String> instanceExtensions, deviceExtensions;
    private final QueueFamilies queueFamilies;
    private final long vmaAllocator;

    public final TrollBuffers buffers;
    public final TrollImages images;
    public final TrollDescriptors descriptors;
    public final TrollPipelines pipelines;
    public final TrollCommands commands;
    public final TrollSync sync;
    public final TrollSwapchains swapchains;
    public final TrollDebug debug;

    private boolean destroyed = false;

    public TrollInstance(
            long glfwWindow, WindowSurface windowSurface, SwapchainSettings swapchainSettings,
            VkInstance vkInstance, VkPhysicalDevice vkPhysicalDevice, VkDevice vkDevice,
            Set<String> instanceExtensions, Set<String> deviceExtensions,
            QueueFamilies queueFamilies, long vmaAllocator
    ) {
        this.glfwWindow = glfwWindow;
        this.windowSurface = windowSurface;
        this.swapchainSettings = swapchainSettings;
        this.vkInstance = vkInstance;
        this.vkPhysicalDevice = vkPhysicalDevice;
        this.vkDevice = vkDevice;
        this.instanceExtensions = Collections.unmodifiableSet(instanceExtensions);
        this.deviceExtensions = Collections.unmodifiableSet(deviceExtensions);
        this.queueFamilies = queueFamilies;
        this.vmaAllocator = vmaAllocator;

        this.buffers = new TrollBuffers(this);
        this.images = new TrollImages(this);
        this.descriptors = new TrollDescriptors(this);
        this.pipelines = new TrollPipelines(this);
        this.commands = new TrollCommands(this);
        this.sync = new TrollSync(this);
        this.swapchains = swapchainSettings != null ? new TrollSwapchains(this) : null;
        this.debug = new TrollDebug(this);
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

    public WindowSurface windowSurface() {
        checkDestroyed();
        checkWindow();
        return windowSurface;
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

    public long vmaAllocator() {
        checkDestroyed();
        return vmaAllocator;
    }

    public void destroy() {
        checkDestroyed();

        swapchains.destroy();
        vmaDestroyAllocator(vmaAllocator);
        vkDestroyDevice(vkDevice, null);
        if (windowSurface != null) windowSurface.destroy(vkInstance);
        vkDestroyInstance(vkInstance, null);
        if (glfwWindow != 0L) glfwDestroyWindow(glfwWindow);

        destroyed = true;
    }
}
