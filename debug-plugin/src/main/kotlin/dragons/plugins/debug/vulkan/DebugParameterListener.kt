package dragons.plugins.debug.vulkan

import dragons.init.MainParameters
import dragons.plugin.interfaces.general.MainParametersListener
import knokko.plugin.PluginInstance

@Suppress("unused")
class DebugParameterListener: MainParametersListener {

    override fun processMainParameters(instance: PluginInstance, mainParameters: MainParameters) {
        instance.state = mainParameters
    }
}
