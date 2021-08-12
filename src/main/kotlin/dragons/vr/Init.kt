package dragons.vr

import dragons.init.MainParameters
import dragons.init.trouble.SimpleStartupException
import dragons.init.trouble.StartupException
import dragons.util.getIntConstantName
import org.lwjgl.openvr.OpenVR
import org.lwjgl.openvr.VR
import org.lwjgl.openvr.VR.*
import org.lwjgl.system.MemoryStack.stackPush
import org.slf4j.LoggerFactory.getLogger

@Throws(StartupException::class)
fun initVr(mainParameters: MainParameters): VrManager {
    val logger = getLogger("VR")
    if (!VR_IsRuntimeInstalled()) {
        logger.warn("No OpenVR runtime found")
        if (mainParameters.requiresHmd) {
            throw SimpleStartupException(
                "No OpenVR runtime found", listOf(
                    "It looks like the OpenVR (SteamVR) runtime is not installed on your computer.",
                    "Please restart this game after installing it.",
                    "You can close this window."
                )
            )
        } else {
            logger.info("Using a dummy VR manager")
            return DummyVrManager()
        }
    }

    logger.info("The OpenVR runtime path is ${VR_RuntimePath()}")

    if (!VR_IsHmdPresent()) {
        logger.warn("Can't find HMD")
        if (mainParameters.requiresHmd) {
            throw SimpleStartupException(
                "Can't find HMD", listOf(
                    "It looks like no HMD (VR helmet) is connected to your computer.",
                    "Please restart this game after plugging it in.",
                    "You can close this window."
                )
            )
        } else {
            logger.info("Using a dummy VR manager")
            return DummyVrManager()
        }
    }

    stackPush().use { stack ->
        val pVrError = stack.callocInt(1)
        val token = VR_InitInternal(pVrError, EVRApplicationType_VRApplication_Scene)

        val vrError = pVrError[0]
        if (vrError != 0) {
            logger.warn("VR_InitInternal returned $vrError")
            val vrErrorName = VR_GetVRInitErrorAsSymbol(vrError)
            if (mainParameters.requiresHmd) {
                throw SimpleStartupException(
                    "Failed to initialize OpenVR", listOf(
                        "VR_InitInternal returned error code $vrError ($vrErrorName).",
                        "You should restart the game after fixing this.",
                        "You can close this window."
                    )
                )
            } else {
                logger.info("Using a dummy VR manager")
                return DummyVrManager()
            }
        }

        OpenVR.create(token)
        logger.info("Initialized OpenVR with token $token")
    }

    return OpenVrManager()
}
