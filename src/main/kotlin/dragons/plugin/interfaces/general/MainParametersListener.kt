package dragons.plugin.interfaces.general

import dragons.init.MainParameters
import knokko.plugin.MagicPluginInterface
import knokko.plugin.PluginInstance

interface MainParametersListener: MagicPluginInterface {

    fun processMainParameters(instance: PluginInstance, mainParameters: MainParameters)
}
