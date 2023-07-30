package troll.builder;

import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import troll.builder.swapchain.CompositeAlphaPicker;
import troll.builder.swapchain.SimpleCompositeAlphaPicker;
import troll.builder.swapchain.SimpleSurfaceFormatPicker;
import troll.builder.swapchain.SurfaceFormatPicker;
import troll.surface.SurfaceFormat;
import troll.surface.WindowSurface;
import troll.swapchain.SwapchainSettings;

import java.util.Collections;
import java.util.HashSet;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.VK10.*;
import static troll.exceptions.VulkanFailureException.assertVkSuccess;

public class TrollSwapchainBuilder {

    static WindowSurface createSurface(VkPhysicalDevice vkPhysicalDevice, long vkSurface) {
        // Note: do NOT allocate the capabilities on the stack because it needs to be read later!
        var capabilities = VkSurfaceCapabilitiesKHR.calloc();

        try (var stack = stackPush()) {
            assertVkSuccess(vkGetPhysicalDeviceSurfaceCapabilitiesKHR(
                    vkPhysicalDevice, vkSurface, capabilities
            ), "GetPhysicalDeviceSurfaceCapabilitiesKHR", "TrollSwapchainBuilder");

            var pNumFormats = stack.callocInt(1);
            assertVkSuccess(vkGetPhysicalDeviceSurfaceFormatsKHR(
                    vkPhysicalDevice, vkSurface, pNumFormats, null
            ), "GetPhysicalDeviceSurfaceFormatsKHR", "TrollSwapchainBuilder-Count");
            int numFormats = pNumFormats.get(0);

            var pFormats = VkSurfaceFormatKHR.calloc(numFormats, stack);
            assertVkSuccess(vkGetPhysicalDeviceSurfaceFormatsKHR(
                    vkPhysicalDevice, vkSurface, pNumFormats, pFormats
            ), "GetPhysicalDeviceSurfaceFormatsKHR", "TrollSwapchainBuilder-Elements");

            var formats = new HashSet<SurfaceFormat>(numFormats);
            for (int index = 0; index < numFormats; index++) {
                var format = pFormats.get(index);
                formats.add(new SurfaceFormat(format.format(), format.colorSpace()));
            }

            var pNumPresentModes = stack.callocInt(1);
            assertVkSuccess(vkGetPhysicalDeviceSurfacePresentModesKHR(
                    vkPhysicalDevice, vkSurface, pNumPresentModes, null
            ), "GetPhysicalDeviceSurfacePresentModesKHR", "TrollSwapchainBuilder-Count");
            int numPresentModes = pNumPresentModes.get(0);

            var pPresentModes = stack.callocInt(numPresentModes);
            assertVkSuccess(vkGetPhysicalDeviceSurfacePresentModesKHR(
                    vkPhysicalDevice, vkSurface, pNumPresentModes, pPresentModes
            ), "GetPhysicalDeviceSurfacePresentModesKHR", "TrollSwapchainBuilder-Count");

            var presentModes = new HashSet<Integer>(numPresentModes);
            for (int index = 0; index < numPresentModes; index++) {
                presentModes.add(pPresentModes.get(index));
            }

            return new WindowSurface(
                    vkSurface, Collections.unmodifiableSet(formats),
                    Collections.unmodifiableSet(presentModes), capabilities
            );
        }
    }

    final int imageUsage;
    SurfaceFormatPicker surfaceFormatPicker = new SimpleSurfaceFormatPicker(
            VK_FORMAT_B8G8R8A8_UNORM, VK_FORMAT_R8G8B8A8_SRGB, VK_FORMAT_B8G8R8A8_SRGB
    );
    CompositeAlphaPicker compositeAlphaPicker = new SimpleCompositeAlphaPicker(
            VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR, VK_COMPOSITE_ALPHA_INHERIT_BIT_KHR
    );

    public TrollSwapchainBuilder(int imageUsage) {
        this.imageUsage = imageUsage;
    }

    public TrollSwapchainBuilder surfaceFormatPicker(SurfaceFormatPicker surfaceFormatPicker) {
        this.surfaceFormatPicker = surfaceFormatPicker;
        return this;
    }

    public TrollSwapchainBuilder compositeAlphaPicker(CompositeAlphaPicker compositeAlphaPicker) {
        this.compositeAlphaPicker = compositeAlphaPicker;
        return this;
    }

    SwapchainSettings chooseSwapchainSettings(WindowSurface windowSurface) {
        return new SwapchainSettings(
                imageUsage,
                surfaceFormatPicker.chooseSurfaceFormat(windowSurface.formats()),
                compositeAlphaPicker.chooseCompositeAlpha(windowSurface.capabilities().supportedCompositeAlpha())
        );
    }
}
