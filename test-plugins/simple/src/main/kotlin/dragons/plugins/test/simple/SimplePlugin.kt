package dragons.plugins.test.simple

import dragons.plugin.interfaces.general.PluginsLoadedListener

class SimplePlugin: PluginsLoadedListener {
    private var ownTestCounter = 0

    override fun afterPluginsLoaded() {
        ownTestCounter += 1
        SimplePluginStore.testCounter = ownTestCounter
    }
}
