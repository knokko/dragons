package graviks.glfw

import org.lwjgl.glfw.GLFWVulkan.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.system.MemoryUtil.memUTF8
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.util.vma.VmaAllocatorCreateInfo
import org.lwjgl.util.vma.VmaVulkanFunctions
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.EXTDebugUtils.*
import org.lwjgl.vulkan.EXTMemoryBudget.VK_EXT_MEMORY_BUDGET_EXTENSION_NAME
import org.lwjgl.vulkan.KHRBindMemory2.VK_KHR_BIND_MEMORY_2_EXTENSION_NAME
import org.lwjgl.vulkan.KHRDedicatedAllocation.VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME
import org.lwjgl.vulkan.KHRGetMemoryRequirements2.VK_KHR_GET_MEMORY_REQUIREMENTS_2_EXTENSION_NAME
import org.lwjgl.vulkan.KHRGetPhysicalDeviceProperties2.VK_KHR_GET_PHYSICAL_DEVICE_PROPERTIES_2_EXTENSION_NAME
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME
import org.lwjgl.vulkan.KHRSwapchain.vkCreateSwapchainKHR
import org.lwjgl.vulkan.VK10.*
import java.nio.ByteBuffer

internal fun assertSuccess(resultCode: Int, vararg otherAcceptedCodes: Int) {
    if (resultCode != VK_SUCCESS && !otherAcceptedCodes.contains(resultCode)) {
        throw RuntimeException("Result is $resultCode")
    }
}

internal fun createVulkanInstance(
    enableValidation: Boolean, applicationName: String, applicationVersion: Int
) = stackPush().use { stack ->

    if (!glfwVulkanSupported()) throw RuntimeException("No Vulkan-GLFW support")

    val appInfo = VkApplicationInfo.calloc(stack)
    appInfo.`sType$Default`()
    appInfo.pApplicationName(stack.UTF8(applicationName))
    appInfo.applicationVersion(applicationVersion)
    appInfo.apiVersion(VK_API_VERSION_1_0)

    val rawRequiredExtensions = glfwGetRequiredInstanceExtensions()
        ?: throw RuntimeException("No suitable set of instance extensions for GLFW was found")

    val requiredExtensions = HashSet<String>()
    for (extensionIndex in rawRequiredExtensions.position() until rawRequiredExtensions.limit()) {
        requiredExtensions.add(memUTF8(rawRequiredExtensions[extensionIndex]))
    }

    println("The following Vulkan instance extensions are required by GLFW:")
    for (extension in requiredExtensions) {
        println(extension)
    }
    println()

    if (enableValidation) {
        requiredExtensions.add(VK_EXT_DEBUG_UTILS_EXTENSION_NAME)
    }

    val availableExtensions = run {
        val pNumExtensions = stack.callocInt(1)
        assertSuccess(
            vkEnumerateInstanceExtensionProperties(null as ByteBuffer?, pNumExtensions, null)
        )
        val numExtensions = pNumExtensions[0]

        val pExtensions = VkExtensionProperties.calloc(numExtensions, stack)
        assertSuccess(
            vkEnumerateInstanceExtensionProperties(null as ByteBuffer?, pNumExtensions, pExtensions)
        )

        val extensions = HashSet<String>()
        for (extension in pExtensions) {
            extensions.add(extension.extensionNameString())
        }
        extensions
    }

    for (extension in requiredExtensions) {
        if (!availableExtensions.contains(extension)) {
            throw RuntimeException("Missing required Vulkan instance extension: $extension")
        }
    }

    val extensions = HashSet<String>()
    extensions.addAll(requiredExtensions)

    if (availableExtensions.contains(VK_KHR_GET_PHYSICAL_DEVICE_PROPERTIES_2_EXTENSION_NAME)) {
        extensions.add(VK_KHR_GET_PHYSICAL_DEVICE_PROPERTIES_2_EXTENSION_NAME)
    }

    val pExtensions = stack.callocPointer(extensions.size)
    for ((index, extension) in extensions.withIndex()) {
        pExtensions.put(index, stack.UTF8(extension))
    }

    val ciInstance = VkInstanceCreateInfo.calloc(stack)
    ciInstance.`sType$Default`()
    ciInstance.pApplicationInfo(appInfo)
    ciInstance.ppEnabledExtensionNames(pExtensions)

    if (enableValidation) {
        val availableLayers = run {
            val pNumLayers = stack.callocInt(1)
            assertSuccess(
                vkEnumerateInstanceLayerProperties(pNumLayers, null)
            )
            val numLayers = pNumLayers[0]

            val pLayers = VkLayerProperties.calloc(numLayers, stack)
            assertSuccess(
                vkEnumerateInstanceLayerProperties(pNumLayers, pLayers)
            )

            val layers = HashSet<String>()
            for (layer in pLayers) {
                layers.add(layer.layerNameString())
            }
            layers
        }

        if (!availableLayers.contains("VK_LAYER_KHRONOS_validation")) {
            throw RuntimeException("Missing validation layer")
        }
        ciInstance.ppEnabledLayerNames(stack.pointers(stack.UTF8("VK_LAYER_KHRONOS_validation")))
    }

    val pInstance = stack.callocPointer(1)
    assertSuccess(
        vkCreateInstance(ciInstance, null, pInstance)
    )

    val vkInstance = VkInstance(pInstance[0], ciInstance)

    if (enableValidation) {
        val ciDebug = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack)
        ciDebug.`sType$Default`()
        ciDebug.messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT or VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT)
        ciDebug.messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT or VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT)
        ciDebug.pfnUserCallback { messageSeverity, messageTypes, pCallbackData, pUserData ->
            val callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData)
            println("Validation: ${callbackData.pMessageString()}")
            VK_FALSE
        }

        val pDebug = stack.callocLong(1)
        assertSuccess(
            vkCreateDebugUtilsMessengerEXT(vkInstance, ciDebug, null, pDebug)
        )
        Pair(vkInstance, pDebug[0])
    } else {
        Pair(vkInstance, NULL)
    }
}

