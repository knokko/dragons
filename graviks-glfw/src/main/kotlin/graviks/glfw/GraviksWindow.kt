package graviks.glfw

import graviks2d.context.GraviksContext
import graviks2d.core.GraviksInstance
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.util.vma.Vma.vmaDestroyAllocator
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT
import org.lwjgl.vulkan.KHRIncrementalPresent.VK_KHR_INCREMENTAL_PRESENT_EXTENSION_NAME
import org.lwjgl.vulkan.KHRPresentWait.VK_KHR_PRESENT_WAIT_EXTENSION_NAME
import org.lwjgl.vulkan.KHRPresentWait.vkWaitForPresentKHR
import org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK10.*
import java.lang.System.nanoTime

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
    val canAwaitPresent: Boolean
    private var lastPresentId = 0
    val hasIncrementalPresent: Boolean

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
            presentFrame(false, null)
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
        this.canAwaitPresent = deviceExtensions.contains(VK_KHR_PRESENT_WAIT_EXTENSION_NAME)
        this.hasIncrementalPresent = deviceExtensions.contains(VK_KHR_INCREMENTAL_PRESENT_EXTENSION_NAME)

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

    fun presentFrame(waitUntilVisible: Boolean, fillPresentRegions: ((MemoryStack) -> VkRectLayerKHR.Buffer)?) {
        if (waitUntilVisible && !canAwaitPresent) {
            throw UnsupportedOperationException("Waiting until presentation is not supported by the Vulkan implementation")
        }

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
                imageDstAccessMask = 0, imageDstStageMask = VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
                shouldAwaitCompletion = false
            )

            val incrementalPresentAddress = if (hasIncrementalPresent && fillPresentRegions != null) {
                val presentRectangles = fillPresentRegions(stack)

                val presentRegions = VkPresentRegionKHR.calloc(1, stack)
                presentRegions.rectangleCount(presentRectangles.capacity())
                presentRegions.pRectangles(presentRectangles)

                val incrementalPresent = VkPresentRegionsKHR.calloc(stack)
                incrementalPresent.`sType$Default`()
                incrementalPresent.swapchainCount(1)
                incrementalPresent.pRegions(presentRegions)

                incrementalPresent.address()
            } else 0L

            val pPresentId = stack.longs(lastPresentId + 1L)

            val presentIdAddress = if (canAwaitPresent) {
                val presentIdInfo = VkPresentIdKHR.calloc(stack)
                presentIdInfo.`sType$Default`()
                presentIdInfo.pNext(incrementalPresentAddress)
                presentIdInfo.swapchainCount(1)
                presentIdInfo.pPresentIds(pPresentId)

                presentIdInfo.address()
            } else 0L

            val presentInfo = VkPresentInfoKHR.calloc(stack)
            presentInfo.`sType$Default`()
            if (waitUntilVisible) presentInfo.pNext(presentIdAddress)
            else presentInfo.pNext(incrementalPresentAddress)
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

            if (waitUntilVisible) {
                val startTime = nanoTime()
                assertSuccess(vkWaitForPresentKHR(graviksInstance.device, swapchain!!, pPresentId[0], 1_000_000_000L))
                println("presentation took ${(nanoTime() - startTime) / 1000} microseconds")
                lastPresentId += 1
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
