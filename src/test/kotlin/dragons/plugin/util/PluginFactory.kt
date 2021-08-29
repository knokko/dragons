package dragons.plugin.util

import dragons.init.GameInitProperties
import dragons.init.MainParameters
import dragons.plugin.PluginInfo
import dragons.plugin.PluginInstance

fun createDummyPluginInstance(name: String) = PluginInstance(
    info = PluginInfo(name),
    gameInitProps = GameInitProperties(MainParameters(arrayOf()), isInDevelopment = true)
)
