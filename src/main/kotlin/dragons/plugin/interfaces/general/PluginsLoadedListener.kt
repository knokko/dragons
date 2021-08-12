package dragons.plugin.interfaces.general

import dragons.plugin.interfaces.PluginInterface

interface PluginsLoadedListener: PluginInterface {

    /**
     * This method will be called after all plug-ins have been loaded. This is the first event that any plug-in can
     * listen to.
     *
     * Plug-ins are encouraged to use this method to start asynchronous tasks that load resources that are expected to
     * be needed soon. Doing this should reduce the loading time of the game.
     */
    fun afterPluginsLoaded()
}
