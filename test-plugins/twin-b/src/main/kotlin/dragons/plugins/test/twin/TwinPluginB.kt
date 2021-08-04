package dragons.plugins.test.twin

import dragons.plugin.interfaces.general.PluginsLoadedListener

class TwinPluginB: PluginsLoadedListener {
    override fun afterPluginsLoaded() {
        TwinStore.testString = getTestString()
    }
}
