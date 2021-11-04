package dragons.plugins.standard.menu

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.menu.MainMenuManager
import dragons.state.StaticGameState

class StandardMainMenu: MainMenuManager {
    override fun requestMainMenuControl(pluginInstance: PluginInstance, agent: MainMenuManager.Agent) {
        /*
         * The standard plug-in uses the lowest priority because it wouldn't make sense if any other plug-in had a lower
         * priority (since the standard plug-in is normally always present, the main menu of that alternative plug-in
         * would never have the highest priority and thus never be used).
         *
         * Thus, the standard main menu will only be used if no other plug-in wants to claim the main menu.
         */
        agent.priority = 0u
    }

    override suspend fun controlMainMenu(gameState: StaticGameState) {
        var numIterationsLeft = 10

        while (numIterationsLeft > 0) {
            val eyeMatrices = gameState.vrManager.prepareRender()
            if (eyeMatrices != null) {
                val (leftEyeMatrix, rightEyeMatrix) = eyeMatrices
            } else {
                // TODO Uh ooh
            }

            gameState.graphics.resolveHelper.resolve(gameState.graphics.vkDevice, gameState.graphics.queueManager, ehm)
            gameState.vrManager.submitFrames()

            numIterationsLeft--
        }
    }
}
