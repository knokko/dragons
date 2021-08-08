package dragons.init

import dragons.init.trouble.StartupException
import dragons.init.trouble.gui.showStartupTroubleWindow
import dragons.init.vr.destroyVr
import dragons.init.vr.initVr
import dragons.plugin.PluginManager
import dragons.plugin.interfaces.general.PluginsLoadedListener
import dragons.plugin.loading.PluginClassLoader
import dragons.plugin.loading.scanDefaultPluginLocations
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() {
    try {
        prepareMainMenu()

        destroyVr()
        // TODO Disable plug-ins
    } catch (startupProblem: StartupException) {
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
fun prepareMainMenu() {
    /*
     * At the moment, initializing VR can't be influenced by plug-ins and can therefore be done in parallel with loading
     * the plug-ins.
     */
    runBlocking {
        launch {
            println("Initialize VR...")
            initVr()
            println("Initialized VR")
        }
        launch {
            println("Scan plug-in locations...")
            val combinedPluginContent = scanDefaultPluginLocations()
            println("Found ${combinedPluginContent.classByteMap.size} plug-in classes and ${combinedPluginContent.resourceByteMap.size} resources")
            val pluginClassLoader = PluginClassLoader(combinedPluginContent)
            println("Found ${pluginClassLoader.magicInstances.size} magic plug-in classes")
            val pluginManager = PluginManager(pluginClassLoader.magicInstances)
            pluginManager.getImplementations(PluginsLoadedListener::class.java).forEach(PluginsLoadedListener::afterPluginsLoaded)
        }
    }
}
