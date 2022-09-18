package dragons.plugins.standard.init

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.general.PluginsLoadedListener
import dragons.plugins.standard.state.StandardPluginState

@Suppress("unused")
class StandardPluginInitialization: PluginsLoadedListener {

    override fun afterPluginsLoaded(instance: PluginInstance) {
        instance.state = StandardPluginState()
        // TODO I might want to start loading some resources at this point
    }
}
