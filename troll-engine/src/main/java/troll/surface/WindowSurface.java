package troll.surface;

import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;

import java.util.Set;

import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;

public record WindowSurface(
        long vkSurface,
        Set<SurfaceFormat> formats,
        Set<Integer> presentModes,
        VkSurfaceCapabilitiesKHR initialCapabilities
) {
    public void destroy(VkInstance vkInstance) {
        vkDestroySurfaceKHR(vkInstance, vkSurface, null);
        initialCapabilities.free();
    }
}
