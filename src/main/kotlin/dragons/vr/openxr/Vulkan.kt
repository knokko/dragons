package dragons.vr.openxr

import dragons.init.trouble.SimpleStartupException
import dragons.state.StaticGraphicsState
import org.lwjgl.openxr.KHRVulkanEnable2
import org.lwjgl.openxr.KHRVulkanEnable2.xrGetVulkanGraphicsRequirements2KHR
import org.lwjgl.openxr.XR10.XR_VERSION_MAJOR
import org.lwjgl.openxr.XR10.XR_VERSION_MINOR
import org.lwjgl.openxr.XrGraphicsRequirementsVulkan2KHR
import org.lwjgl.openxr.XrInstance
import org.lwjgl.system.MemoryStack.stackPush

fun checkOpenXrVulkanVersion(xrInstance: XrInstance, xrSystemId: Long) {
    stackPush().use { stack ->
        val xrRequirements = XrGraphicsRequirementsVulkan2KHR.calloc(stack)
        xrRequirements.`type$Default`()
        assertXrSuccess(
            xrGetVulkanGraphicsRequirements2KHR(xrInstance, xrSystemId, xrRequirements),
            "GetVulkanGraphicsRequirements2KHR"
        )

        val minMajorVersion = XR_VERSION_MAJOR(xrRequirements.minApiVersionSupported()).toInt()
        val minMinorVersion = XR_VERSION_MINOR(xrRequirements.minApiVersionSupported()).toInt()

        if (minMajorVersion > 1 || (minMajorVersion == 1 && minMinorVersion > 2)) {
            throw SimpleStartupException(
                "Can't agree on Vulkan version",
                listOf(
                    "The OpenXR runtime only supports Vulkan $minMajorVersion.$minMinorVersion and later,",
                    "but this game only supports Vulkan 1.0"
                )
            )
        }
    }
}
