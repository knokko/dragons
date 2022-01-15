package dragons.vr

import dragons.init.GameInitProperties
import dragons.init.trouble.SimpleStartupException
import dragons.init.trouble.StartupException
import org.lwjgl.openvr.OpenVR
import org.lwjgl.openvr.VR.*
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.Platform
import org.slf4j.LoggerFactory.getLogger

@Throws(StartupException::class)
fun initVr(initProps: GameInitProperties): VrManager {
    val logger = getLogger("VR")

    return if (initProps.mainParameters.useOpenVR) {
        logger.info("Trying to initialize OpenVR...")
        tryInitOpenVR(initProps, logger)
    } else {
        logger.info("Trying to initialize OpenXR...")
        tryInitOpenXR(initProps, logger)
    }
}
