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
import dragons.vulkan.destroy.destroyVulkanDevice
import dragons.vulkan.destroy.destroyVulkanInstance
import dragons.vulkan.init.choosePhysicalDevice
import dragons.vulkan.init.createLogicalDevice
import dragons.vulkan.init.initVulkanInstance
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkPhysicalDevice
import org.slf4j.Logger
import org.slf4j.Logger.ROOT_LOGGER_NAME
import org.slf4j.LoggerFactory.getLogger
import java.io.File
import java.nio.file.Files
import kotlin.io.path.absolutePathString

fun main(args: Array<String>) {
    try {
        // Clear the old logs each time the game is started
        File("logs").deleteRecursively()

        val logger = getLogger(ROOT_LOGGER_NAME)
        logger.info("Running with main arguments ${args.contentToString()}")
        val mainParameters = MainParameters(args)

        val isInDevelopment = File("gradlew.bat").exists()

        if (isInDevelopment) {
            if (!ensurePluginsAreBuilt(logger)) {
                return
            }
        }

        val initProps = GameInitProperties(mainParameters, isInDevelopment)

        val prepareMainMenuResult = prepareMainMenu(initProps)
        logger.info("Finished preparing the main menu")

        logger.info("Start with shutting down the game")
        destroyVulkanDevice(
            prepareMainMenuResult.vkInstance, prepareMainMenuResult.vkPhysicalDevice, prepareMainMenuResult.vkDevice,
            prepareMainMenuResult.pluginManager
        )
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

fun ensurePluginsAreBuilt(logger: Logger): Boolean {
    logger.warn("Running in development, so the plug-ins need to be built...")

    // TODO Add Linux support
    val buildPluginCommand = "./gradlew.bat build -x test"
    val buildPluginScript = Files.createTempFile("", ".bat")
    Files.writeString(buildPluginScript, buildPluginCommand)

    val buildPluginsBuilder = ProcessBuilder(buildPluginScript.absolutePathString())
    buildPluginsBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
    val buildPluginsProcess = buildPluginsBuilder.start()
    val exitCode = buildPluginsProcess.waitFor()

    if (exitCode != 0) {
        logger.error("Failed to build plug-ins (exit code $exitCode). The game will terminate")
        return false
    }
    logger.warn("Plug-ins were built successfully")

    return true
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

        val vulkanInstanceJob = async {
            initVulkanInstance(pluginJob.await(), vrJob.await())
        }

        val vkPhysicalDeviceJob = async {
            choosePhysicalDevice(vulkanInstanceJob.await(), pluginJob.await(), vrJob.await())
        }

        val vkDeviceJob = async {
            createLogicalDevice(vulkanInstanceJob.await(), vkPhysicalDeviceJob.await(), pluginJob.await(), vrJob.await())
        }

        PrepareMainMenuResult(
            pluginJob.await(), vrJob.await(),
            vulkanInstanceJob.await(), vkPhysicalDeviceJob.await(), vkDeviceJob.await()
        )
    }
}

class PrepareMainMenuResult(
    val pluginManager: PluginManager, val vrManager: VrManager,
    val vkInstance: VkInstance,
    val vkPhysicalDevice: VkPhysicalDevice, val vkDevice: VkDevice
)
