package dragons.plugin.interfaces.menu

import dragons.state.StaticGameState
import knokko.plugin.MagicPluginInterface
import knokko.plugin.PluginInstance

interface MainMenuManager: MagicPluginInterface {

    fun requestMainMenuControl(pluginInstance: PluginInstance, agent: Agent)

    class Agent(
        var priority: UInt? = null
    )

    // TODO Use return value to indicate the next game state
    suspend fun controlMainMenu(pluginInstance: PluginInstance, gameState: StaticGameState)
}
