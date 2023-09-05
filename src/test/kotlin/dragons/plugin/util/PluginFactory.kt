package dragons.plugin.util

import knokko.plugin.PluginInfo
import knokko.plugin.PluginInstance

fun createDummyPluginInstance(name: String) = PluginInstance(
    info = PluginInfo(name),
)
