package dragons.vr

import dragons.init.GameInitProperties
import dragons.init.trouble.StartupException
import org.slf4j.LoggerFactory.getLogger

@Throws(StartupException::class)
fun initVr(initProps: GameInitProperties): VrManager {
    val logger = getLogger("VR")

    logger.info("Trying to initialize OpenXR...")
    return tryInitOpenXR(initProps, logger)
}
