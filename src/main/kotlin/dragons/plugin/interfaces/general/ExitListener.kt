package dragons.plugin.interfaces.general

import knokko.plugin.MagicPluginInterface
import knokko.plugin.PluginInstance

interface ExitListener: MagicPluginInterface {

    fun onExit(instance: PluginInstance)
}
