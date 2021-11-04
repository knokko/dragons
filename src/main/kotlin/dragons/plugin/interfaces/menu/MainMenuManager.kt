package dragons.plugin.interfaces.menu

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.PluginInterface
import dragons.state.StaticGameState
import kotlinx.coroutines.job

interface MainMenuManager: PluginInterface {

    fun requestMainMenuControl(pluginInstance: PluginInstance, agent: Agent)

    class Agent(
        var priority: UInt? = null
    )

    // TODO Use return value to indicate the next game state
    suspend fun controlMainMenu(gameState: StaticGameState)
}
