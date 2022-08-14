package dragons.plugins.standard.menu

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.menu.MainMenuManager
import dragons.plugins.standard.state.StandardPluginState
import dragons.plugins.standard.vulkan.MAX_NUM_INDIRECT_DRAW_CALLS
import dragons.plugins.standard.vulkan.command.fillDrawingBuffers
import dragons.plugins.standard.vulkan.pipeline.updateBasicDynamicDescriptorSet
import dragons.plugins.standard.vulkan.render.StandardSceneRenderer
import dragons.state.StaticGameState
import dragons.util.Angle
import dragons.util.getStandardOutputHistory
import dragons.vulkan.util.assertVkSuccess
import graviks2d.resource.text.TextStyle
import graviks2d.util.Color
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.joml.Math.cos
import org.joml.Math.sin
import org.joml.Vector3f
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkSemaphoreCreateInfo
import kotlin.math.atan2

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

        // TODO Handle these properly...
        updateBasicDynamicDescriptorSet(
            gameState.graphics.vkDevice,
            pluginState.graphics.basicDynamicDescriptorSet,
            pluginState.graphics.mainMenu.textureSet.colorTextureList,
            pluginState.graphics.mainMenu.textureSet.heightTextureList
        )

        val sceneRenderer = StandardSceneRenderer(gameState, pluginState)

        sceneRenderer.addChunk(
            vertexBuffer = pluginState.graphics.mainMenu.modelSet.vertexBuffer,
            indexBuffer = pluginState.graphics.mainMenu.modelSet.indexBuffer,
            dynamicDescriptorSet = pluginState.graphics.basicDynamicDescriptorSet,
            maxNumIndirectDrawCalls = MAX_NUM_INDIRECT_DRAW_CALLS - 1 // TODO Handle this neatly
        )

        val renderFinishedSemaphore = createSemaphore("main menu rendering")
        val debugPanelSemaphore = createSemaphore("main menu debug panel")

        // TODO Add more iterations (and eventually loop indefinitely)
        var numIterationsLeft = 500

        val currentPosition = Vector3f()
        var extraRotation = 0f

        var lastStandardOutputHistory = emptyList<String>()

        while (!gameState.vrManager.shouldStop()) {
            val currentInput = gameState.vrManager.getDragonControls()

            val eyeMatrices = gameState.vrManager.prepareRender(Angle.degrees(extraRotation))
            if (eyeMatrices != null) {

                val currentMovement = currentInput.walkDirection
                val currentDirection = eyeMatrices.averageViewMatrix.transformDirection(Vector3f(0f, 0f, 1f))
                val currentRotation = atan2(currentDirection.x, currentDirection.z)

                currentPosition.x += 0.1f * (cos(currentRotation) * currentMovement.x + sin(currentRotation) * currentMovement.y)
                currentPosition.z += 0.1f * (sin(currentRotation) * currentMovement.x - cos(currentRotation) * currentMovement.y)

                extraRotation += currentInput.cameraTurnDirection * 2f

                if (currentInput.isGrabbingLeft) {
                    currentPosition.y -= 0.1f
                }
                if (currentInput.isGrabbingRight) {
                    currentPosition.y += 0.1f
                }

                // Work around to let the user choose when to end the game
                if (currentMovement.length() > 0f) {
                    numIterationsLeft = -1
                }
                if (currentInput.shouldToggleMenu) {
                    numIterationsLeft = 1
                }

                val averageEyePosition = eyeMatrices.averageVirtualEyePosition.add(currentPosition, Vector3f())

                pluginState.graphics.debugPanel.execute {

                    val newStandardOutputHistory = getStandardOutputHistory(50)
                    if (newStandardOutputHistory !== lastStandardOutputHistory) {

                        val backgroundColor = Color.rgbInt(200, 0, 0)
                        it.fillRect(0.1f, 0.1f, 0.9f, 0.9f, backgroundColor)
                        it.fillRect(0f, 0f, 1f, 0.1f, Color.rgbInt(0, 100, 0))
                        it.fillRect(0f, 0.9f, 1f, 1f, Color.rgbInt(0, 255, 0))
                        it.fillRect(0f, 0f, 0.1f, 1f, Color.rgbInt(0, 0, 100))
                        it.fillRect(0.9f, 0f, 1f, 1f, Color.rgbInt(0, 0, 255))

                        val style = TextStyle(fillColor = Color.rgbInt(0, 0, 0), font = null)
                        for ((index, line) in newStandardOutputHistory.withIndex()) {
                            it.drawString(
                                0.11f, 0.885f - index * 0.015f, 0.89f, 0.9f - index * 0.015f,
                                line, style, backgroundColor
                            )
                        }

                        lastStandardOutputHistory = newStandardOutputHistory
                    }
                }

                val submissionMarker = CompletableDeferred<Unit>()
                pluginState.graphics.debugPanel.updateImage(debugPanelSemaphore, submissionMarker)

                stackPush().use { stack ->

                    sceneRenderer.startFrame(stack, eyeMatrices.leftEyeMatrix, eyeMatrices.rightEyeMatrix)
                    fillDrawingBuffers(sceneRenderer, pluginState.graphics, averageEyePosition)
                    sceneRenderer.endFrame()

                    // The debug panel queue submission must have ended before I can submit the frame queue submission
                    runBlocking { submissionMarker.await() }

                    gameState.vrManager.markFirstFrameQueueSubmit()
                    sceneRenderer.submit(
                        longArrayOf(debugPanelSemaphore),
                        intArrayOf(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT),
                        longArrayOf(renderFinishedSemaphore)
                    )
                }
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
