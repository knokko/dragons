package dragons.vr.openxr

import dragons.util.getIntConstantName
import org.lwjgl.openxr.XR10
import org.lwjgl.openxr.XR10.XR_TIMEOUT_EXPIRED
import org.slf4j.LoggerFactory
import kotlin.jvm.Throws

@Throws(OpenXRFailureException::class)
internal fun assertXrSuccess(returnCode: Int, functionName: String, functionContext: String? = null) {

    // I don't consider timeout a success
    if (returnCode < 0 || returnCode == XR_TIMEOUT_EXPIRED) {
        val contextPart = if (functionContext != null) { "($functionContext) " } else { "" }
        val returnCodeName = if (returnCode == XR_TIMEOUT_EXPIRED) "XR_TIMEOUT" else getIntConstantName(XR10::class, returnCode, "XR_ERROR_")
        LoggerFactory.getLogger("VR").error("xr$functionName ${contextPart}returned $returnCode ($returnCodeName)")
        throw OpenXRFailureException("xr$functionName ${contextPart}returned $returnCode ($returnCodeName)")
    }
}

internal class OpenXRFailureException(message: String): Exception(message)
