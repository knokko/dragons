package dragons.init.vr

import dragons.init.trouble.SimpleStartupException
import dragons.init.trouble.StartupException
import org.lwjgl.openvr.OpenVR
import org.lwjgl.openvr.VR.*
import org.lwjgl.system.MemoryStack.stackPush

@Throws(StartupException::class)
fun initVr() {
    if (!VR_IsRuntimeInstalled()) {
        throw SimpleStartupException("No OpenVR runtime found", listOf(
            "It looks like the OpenVR (SteamVR) runtime is not installed on your computer.",
            "Please restart this game after installing it.",
            "You can close this window."
        ))
    }

    if (!VR_IsHmdPresent()) {
        throw SimpleStartupException("Can't find HMD", listOf(
            "It looks like no HMD (VR helmet) is connected to your computer.",
            "Please restart this game after plugging it in.",
            "You can close this window."
        ))
    }

    stackPush().use { stack ->
        val pVrError = stack.callocInt(1)
        val token = VR_InitInternal(pVrError, EVRApplicationType_VRApplication_Scene)

        val vrError = pVrError[0]
        if (vrError != 0) {
            // TODO Convert error code to a meaningful string and throw a StartupException
        }

        OpenVR.create(token)
    }
}

fun destroyVr() {
    VR_ShutdownInternal()
}
