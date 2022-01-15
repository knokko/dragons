package dragons.vr.openxr

import dragons.init.GameInitProperties
import dragons.init.trouble.SimpleStartupException
import dragons.vr.assertXrSuccess
import org.lwjgl.openxr.XR10
import org.lwjgl.openxr.XrInstance
import org.lwjgl.openxr.XrSystemGetInfo
import org.lwjgl.system.MemoryStack.stackPush
import org.slf4j.Logger

fun createOpenXrSystem(initProps: GameInitProperties, logger: Logger, xrInstance: XrInstance): Long? {
    return stackPush().use { stack ->
        val giSystem = XrSystemGetInfo.calloc(stack)
        giSystem.`type$Default`()
        giSystem.formFactor(XR10.XR_FORM_FACTOR_HEAD_MOUNTED_DISPLAY)

        val pSystemId = stack.callocLong(1)
        val systemResult = XR10.xrGetSystem(xrInstance, giSystem, pSystemId)
        if (systemResult == XR10.XR_ERROR_FORM_FACTOR_UNAVAILABLE) {
            logger.error("xrGetSystem returned XR_ERROR_FORM_FACTOR_UNAVAILABLE")
            if (initProps.mainParameters.requiresHmd) {
                throw SimpleStartupException(
                    "Can't find HMD", listOf(
                        "It looks like no HMD (VR helmet) is connected to your computer.",
                        "Please restart this game after plugging it in.",
                        "You can close this window."
                    )
                )
            }
            return null
        } else {
            assertXrSuccess(systemResult, "GetSystem")
        }

        pSystemId[0]
    }
}