private fun getAvailableExtensions(device: VkPhysicalDevice, stack: MemoryStack): Set<String> {
    val pNumExtensions = stack.callocInt(1)
    assertSuccess(
        vkEnumerateDeviceExtensionProperties(device, null as ByteBuffer?, pNumExtensions, null)
    )
    val numExtensions = pNumExtensions[0]

    val pExtensions = VkExtensionProperties.calloc(numExtensions, stack)
    assertSuccess(
        vkEnumerateDeviceExtensionProperties(device, null as ByteBuffer?, pNumExtensions, pExtensions)
    )

    val extensions = HashSet<String>()
    for (extension in pExtensions) {
        extensions.add(extension.extensionNameString())
    }
    return extensions
}

internal fun chooseVulkanPhysicalDevice(vkInstance: VkInstance, surface: Long, preferPowerfulDevice: Boolean) = stackPush().use { stack ->
    val pNumDevices = stack.callocInt(1)
    assertSuccess(
        vkEnumeratePhysicalDevices(vkInstance, pNumDevices, null)
    )
    val numDevices = pNumDevices[0]

    val pDevices = stack.callocPointer(numDevices)
    assertSuccess(
        vkEnumeratePhysicalDevices(vkInstance, pNumDevices, pDevices)
    )

    var discreteDeviceIndex: Int? = null
    var discreteQueueFamilyIndex: Int? = null

    var integratedDeviceIndex: Int? = null
    var integratedQueueFamilyIndex: Int? = null

    var firstSupportedDeviceIndex: Int? = null
    var firstSupportedQueueFamilyIndex: Int? = null

    val deviceProperties = VkPhysicalDeviceProperties.calloc(stack)

    for (deviceIndex in 0 until numDevices) {
        val device = VkPhysicalDevice(pDevices[deviceIndex], vkInstance)

        val pNumQueueFamilies = stack.callocInt(1)
        vkGetPhysicalDeviceQueueFamilyProperties(device, pNumQueueFamilies, null)
        val numQueueFamilies = pNumQueueFamilies[0]

        val pQueueFamilies = VkQueueFamilyProperties.calloc(numQueueFamilies, stack)
        vkGetPhysicalDeviceQueueFamilyProperties(device, pNumQueueFamilies, pQueueFamilies)

        // Check if the device is suitable for presentation
        if (getAvailableExtensions(device, stack).contains(VK_KHR_SWAPCHAIN_EXTENSION_NAME)) {
            val pNumFormats = stack.callocInt(1)
            assertSuccess(
                vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, pNumFormats, null)
            )

            val pNumPresentModes = stack.callocInt(1)
            assertSuccess(
                vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, pNumPresentModes, null)
            )

            // We need at least 1 surface format and at least 1 present mode
            if (pNumFormats[0] > 0 && pNumPresentModes[0] > 0) {

                // Check if the device has at least 1 graphics queue family with presentation support
                for ((queueFamilyIndex, queueFamily) in pQueueFamilies.withIndex()) {
                    if ((queueFamily.queueFlags() and VK_QUEUE_GRAPHICS_BIT) != 0 && glfwGetPhysicalDevicePresentationSupport(
                            vkInstance,
                            device,
                            queueFamilyIndex
                        )
                    ) {

                        val pSupportsPresent = stack.callocInt(1)
                        assertSuccess(
                            vkGetPhysicalDeviceSurfaceSupportKHR(
                                device,
                                queueFamilyIndex,
                                surface,
                                pSupportsPresent
                            )
                        )
                        if (pSupportsPresent[0] == VK_TRUE) {

                            // Check what kind of device it is
                            vkGetPhysicalDeviceProperties(device, deviceProperties)
                            if (deviceProperties.deviceType() == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) {
                                discreteDeviceIndex = deviceIndex
                                discreteQueueFamilyIndex = queueFamilyIndex
                            }
                            if (deviceProperties.deviceType() == VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU) {
                                integratedDeviceIndex = deviceIndex
                                integratedQueueFamilyIndex = queueFamilyIndex
                            }
                            if (firstSupportedDeviceIndex == null) {
                                firstSupportedDeviceIndex = deviceIndex
                                firstSupportedQueueFamilyIndex = queueFamilyIndex
                            }

                            // 1 queue family is enough: don't bother looking for more
                            break
                        }
                    }
                }
            }
        }
    }

    if (firstSupportedDeviceIndex == null) throw RuntimeException("No suitable physical device was found")

    val (chosenDevicePointer, rawQueueFamilyIndex) = if (preferPowerfulDevice) {
        if (discreteDeviceIndex != null) Pair(pDevices[discreteDeviceIndex], discreteQueueFamilyIndex)
        else if (integratedDeviceIndex != null) Pair(pDevices[integratedDeviceIndex], integratedQueueFamilyIndex)
        else Pair(pDevices[firstSupportedDeviceIndex], firstSupportedQueueFamilyIndex)
    } else {
        if (integratedDeviceIndex != null) Pair(pDevices[integratedDeviceIndex], integratedQueueFamilyIndex)
        else if (discreteDeviceIndex != null) Pair(pDevices[discreteDeviceIndex], discreteQueueFamilyIndex)
        else Pair(pDevices[firstSupportedDeviceIndex], firstSupportedQueueFamilyIndex)
    }

    Pair(VkPhysicalDevice(chosenDevicePointer, vkInstance), rawQueueFamilyIndex!!)
}

