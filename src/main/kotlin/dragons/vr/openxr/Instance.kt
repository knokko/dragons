package dragons.vr.openxr

import dragons.init.GameInitProperties
import dragons.init.trouble.ExtensionStartupException
import org.lwjgl.openxr.*
import org.lwjgl.openxr.EXTDebugUtils.XR_EXT_DEBUG_UTILS_EXTENSION_NAME
import org.lwjgl.openxr.KHRVulkanEnable2.XR_KHR_VULKAN_ENABLE2_EXTENSION_NAME
import org.lwjgl.openxr.XR10.XR_MAKE_VERSION
import org.lwjgl.openxr.XR10.xrCreateInstance
import org.lwjgl.system.MemoryStack.stackPush
import org.slf4j.Logger

private const val XR_VALIDATION_LAYER_NAME = "XR_APILAYER_LUNARG_core_validation"

internal fun createOpenXrInstance(initProps: GameInitProperties, logger: Logger): XrInstance? {
    return stackPush().use { stack ->

        val availableExtensions = getAvailableOpenXrExtensions(logger)
        if (!availableExtensions.contains(XR_KHR_VULKAN_ENABLE2_EXTENSION_NAME)) {
            logger.error("Missing required OpenXR extension $XR_KHR_VULKAN_ENABLE2_EXTENSION_NAME")
            if (initProps.mainParameters.requiresHmd) {
                throw ExtensionStartupException(
                    "Missing required OpenXR extension",
                    "The game requires the OpenXR extension $XR_KHR_VULKAN_ENABLE2_EXTENSION_NAME, but it is not available.",
                    availableExtensions = availableExtensions,
                    requiredExtensions = setOf(XR_KHR_VULKAN_ENABLE2_EXTENSION_NAME)
                )
            }
            return null
        }

        val extensionsToEnable = mutableSetOf(XR_KHR_VULKAN_ENABLE2_EXTENSION_NAME)
        if (!initProps.mainParameters.forbidDebug && availableExtensions.contains(XR_EXT_DEBUG_UTILS_EXTENSION_NAME)) {
            extensionsToEnable.add(XR_EXT_DEBUG_UTILS_EXTENSION_NAME)
        }

        val availableLayers = getAvailableOpenXrLayers(logger)
        val layersToEnable = mutableSetOf<String>()
        if (!initProps.mainParameters.forbidDebug && availableLayers.contains(XR_VALIDATION_LAYER_NAME)) {
            layersToEnable.add(XR_VALIDATION_LAYER_NAME)
        }

        val xrAppInfo = XrApplicationInfo.calloc(stack)
        xrAppInfo.applicationName(stack.UTF8("Dragons"))
        xrAppInfo.applicationVersion(1) // TODO Query application name and version from somewhere else
        xrAppInfo.apiVersion(XR_MAKE_VERSION(1, 0, 0))

        logger.info("The following ${extensionsToEnable.size} OpenXR extensions will be enabled:")
        val pExtensionsToEnable = stack.callocPointer(extensionsToEnable.size)
        for ((index, extension) in extensionsToEnable.withIndex()) {
            pExtensionsToEnable.put(index, stack.UTF8(extension))
            logger.info(extension)
        }

        logger.info("The following ${layersToEnable.size} OpenXR layers will be enabled:")
        val pLayersToEnable = stack.callocPointer(layersToEnable.size)
        for ((index, layer) in layersToEnable.withIndex()) {
            pLayersToEnable.put(index, stack.UTF8(layer))
            logger.info(layer)
        }

        val ciInstance = XrInstanceCreateInfo.calloc(stack)
        ciInstance.`type$Default`()
        ciInstance.applicationInfo(xrAppInfo)
        ciInstance.enabledExtensionNames(pExtensionsToEnable)
        ciInstance.enabledApiLayerNames(pLayersToEnable)

        val pInstance = stack.callocPointer(1)
        assertXrSuccess(xrCreateInstance(ciInstance, pInstance), "CreateInstance")
        XrInstance(pInstance[0], ciInstance)
    }
}
