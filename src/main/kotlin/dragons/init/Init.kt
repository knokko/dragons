package dragons.init

import dragons.init.trouble.StartupException
import dragons.init.trouble.gui.showStartupTroubleWindow
import dragons.plugin.PluginManager
import dragons.plugin.interfaces.general.ExitListener
import dragons.plugin.interfaces.general.PluginsLoadedListener
import dragons.plugin.loading.PluginClassLoader
import dragons.plugin.loading.scanDefaultPluginLocations
import dragons.vr.VrManager
import dragons.vr.initVr
import dragons.vulkan.destroy.destroyVulkanInstance
import dragons.vulkan.init.initVulkanInstance
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.lwjgl.vulkan.VkInstance
import org.slf4j.Logger.ROOT_LOGGER_NAME
import org.slf4j.LoggerFactory.getLogger
import java.io.File

fun main(args: Array<String>) {
    try {
        // Clear the old logs each time the game is started
        File("logs").deleteRecursively()

        val logger = getLogger(ROOT_LOGGER_NAME)
        logger.info("Running with main arguments ${args.contentToString()}")
        val mainParameters = MainParameters(args)

        val initProps = GameInitProperties(mainParameters)

        val prepareMainMenuResult = prepareMainMenu(initProps)
        logger.info("Finished preparing the main menu")

        logger.info("Start with shutting down the game")
        destroyVulkanInstance(prepareMainMenuResult.vkInstance, prepareMainMenuResult.pluginManager)
        prepareMainMenuResult.vrManager.destroy()
        prepareMainMenuResult.pluginManager.getImplementations(ExitListener::class).forEach {
                listenerPair -> listenerPair.first.onExit(listenerPair.second)
        }
        logger.info("Finished shutting down the game")
    } catch (startupProblem: StartupException) {
        getLogger(ROOT_LOGGER_NAME).error("Failed to start", startupProblem)
        showStartupTroubleWindow(startupProblem)
    }
}

/**
 * This method does the preparation that needs to be done before the main menu can be shown and should thus be called
 * at the start of the application.
 *
 * This preparation consists of loading all plug-ins and creating some rendering resources.
 */
@Throws(StartupException::class)
fun prepareMainMenu(initProps: GameInitProperties): PrepareMainMenuResult {
    return runBlocking {
        val vrJob = async { initVr(initProps) }
        val pluginJob = async {
            val logger = getLogger(ROOT_LOGGER_NAME)
            logger.info("Scan plug-in locations...")
            val combinedPluginContent = scanDefaultPluginLocations(initProps)
            logger.info("Found ${combinedPluginContent.size} plug-ins:")
            for (pluginPair in combinedPluginContent) {
                logger.info(pluginPair.second.info.name)
            }
            val pluginClassLoader = PluginClassLoader(combinedPluginContent)
            logger.info("Found ${pluginClassLoader.magicInstances.size} magic plug-in classes")
            val pluginManager = PluginManager(pluginClassLoader.magicInstances)
            pluginManager.getImplementations(PluginsLoadedListener::class.java).forEach {
                    listenerPair -> listenerPair.first.afterPluginsLoaded(listenerPair.second)
            }
            pluginManager
        }

        val pluginManager = pluginJob.await()
        val vrManager = vrJob.await()

        val vulkanInstanceJob = async {
            initVulkanInstance(pluginManager, vrManager)
        }

        val vkInstance = vulkanInstanceJob.await()

        PrepareMainMenuResult(pluginManager, vrManager, vkInstance)
    }
}

class PrepareMainMenuResult(val pluginManager: PluginManager, val vrManager: VrManager, val vkInstance: VkInstance)
