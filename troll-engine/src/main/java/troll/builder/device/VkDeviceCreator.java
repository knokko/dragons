package troll.builder.device;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDevice;

@FunctionalInterface
public interface VkDeviceCreator {

    VkDevice vkCreateDevice(MemoryStack stack, VkPhysicalDevice physicalDevice, VkDeviceCreateInfo ciDevice);
}
