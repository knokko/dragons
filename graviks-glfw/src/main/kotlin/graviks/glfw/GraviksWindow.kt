package graviks.glfw

import graviks2d.context.GraviksContext
import graviks2d.core.GraviksInstance
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.util.vma.Vma.vmaDestroyAllocator
import org.lwjgl.vulkan.EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT
import org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR
import org.lwjgl.vulkan.VK10.*

class GraviksWindow(
    initialWidth: Int,
    initialHeight: Int,
    title: String,
    enableValidation: Boolean,
    applicationName: String,
    applicationVersion: Int,
    preferPowerfulDevice: Boolean,
    monitor: Long = NULL,
    shareWindow: Long = NULL,
    val createContext: (width: Int, height: Int) -> GraviksContext
) {

    val windowHandle: Long
    val windowSurface: Long
    val graviksInstance: GraviksInstance
    val debugMessenger: Long

    init {
        if (!glfwInit()) throw RuntimeException("Failed to initialize GLFW")

        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API)
        windowHandle = glfwCreateWindow(initialWidth, initialHeight, title, monitor, shareWindow)

        if (windowHandle == NULL) throw RuntimeException("Failed to create window")

        glfwSetWindowRefreshCallback(windowHandle) {
            // TODO Redraw
        }

        glfwSetFramebufferSizeCallback(windowHandle) { _, newWidth, newHeight ->
            // TODO Recreate context once idle for X milliseconds
        }

        val (vkInstance, debugMessenger) = createVulkanInstance(enableValidation, applicationName, applicationVersion)
        this.debugMessenger = debugMessenger
        windowSurface = stackPush().use { stack ->
            val pSurface = stack.callocLong(1)
            assertSuccess(glfwCreateWindowSurface(vkInstance, windowHandle, null, pSurface))
            pSurface[0]
        }

        val (vkPhysicalDevice, queueFamilyIndex) = chooseVulkanPhysicalDevice(vkInstance, windowSurface, preferPowerfulDevice)
        val (vkDevice, deviceExtensions, queue) = createVulkanDevice(vkPhysicalDevice, queueFamilyIndex)
        val vmaAllocator = createVulkanMemoryAllocator(vkInstance, vkPhysicalDevice, vkDevice, deviceExtensions)

        graviksInstance = GraviksInstance(
            vkInstance, vkPhysicalDevice, vkDevice, vmaAllocator, queueFamilyIndex,
            { pSubmitInfo, fence -> vkQueueSubmit(queue, pSubmitInfo, fence) }
        )
    }

    fun destroy() {
        graviksInstance.destroy()

        vmaDestroyAllocator(graviksInstance.vmaAllocator)
        vkDestroyDevice(graviksInstance.device, null)

        vkDestroySurfaceKHR(graviksInstance.instance, windowSurface, null)
        if (debugMessenger != NULL) {
            vkDestroyDebugUtilsMessengerEXT(graviksInstance.instance, debugMessenger, null)
        }
        vkDestroyInstance(graviksInstance.instance, null)

        glfwDestroyWindow(windowHandle)
        glfwTerminate()
    }
}