internal fun createVulkanDevice(chosenPhysicalDevice: VkPhysicalDevice, chosenQueueFamilyIndex: Int) = stackPush().use { stack ->
    val availableExtensions = getAvailableExtensions(chosenPhysicalDevice, stack)

    val extensions = HashSet<String>()
    for (desiredExtension in arrayOf(
        VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME,
        VK_KHR_GET_MEMORY_REQUIREMENTS_2_EXTENSION_NAME,
        VK_KHR_BIND_MEMORY_2_EXTENSION_NAME,
        VK_EXT_MEMORY_BUDGET_EXTENSION_NAME
    )) {
        if (availableExtensions.contains(desiredExtension)) {
            extensions.add(desiredExtension)
        }
    }

    // The physical device selection procedure will only choose devices that support this extension
    extensions.add(VK_KHR_SWAPCHAIN_EXTENSION_NAME)

    val pExtensions = stack.callocPointer(extensions.size)
    for ((index, extension) in extensions.withIndex()) {
        pExtensions.put(index, stack.UTF8(extension))
    }

    val ciQueues = VkDeviceQueueCreateInfo.calloc(1, stack)
    val ciQueue = ciQueues[0]
    ciQueue.`sType$Default`()
    ciQueue.queueFamilyIndex(chosenQueueFamilyIndex)
    ciQueue.pQueuePriorities(stack.floats(0.5f))

    val ciDevice = VkDeviceCreateInfo.calloc(stack)
    ciDevice.`sType$Default`()
    ciDevice.pQueueCreateInfos(ciQueues)
    ciDevice.ppEnabledExtensionNames(pExtensions)

    val pDevice = stack.callocPointer(1)
    assertSuccess(
        vkCreateDevice(chosenPhysicalDevice, ciDevice, null, pDevice)
    )
    val device = VkDevice(pDevice[0], chosenPhysicalDevice, ciDevice)

    val pQueue = stack.callocPointer(1)
    vkGetDeviceQueue(device, chosenQueueFamilyIndex, 0, pQueue)

    Triple(device, extensions, VkQueue(pQueue[0], device))
}

