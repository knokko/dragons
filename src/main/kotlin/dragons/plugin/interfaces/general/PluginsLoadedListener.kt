package dragons.plugin.interfaces.general

import knokko.plugin.MagicPluginInterface
import knokko.plugin.PluginInstance

interface PluginsLoadedListener: MagicPluginInterface {

    /**
     * This method will be called after all plug-ins have been loaded. This is the first event that any plug-in can
     * listen to.
     *
     * Plug-ins are encouraged to use this method to start time-consuming asynchronous tasks whose results are expected
     * to be needed soon. Doing this should reduce the loading time of the game.
     */
    fun afterPluginsLoaded(instance: PluginInstance)
}
