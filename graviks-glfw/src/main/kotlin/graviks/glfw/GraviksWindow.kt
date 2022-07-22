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
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkFenceCreateInfo
import org.lwjgl.vulkan.VkPresentInfoKHR
import org.lwjgl.vulkan.VkQueue
import org.lwjgl.vulkan.VkSemaphoreCreateInfo

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
    private val createContext: (instance: GraviksInstance, width: Int, height: Int) -> GraviksContext
) {

    val windowHandle: Long
    private val windowSurface: Long
    private val queue: VkQueue
    val graviksInstance: GraviksInstance

    private val acquireFence: Long
    private val copySemaphore: Long
    private val debugMessenger: Long

    // Swapchain-dependant variables
    private var swapchain: Long? = null
    private var swapchainFormat: Int? = null
    private var swapchainImages: LongArray? = null
    var graviksContext: GraviksContext? = null
    private var shouldResize = false

    init {
        if (!glfwInit()) throw RuntimeException("Failed to initialize GLFW")

        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API)
        windowHandle = glfwCreateWindow(initialWidth, initialHeight, title, monitor, shareWindow)

        if (windowHandle == NULL) throw RuntimeException("Failed to create window")

        glfwSetWindowRefreshCallback(windowHandle) {
            presentFrame()
        }

        glfwSetFramebufferSizeCallback(windowHandle) { _, _, _->
            shouldResize = true
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
        this.queue = queue
        val vmaAllocator = createVulkanMemoryAllocator(vkInstance, vkPhysicalDevice, vkDevice, deviceExtensions)

        graviksInstance = GraviksInstance(
            vkInstance, vkPhysicalDevice, vkDevice, vmaAllocator, queueFamilyIndex,
            { pSubmitInfo, fence -> vkQueueSubmit(queue, pSubmitInfo, fence) }
        )

        acquireFence = stackPush().use { stack ->
            val ciFence = VkFenceCreateInfo.calloc(stack)
            ciFence.`sType$Default`()

            val pFence = stack.callocLong(1)
            assertSuccess(vkCreateFence(vkDevice, ciFence, null, pFence))
            pFence[0]
        }

        copySemaphore = stackPush().use { stack ->
            val ciSemaphore = VkSemaphoreCreateInfo.calloc(stack)
            ciSemaphore.`sType$Default`()

            val pSemaphore = stack.callocLong(1)
            assertSuccess(vkCreateSemaphore(vkDevice, ciSemaphore, null, pSemaphore))
            pSemaphore[0]
        }

        createSwapchain()
    }

    private fun createSwapchain() {
        val (actualInitialWidth, actualInitialHeight) = stackPush().use { stack ->
            val pWidth = stack.callocInt(1)
            val pHeight = stack.callocInt(1)
            glfwGetFramebufferSize(windowHandle, pWidth, pHeight)
            Pair(pWidth[0], pHeight[0])
        }

        if (actualInitialWidth == 0 || actualInitialHeight == 0) {
            this.swapchain = null
            this.swapchainFormat = null
            this.swapchainImages = null
            this.graviksContext = null
            return
        }

        val (swapchain, swapchainFormat, swapchainImages) = createVulkanSwapchain(
            graviksInstance.physicalDevice, graviksInstance.device, windowSurface, actualInitialWidth, actualInitialHeight
        )
        this.swapchain = swapchain
        this.swapchainFormat = swapchainFormat
        this.swapchainImages = swapchainImages

        this.graviksContext = createContext(graviksInstance, actualInitialWidth, actualInitialHeight)
    }

    fun shouldResize() = shouldResize

    fun resize() {
        destroySwapchain()
        createSwapchain()
        shouldResize = false
    }

    fun presentFrame() {
        // The swapchain will be null when the window is minified. In this case, we should not do anything
        if (swapchain == null) return

        // If it takes more than 1 second to present a frame, something is wrong
        val timeout = 1_000_000_000L

        stackPush().use { stack ->
            val pImageIndex = stack.callocInt(1)
            val acquireResult = vkAcquireNextImageKHR(
                graviksInstance.device, swapchain!!, timeout, VK_NULL_HANDLE, acquireFence, pImageIndex
            )
            if (acquireResult == VK_SUBOPTIMAL_KHR || acquireResult == VK_ERROR_OUT_OF_DATE_KHR) {
                shouldResize = true
            } else if (acquireResult != VK_SUCCESS) {
                throw RuntimeException("vkAcquireNextImageKHR returned $acquireResult")
            }

            if (acquireResult == VK_ERROR_OUT_OF_DATE_KHR) {
                assertSuccess(vkResetFences(graviksInstance.device, stack.longs(acquireFence)))
                return
            }

            val imageIndex = pImageIndex[0]

            assertSuccess(vkWaitForFences(graviksInstance.device, stack.longs(acquireFence), true, timeout))
            assertSuccess(vkResetFences(graviksInstance.device, stack.longs(acquireFence)))

            graviksContext!!.copyColorImageTo(
                destImage = swapchainImages!![imageIndex], destImageFormat = swapchainFormat,
                destBuffer = null, signalSemaphore = copySemaphore,
                originalImageLayout = VK_IMAGE_LAYOUT_UNDEFINED, finalImageLayout = VK_IMAGE_LAYOUT_PRESENT_SRC_KHR,
                imageSrcAccessMask = 0, imageSrcStageMask = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                // There is no need to give proper destinations masks since vkQueuePresentKHR takes care of that
                imageDstAccessMask = 0, imageDstStageMask = VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT
            )

            val presentInfo = VkPresentInfoKHR.calloc(stack)
            presentInfo.`sType$Default`()
            presentInfo.pWaitSemaphores(stack.longs(copySemaphore))
            presentInfo.swapchainCount(1)
            presentInfo.pSwapchains(stack.longs(swapchain!!))
            presentInfo.pImageIndices(pImageIndex)

            val presentResult = vkQueuePresentKHR(queue, presentInfo)
            if (presentResult == VK_SUBOPTIMAL_KHR || presentResult == VK_ERROR_OUT_OF_DATE_KHR) {
                shouldResize = true
            } else if (presentResult != VK_SUCCESS) {
                throw RuntimeException("vkQueuePresentKHR returned $presentResult")
            }
        }
    }

    private fun destroySwapchain() {
        vkDeviceWaitIdle(graviksInstance.device)
        if (swapchain != null) {
            graviksContext!!.destroy()
            vkDestroySwapchainKHR(graviksInstance.device, swapchain!!, null)
        }
    }

    fun destroy() {
        destroySwapchain()
        vkDestroyFence(graviksInstance.device, acquireFence, null)
        vkDestroySemaphore(graviksInstance.device, copySemaphore, null)
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
