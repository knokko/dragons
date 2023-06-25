package troll.builder.instance;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;

@FunctionalInterface
public interface VkInstanceCreator {

    VkInstance vkCreateInstance(MemoryStack stack, VkInstanceCreateInfo ciInstance);
}
