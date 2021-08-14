package dragons.plugin.interfaces.general

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.PluginInterface

interface ExitListener: PluginInterface {

    fun onExit(instance: PluginInstance)
}
