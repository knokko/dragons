package dragons.init

import dragons.init.trouble.SimpleStartupException
import dragons.init.trouble.StartupException
import dragons.init.trouble.gui.showStartupTroubleWindow
import dragons.plugin.PluginManager
import dragons.plugin.interfaces.general.ExitListener
import dragons.plugin.interfaces.general.PluginsLoadedListener
import dragons.plugin.interfaces.general.StaticStateListener
import dragons.plugin.interfaces.menu.MainMenuManager
import dragons.plugin.loading.PluginClassLoader
import dragons.plugin.loading.scanDefaultPluginLocations
import dragons.state.StaticGameState
import dragons.state.StaticGraphicsState
import dragons.vr.ResolveHelper
import dragons.vr.initVr
import dragons.vulkan.destroy.destroyVulkanDevice
import dragons.vulkan.destroy.destroyVulkanInstance
import dragons.vulkan.init.choosePhysicalDevice
import dragons.vulkan.init.createLogicalDevice
import dragons.vulkan.init.initVulkanInstance
import dragons.vulkan.memory.MemoryInfo
import dragons.vulkan.memory.allocateStaticMemory
import kotlinx.coroutines.*
import org.lwjgl.system.Platform
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

        val (staticGameState, priorityProblem) = runBlocking {
            val staticGameState = prepareStaticGameState(initProps, this)
            logger.info("Finished preparing the static game state")

            val mainMenuCandidates = staticGameState.pluginManager.getImplementations(MainMenuManager::class).map { (manager, instance) ->
                val agent = MainMenuManager.Agent()
                manager.requestMainMenuControl(instance, agent)
                Triple(instance, manager, agent.priority)
            }.filter { (_, _, priority) -> priority != null }
            logger.info("The following plug-ins requested to control the main menu:")
            for ((instance, _, priority) in mainMenuCandidates) {
                logger.info("Plug-in ${instance.info.name} has priority $priority")
            }

            val mainMenuWinner1 = mainMenuCandidates.maxByOrNull { (_, _, priority) -> priority!! }
            if (mainMenuWinner1 == null) {
                logger.error("Not a single plug-in requested to control the main menu. Aborting...")
                return@runBlocking Pair(staticGameState, SimpleStartupException(
                    "No main menu",
                    listOf(
                        "Not a single plug-in is able to control the main menu, so the game can't start.",
                        "This can only happen if the standard plug-in is disabled without alternative."
                    )
                ))
            }

            // To avoid non-determinism, a tie in main menu priority causes start-up to fail
            val mainMenuWinner2 = mainMenuCandidates.reversed().maxByOrNull { (_, _, priority) -> priority!! }!!
            if (mainMenuWinner1 !== mainMenuWinner2) {
                val name1 = mainMenuWinner1.first.info.name
                val name2 = mainMenuWinner2.first.info.name
                val priority = mainMenuWinner1.third!!
                logger.error("The $name1 plug-in and the $name2 plug-in share the highest main menu priority ($priority). Aborting...")
                return@runBlocking Pair(staticGameState, SimpleStartupException(
                    "Plug-in conflict",
                    listOf(
                        "The $name1 plug-in and the $name2 plug-in share the highest priority to control the main menu ($priority).",
                        "But, only 1 of them could be chosen.",
                        "Since no fair choice can be made, the game can't start."
                    )
                ))
            }
            logger.info("The ${mainMenuWinner1.first.info.name} plug-in gets control of the main menu")

            val nextGameState = mainMenuWinner1.second.controlMainMenu(mainMenuWinner1.first, staticGameState)
            // TODO Handle the next game state

            Pair(staticGameState, null)
        }

        logger.info("Start with shutting down the game")
        val staticGraphics = staticGameState.graphics
        staticGraphics.memoryScope.destroy(staticGraphics.vkDevice)
        staticGraphics.resolveHelper.destroy(staticGraphics.vkDevice)
        destroyVulkanDevice(
            staticGraphics.vkInstance, staticGraphics.vkPhysicalDevice, staticGraphics.vkDevice,
            staticGameState.pluginManager
        )
        destroyVulkanInstance(staticGraphics.vkInstance, staticGameState.pluginManager)
        staticGameState.vrManager.destroy()
        staticGameState.pluginManager.getImplementations(ExitListener::class).forEach {
                listenerPair -> listenerPair.first.onExit(listenerPair.second)
        }
        logger.info("Finished shutting down the game")

        // The user should be informed about the conflict
        if (priorityProblem != null) {
            throw priorityProblem
        }
    } catch (startupProblem: StartupException) {
        getLogger(ROOT_LOGGER_NAME).error("Failed to start", startupProblem)
        showStartupTroubleWindow(startupProblem)
    }
}

