package dragons.plugins.standard.menu

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.menu.MainMenuManager
import dragons.plugins.standard.state.StandardPluginState
import dragons.plugins.standard.vulkan.render.StandardSceneRenderer
import dragons.plugins.standard.world.entity.MainMenuPlayerEntity
import dragons.state.StaticGameState
import dragons.geometry.Angle
import dragons.geometry.Distance
import dragons.geometry.Position
import dragons.geometry.Vector
import dragons.util.PerformanceStatistics
import dragons.util.printVector
import org.joml.Math.cos
import org.joml.Math.sin
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkSemaphoreCreateInfo
import troll.exceptions.VulkanFailureException.assertVkSuccess
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
                    vkCreateSemaphore(gameState.graphics.troll.vkDevice(), ciSemaphore, null, pSemaphore),
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
        var numIterationsLeft = 500

        var currentPosition = Position.meters(0, 0, 0)
        var extraRotation = Angle.degrees(0f)
        val nearPlane = Distance.milliMeters(10)
        val farPlane = Distance.meters(500)

        val playerEntity = realm.addEntity(MainMenuPlayerEntity(), MainMenuPlayerEntity.State(currentPosition))

        while (!gameState.vrManager.shouldStop()) {
            val eyeMatrices = gameState.vrManager.prepareRender(nearPlane, farPlane, extraRotation)
            val currentInput = gameState.vrManager.getDragonControls()

            if (eyeMatrices != null) {

                PerformanceStatistics.markFrame()

                val currentMovement = currentInput.walkDirection
                val currentDirection = eyeMatrices.averageViewMatrix.transformDirection(Vector3f(0f, 0f, 1f))
                val currentRotation = atan2(currentDirection.x, currentDirection.z)

                val dx = 0.1f * (cos(currentRotation) * currentMovement.x + sin(currentRotation) * currentMovement.y)
                var dy = 0f
                val dz = 0.1f * (sin(currentRotation) * currentMovement.x - cos(currentRotation) * currentMovement.y)

                extraRotation += Angle.degrees(currentInput.cameraTurnDirection * 2f)

                if (currentInput.isGrabbingLeft) {
                    dy -= 0.1f
                }
                if (currentInput.isGrabbingRight) {
                    dy += 0.1f
                }

                currentPosition += Vector.meters(dx, dy, dz)

                // Work around to let the user choose when to end the game
                if (currentMovement.length() > 0f) {
                    numIterationsLeft = -1
                }
                if (currentInput.shouldToggleMenu) {
                    numIterationsLeft = 1
                }

                val averageEyePosition = Vector.meters(eyeMatrices.averageVirtualEyePosition) + currentPosition

                val currentState = playerEntity.copyState() as MainMenuPlayerEntity.State
                currentState.position = currentPosition
                currentState.regularRotation = Angle.radians(currentRotation)

                fun updatePose(position: Vector3f?, orientation: Quaternionf?, setMatrix: (Matrix4f) -> Unit) {
                    if (position != null && orientation != null) {
                        setMatrix(Matrix4f().rotateY(-extraRotation.radians).translate(
                            eyeMatrices.averageRealEyePosition.negate(Vector3f())
                        ).translate(position).rotate(orientation))
                    }
                }

                updatePose(currentInput.leftHandPosition, currentInput.leftHandOrientation) {
                    currentState.leftHandMatrix = it
                }
                updatePose(currentInput.rightHandPosition, currentInput.rightHandOrientation) {
                    currentState.rightHandMatrix = it
                }
                updatePose(currentInput.leftHandAimPosition, currentInput.leftHandAimOrientation) {
                    currentState.leftHandAimMatrix = it
                }
                updatePose(currentInput.rightHandAimPosition, currentInput.rightHandAimOrientation) {
                    currentState.rightHandAimMatrix = it
                }

                playerEntity.setState(currentState)

                val rightHandAimMatrix = currentState.rightHandAimMatrix
                if (rightHandAimMatrix != null) {
                    val rawRayStart = rightHandAimMatrix.translate(0f, 0f, -0.1f, Matrix4f()).getTranslation(Vector3f())
                    val rawPointOnRay = rightHandAimMatrix.translate(0f, 0f, -1f, Matrix4f()).getTranslation(Vector3f())
                    val rayDirection = rawPointOnRay.sub(rawRayStart, Vector3f())
                    val rayStart = currentState.position + Vector.meters(rawRayStart) + Vector.meters(eyeMatrices.averageVirtualEyePosition)
                    val rayHit = realm.raytrace(rayStart, rayDirection, Distance.meters(150), playerEntity.id)
                    if (rayHit != null) {
                        println("Ray hit at ${rayHit.second} and player position is ${currentState.position}")
                        println("And rayStart is $rayStart and real eye position is ${printVector(eyeMatrices.averageRealEyePosition)}")
                    } else {
                        println("Ray miss")
                    }
                }

                sceneRenderer.render(realm, averageEyePosition, eyeMatrices.leftEyeMatrix, eyeMatrices.rightEyeMatrix)
                gameState.vrManager.markFirstFrameQueueSubmit()
                sceneRenderer.submit(realm, emptyArray(), longArrayOf(renderFinishedSemaphore))
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

        sceneRenderer.destroy(gameState.graphics.troll.vkDevice())

        vkDestroySemaphore(gameState.graphics.troll.vkDevice(), renderFinishedSemaphore, null)
        vkDestroySemaphore(gameState.graphics.troll.vkDevice(), debugPanelSemaphore, null)
    }
}
