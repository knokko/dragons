package dragons.plugins.test.twin

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.general.PluginsLoadedListener

class TwinPluginB: PluginsLoadedListener {
    override fun afterPluginsLoaded(instance: PluginInstance) {
        TwinStore.testString = getTestString()
    }
}