fun ensurePluginsAreBuilt(logger: Logger): Boolean {
    logger.warn("Running in development, so the plug-ins need to be built...")

    val exitCode = if (Platform.get() == Platform.WINDOWS) {
        val buildPluginCommand = "./gradlew.bat build -x test"
        val buildPluginScript = Files.createTempFile("", ".bat")
        Files.writeString(buildPluginScript, buildPluginCommand)

        val buildPluginsBuilder = ProcessBuilder(buildPluginScript.absolutePathString())
        buildPluginsBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
        val buildPluginsProcess = buildPluginsBuilder.start()
        buildPluginsProcess.waitFor()
    } else {
        val buildPluginsProcess = Runtime.getRuntime().exec("./gradlew build -x test")
        buildPluginsProcess.waitFor()
    }

    if (exitCode != 0) {
        logger.error("Failed to build plug-ins (exit code $exitCode). The game will terminate")
        return false
    }
    logger.warn("Plug-ins were built successfully")

    return true
}

/**
 * This method constructs the part of the game state that should persist throughout the entire game session. This game
 * state consists mostly of Vulkan resources (crucial stuff like the Instance as well as some important GPU memory
 * allocations). This method will also load all plug-ins and fire some plug-in events (especially Vulkan-related events).
 *
 * This method should be called soon after the game is started. When this method returns, the main menu should be
 * created and opened.
 */
@Throws(StartupException::class)
fun prepareStaticGameState(initProps: GameInitProperties, staticCoroutineScope: CoroutineScope): StaticGameState {
    // Use IO dispatcher because the workload is expected to be a mixture of computations, IO, and blocking native calls
    return runBlocking(Dispatchers.IO) {

        val prepareScope = this
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
            Pair(pluginManager, pluginClassLoader)
        }

        val (pluginManager, pluginClassLoader) = pluginJob.await()
        val vrManager = vrJob.await()

        val vulkanInstanceJob = async {
            initVulkanInstance(pluginManager, vrManager)
        }

        val vulkanInstance = vulkanInstanceJob.await()
        val vkPhysicalDeviceJob = async {
            choosePhysicalDevice(vulkanInstance, pluginManager, vrManager)
        }

        val vulkanPhysicalDevice = vkPhysicalDeviceJob.await()

        val memoryInfoJob = async { MemoryInfo(vulkanPhysicalDevice) }

        val vkDeviceJob = async {
            createLogicalDevice(
                vulkanInstance, vulkanPhysicalDevice, pluginManager, vrManager, prepareScope
            )
        }

        val (vulkanDevice, queueManager, renderImageInfo) = vkDeviceJob.await()
        val memoryInfo = memoryInfoJob.await()

        val staticMemoryJob = async {
            allocateStaticMemory(
                vulkanDevice, queueManager, pluginManager, pluginClassLoader,
                vrManager, memoryInfo, renderImageInfo, prepareScope
            )
        }

        val (staticMemoryScope, coreStaticMemory) = staticMemoryJob.await()

        val resolveHelper = ResolveHelper(
            leftSourceImage = coreStaticMemory.leftColorImage,
            rightSourceImage = coreStaticMemory.rightColorImage,
            leftResolvedImage = coreStaticMemory.leftResolveImage,
            rightResolvedImage = coreStaticMemory.rightResolveImage,
            screenshotStagingBuffer = coreStaticMemory.screenshotBuffer.second,
            screenshotHostBuffer = coreStaticMemory.screenshotBuffer.first,
            vkDevice = vulkanDevice,
            queueManager = queueManager,
            renderImageInfo = renderImageInfo
        )

        val staticGameState = StaticGameState(
            graphics = StaticGraphicsState(
                vulkanInstance,
                vulkanPhysicalDevice,
                vulkanDevice,
                queueManager,
                renderImageInfo,
                resolveHelper,
                staticMemoryScope,
                coreStaticMemory
            ),
            initProperties = initProps,
            pluginManager = pluginManager,
            vrManager = vrManager,
            classLoader = pluginClassLoader,
            coroutineScope = staticCoroutineScope
        )

        for ((stateListener, pluginInstance) in pluginManager.getImplementations(StaticStateListener::class)) {
            val agent = StaticStateListener.Agent(staticGameState)
            stateListener.afterStaticStateCreation(pluginInstance, agent)
        }

        staticGameState
    }
}