internal fun createVulkanMemoryAllocator(
    vkInstance: VkInstance, vkPhysicalDevice: VkPhysicalDevice, vkDevice: VkDevice, deviceExtensions: Set<String>
) = stackPush().use { stack ->
    val vmaVulkanFunctions = VmaVulkanFunctions.calloc(stack)
    vmaVulkanFunctions.set(vkInstance, vkDevice)

    var allocatorFlags = 0
    if (deviceExtensions.contains(VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME)) {
        allocatorFlags = allocatorFlags or VMA_ALLOCATOR_CREATE_KHR_DEDICATED_ALLOCATION_BIT
    }
    if (deviceExtensions.contains(VK_KHR_BIND_MEMORY_2_EXTENSION_NAME)) {
        allocatorFlags = allocatorFlags or VMA_ALLOCATOR_CREATE_KHR_BIND_MEMORY2_BIT
    }
    if (deviceExtensions.contains(VK_EXT_MEMORY_BUDGET_EXTENSION_NAME)) {
        allocatorFlags = allocatorFlags or VMA_ALLOCATOR_CREATE_EXT_MEMORY_BUDGET_BIT
    }

    val ciAllocator = VmaAllocatorCreateInfo.calloc(stack)
    ciAllocator.flags(allocatorFlags)
    ciAllocator.vulkanApiVersion(VK_API_VERSION_1_0)
    ciAllocator.physicalDevice(vkPhysicalDevice)
    ciAllocator.device(vkDevice)
    ciAllocator.instance(vkInstance)
    ciAllocator.pVulkanFunctions(vmaVulkanFunctions)

    val pAllocator = stack.callocPointer(1)
    assertSuccess(vmaCreateAllocator(ciAllocator, pAllocator))
    pAllocator[0]
}

fun createVulkanSwapchain(
    vkPhysicalDevice: VkPhysicalDevice, vkDevice: VkDevice, surface: Long, width: Int, height: Int
) = stackPush().use { stack ->
    val surfaceCapabilities = VkSurfaceCapabilitiesKHR.calloc(stack)
    assertSuccess(
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(vkPhysicalDevice, surface, surfaceCapabilities)
    )

    val pNumSurfaces = stack.callocInt(1)
    assertSuccess(
        vkGetPhysicalDeviceSurfaceFormatsKHR(vkPhysicalDevice, surface, pNumSurfaces, null)
    )
    val numSurfaces = pNumSurfaces[0]

    val surfaceFormats = VkSurfaceFormatKHR.calloc(numSurfaces, stack)
    assertSuccess(
        vkGetPhysicalDeviceSurfaceFormatsKHR(vkPhysicalDevice, surface, pNumSurfaces, surfaceFormats)
    )

    // TODO Experiment with this... I wonder what happens if the format doesn't match the internal Graviks2D format...
    var chosenSurfaceFormat = surfaceFormats[0]
    for (surfaceFormat in surfaceFormats) {
        if (surfaceFormat.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR && surfaceFormat.format() == VK_FORMAT_B8G8R8A8_SRGB) {
            chosenSurfaceFormat = surfaceFormat
            break
        }
    }

    var minImageCount = surfaceCapabilities.minImageCount()
    if (surfaceCapabilities.maxImageCount() == 0 || surfaceCapabilities.maxImageCount() > minImageCount) {
        minImageCount += 1
    }

    val ciSwapchain = VkSwapchainCreateInfoKHR.calloc(stack)
    ciSwapchain.`sType$Default`()
    ciSwapchain.surface(surface)
    ciSwapchain.minImageCount(minImageCount)
    ciSwapchain.imageFormat(chosenSurfaceFormat.format())
    ciSwapchain.imageColorSpace(chosenSurfaceFormat.colorSpace())
    ciSwapchain.imageExtent { extent -> extent.set(width, height) }
    ciSwapchain.imageArrayLayers(1)
    ciSwapchain.imageUsage(VK_IMAGE_USAGE_TRANSFER_DST_BIT)
    ciSwapchain.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
    ciSwapchain.preTransform(surfaceCapabilities.currentTransform())
    ciSwapchain.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
    ciSwapchain.presentMode(VK_PRESENT_MODE_FIFO_KHR)
    ciSwapchain.clipped(true)
    ciSwapchain.oldSwapchain(VK_NULL_HANDLE)

    val pSwapchain = stack.callocLong(1)
    assertSuccess(
        vkCreateSwapchainKHR(vkDevice, ciSwapchain, null, pSwapchain)
    )
}
