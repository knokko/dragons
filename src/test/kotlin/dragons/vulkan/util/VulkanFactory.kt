package dragons.vulkan.util

import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkInstanceCreateInfo
import org.lwjgl.vulkan.VkPhysicalDevice

/**
 * Creates a minimal, but valid VkInstance
 */
fun createDummyVulkanInstance(): VkInstance {
    return stackPush().use { stack ->
        val ciInstance = VkInstanceCreateInfo.callocStack(stack)
        ciInstance.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)

        val pInstance = stack.callocPointer(1)
        assertVkSuccess(vkCreateInstance(ciInstance, null, pInstance), "CreateInstance", "dummy")

        VkInstance(pInstance[0], ciInstance)
    }
}

/**
 * Queries a VkPhysicalDevice from the given VkInstance
 */
fun getDummyVulkanPhysicalDevice(vkInstance: VkInstance): VkPhysicalDevice {
    return stackPush().use { stack ->
        val pDeviceCount = stack.ints(1)

        val pDevice = stack.callocPointer(1)
        assertVkSuccess(vkEnumeratePhysicalDevices(vkInstance, pDeviceCount, pDevice), "EnumeratePhysicalDevices", "dummy")

        VkPhysicalDevice(pDevice[0], vkInstance)
    }
}
