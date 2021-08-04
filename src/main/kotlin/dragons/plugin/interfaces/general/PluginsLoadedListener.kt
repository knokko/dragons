package dragons.plugin.interfaces.general

import dragons.plugin.interfaces.PluginInterface

interface PluginsLoadedListener: PluginInterface {

    /**
     * This method will be called after all plug-ins have been loaded
     */
    fun afterPluginsLoaded()
}
