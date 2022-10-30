package dragons.plugins.standard.menu

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.menu.MainMenuManager
import dragons.plugins.standard.state.StandardPluginState
import dragons.plugins.standard.vulkan.render.StandardSceneRenderer
import dragons.state.StaticGameState
import dragons.space.Angle
import dragons.space.Distance
import dragons.space.Position
import dragons.util.PerformanceStatistics
import dragons.vulkan.util.assertVkSuccess
import org.joml.Math.cos
import org.joml.Math.sin
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkSemaphoreCreateInfo
import kotlin.math.atan2

@Suppress("unused")
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

    override suspend fun controlMainMenu(pluginInstance: PluginInstance, gameState: StaticGameState) {

        fun createSemaphore(description: String): Long {
            return stackPush().use { stack ->
                val ciSemaphore = VkSemaphoreCreateInfo.calloc(stack)
                ciSemaphore.`sType$Default`()

                val pSemaphore = stack.callocLong(1)
                assertVkSuccess(
                    vkCreateSemaphore(gameState.graphics.vkDevice, ciSemaphore, null, pSemaphore),
                    "CreateSemaphore", "standard plug-in: $description"
                )
                pSemaphore[0]
            }
        }

        val pluginState = pluginInstance.state as StandardPluginState

        val sceneRenderer = StandardSceneRenderer(gameState, pluginState)

        val realm = createStandardMainMenuRealm()

        val renderFinishedSemaphore = createSemaphore("main menu rendering")
        val debugPanelSemaphore = createSemaphore("main menu debug panel")

        // TODO Add more iterations (and eventually loop indefinitely)
        var numIterationsLeft = 5

        var currentPosition = Position.meters(0, 0, 0)
        var extraRotation = 0f
        val nearPlane = Distance.milliMeters(10)
        val farPlane = Distance.meters(500)

        while (!gameState.vrManager.shouldStop()) {
            val eyeMatrices = gameState.vrManager.prepareRender(nearPlane, farPlane, Angle.degrees(extraRotation))
            val currentInput = gameState.vrManager.getDragonControls()

            if (eyeMatrices != null) {

                PerformanceStatistics.markFrame()

                val currentMovement = currentInput.walkDirection
                val currentDirection = eyeMatrices.averageViewMatrix.transformDirection(Vector3f(0f, 0f, 1f))
                val currentRotation = atan2(currentDirection.x, currentDirection.z)

                val dx = 0.1f * (cos(currentRotation) * currentMovement.x + sin(currentRotation) * currentMovement.y)
                var dy = 0f
                val dz = 0.1f * (sin(currentRotation) * currentMovement.x - cos(currentRotation) * currentMovement.y)

                extraRotation += currentInput.cameraTurnDirection * 2f

                if (currentInput.isGrabbingLeft) {
                    dy -= 0.1f
                }
                if (currentInput.isGrabbingRight) {
                    dy += 0.1f
                }

                currentPosition += Position.meters(dx, dy, dz)

                // Work around to let the user choose when to end the game
                if (currentMovement.length() > 0f) {
                    numIterationsLeft = -1
                }
                if (currentInput.shouldToggleMenu) {
                    numIterationsLeft = 1
                }

                val averageEyePosition = Position.meters(eyeMatrices.averageVirtualEyePosition) + currentPosition

                // TODO Render hands
                if (currentInput.leftHandPosition != null) {
                    val leftHandMatrix = Matrix4f().translation(currentInput.leftHandPosition)
                    if (currentInput.leftHandOrientation != null) {
                        leftHandMatrix.rotate(currentInput.leftHandOrientation)
                    }
                }
                sceneRenderer.render(realm, averageEyePosition, eyeMatrices.leftEyeMatrix, eyeMatrices.rightEyeMatrix)
                gameState.vrManager.markFirstFrameQueueSubmit()
                sceneRenderer.submit(realm, longArrayOf(), intArrayOf(), longArrayOf(renderFinishedSemaphore))
                gameState.vrManager.resolveAndSubmitFrames(
                    renderFinishedSemaphore, numIterationsLeft == 1
                )
            } else {
                println("Can't render main menu")
                gameState.vrManager.resolveAndSubmitFrames(
                    null, numIterationsLeft == 1
                )
            }

            numIterationsLeft -= 1
            if (numIterationsLeft == 0) {
                gameState.vrManager.requestStop()
            }
        }

        sceneRenderer.destroy(gameState.graphics.vkDevice)

        vkDestroySemaphore(gameState.graphics.vkDevice, renderFinishedSemaphore, null)
        vkDestroySemaphore(gameState.graphics.vkDevice, debugPanelSemaphore, null)
    }
}
