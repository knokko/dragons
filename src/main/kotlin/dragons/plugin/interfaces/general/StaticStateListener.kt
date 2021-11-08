package dragons.plugin.interfaces.general

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.PluginInterface
import dragons.state.StaticGameState

interface StaticStateListener: PluginInterface {

    /**
     * This method will be called after the static game state of the game has been constructed, which should happen
     * right before the main menu is created. This part of the game state should remain valid for the rest of this
     * game session.
     */
    fun afterStaticStateCreation(pluginInstance: PluginInstance, agent: Agent)

    class Agent(
        val gameState: StaticGameState
    )
}
