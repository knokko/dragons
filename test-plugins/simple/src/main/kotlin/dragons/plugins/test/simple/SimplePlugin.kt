package dragons.plugins.test.simple

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.general.PluginsLoadedListener

class SimplePlugin: PluginsLoadedListener {
    private var ownTestCounter = 0

    override fun afterPluginsLoaded(instance: PluginInstance) {
        ownTestCounter += 1
        SimplePluginStore.testCounter = ownTestCounter
    }
}
