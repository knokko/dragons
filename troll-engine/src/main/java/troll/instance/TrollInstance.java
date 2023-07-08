package troll.instance;

import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;
import troll.buffer.TrollBuffers;
import troll.commands.TrollCommands;
import troll.debug.TrollDebug;
import troll.descriptors.TrollDescriptors;
import troll.pipelines.TrollPipelines;
import troll.queue.QueueFamilies;
import troll.sync.TrollSync;

import java.util.Collections;
import java.util.Set;

import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.util.vma.Vma.vmaDestroyAllocator;
import static org.lwjgl.vulkan.VK10.vkDestroyDevice;
import static org.lwjgl.vulkan.VK10.vkDestroyInstance;

public class TrollInstance {

    private final long glfwWindow;
    private final long glfwWindowSurface;
    private final VkInstance vkInstance;
    private final VkPhysicalDevice vkPhysicalDevice;
    private final VkDevice vkDevice;
    public final Set<String> instanceExtensions, deviceExtensions;
    private final QueueFamilies queueFamilies;
    private final long vmaAllocator;

    public final TrollBuffers buffers;
    public final TrollDescriptors descriptors;
    public final TrollPipelines pipelines;
    public final TrollCommands commands;
    public final TrollSync sync;
    public final TrollDebug debug;

    private boolean destroyed = false;

    public TrollInstance(
            long glfwWindow, long glfwWindowSurface,
            VkInstance vkInstance, VkPhysicalDevice vkPhysicalDevice, VkDevice vkDevice,
            Set<String> instanceExtensions, Set<String> deviceExtensions,
            QueueFamilies queueFamilies, long vmaAllocator
    ) {
        this.glfwWindow = glfwWindow;
        this.glfwWindowSurface = glfwWindowSurface;
        this.vkInstance = vkInstance;
        this.vkPhysicalDevice = vkPhysicalDevice;
        this.vkDevice = vkDevice;
        this.instanceExtensions = Collections.unmodifiableSet(instanceExtensions);
        this.deviceExtensions = Collections.unmodifiableSet(deviceExtensions);
        this.queueFamilies = queueFamilies;
        this.vmaAllocator = vmaAllocator;

        this.buffers = new TrollBuffers(this);
        this.descriptors = new TrollDescriptors(this);
        this.pipelines = new TrollPipelines(this);
        this.commands = new TrollCommands(this);
        this.sync = new TrollSync(this);
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

    public long vmaAllocator() {
        checkDestroyed();
        return vmaAllocator;
    }

    public void destroy() {
        checkDestroyed();

        vmaDestroyAllocator(vmaAllocator);
        vkDestroyDevice(vkDevice, null);
        vkDestroyInstance(vkInstance, null);
        if (glfwWindow != 0L) glfwDestroyWindow(glfwWindow);

        destroyed = true;
    }
}
