package dragons.vr.openxr

import dragons.vr.assertXrSuccess
import org.lwjgl.openxr.XR10.*
import org.lwjgl.openxr.XrInstance
import org.lwjgl.openxr.XrViewConfigurationView
import org.lwjgl.system.MemoryStack.stackPush
import kotlin.math.min

internal fun determineOpenXrSwapchainSize(
    xrInstance: XrInstance, xrSystemId: Long
): Pair<Int, Int> {
    return stackPush().use { stack ->
        val pNumViewConfigurations = stack.callocInt(1)
        assertXrSuccess(
            xrEnumerateViewConfigurations(xrInstance, xrSystemId, pNumViewConfigurations, null),
            "EnumerateViewConfigurations", "count"
        )
        val numViewConfigurations = pNumViewConfigurations[0]

        val pViewConfigurations = stack.callocInt(numViewConfigurations)
        assertXrSuccess(
            xrEnumerateViewConfigurations(xrInstance, xrSystemId, pNumViewConfigurations, pViewConfigurations),
            "EnumerateViewConfigurations", "configurations"
        )

        var hasStereo = false
        for (index in 0 until numViewConfigurations) {
            if (pViewConfigurations[index] == XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO) {
                hasStereo = true
            }
        }

        if (!hasStereo) {
            throw UnsupportedOperationException("XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO is required, but not supported")
        }

        val pNumViews = stack.callocInt(1)
        assertXrSuccess(
            xrEnumerateViewConfigurationViews(xrInstance, xrSystemId, XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO, pNumViews, null),
            "EnumerateViewConfigurationViews", "count"
        )
        val numViews = pNumViews[0]

        if (numViews != 2) {
            throw UnsupportedOperationException("Number of view configuration views ($numViews) must be 2")
        }

        val pViews = XrViewConfigurationView.calloc(numViews, stack)
        assertXrSuccess(
            xrEnumerateViewConfigurationViews(xrInstance, xrSystemId, XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO, pNumViews, pViews),
            "EnumerateViewConfigurationViews", "views"
        )

        val width = min(pViews[0].recommendedImageRectWidth(), pViews[1].recommendedImageRectWidth())
        val height = min(pViews[0].recommendedImageRectHeight(), pViews[1].recommendedImageRectHeight())
        Pair(width, height)
    }
}
