package dragons.plugins.standard.menu

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.menu.MainMenuManager
import dragons.plugins.standard.vulkan.command.createMainMenuRenderCommands
import dragons.plugins.standard.vulkan.command.fillDrawingBuffers
import dragons.state.StaticGameState
import dragons.vulkan.util.assertVkSuccess
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkSemaphoreCreateInfo
import org.lwjgl.vulkan.VkSubmitInfo

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

        val (mainCommandPool, mainCommandBuffer) = createMainMenuRenderCommands(pluginInstance, gameState)
        val renderFinishedSemaphore = stackPush().use { stack ->
            val ciSemaphore = VkSemaphoreCreateInfo.calloc(stack)
            ciSemaphore.`sType$Default`()

            val pSemaphore = stack.callocLong(1)
            assertVkSuccess(
                vkCreateSemaphore(gameState.graphics.vkDevice, ciSemaphore, null, pSemaphore),
                "CreateSemaphore", "standard plug-in: main menu rendering"
            )
            pSemaphore[0]
        }

        // TODO Add more iterations (and eventually loop indefinitely)
        var numIterationsLeft = 1000

        while (numIterationsLeft > 0) {
            val eyeMatrices = gameState.vrManager.prepareRender()
            if (eyeMatrices != null) {

                val (leftEyeMatrix, rightEyeMatrix) = eyeMatrices
                fillDrawingBuffers(pluginInstance, gameState, leftEyeMatrix, rightEyeMatrix)

                stackPush().use { stack ->
                    val pSubmits = VkSubmitInfo.calloc(1, stack)
                    val pSubmit = pSubmits[0]
                    pSubmit.`sType$Default`()
                    pSubmit.waitSemaphoreCount(0)
                    pSubmit.pCommandBuffers(stack.pointers(mainCommandBuffer.address()))
                    pSubmit.pSignalSemaphores(stack.longs(renderFinishedSemaphore))
                    gameState.vrManager.markFirstFrameQueueSubmit()
                    gameState.graphics.queueManager.generalQueueFamily.getRandomPriorityQueue().submit(pSubmits, VK_NULL_HANDLE)
                }

                gameState.graphics.resolveHelper.resolve(
                    gameState.graphics.vkDevice, gameState.graphics.queueManager,
                    renderFinishedSemaphore, false
                )
            } else {
                println("Can't render main menu")
            }

            gameState.vrManager.submitFrames()

            numIterationsLeft--
        }

        vkDestroySemaphore(gameState.graphics.vkDevice, renderFinishedSemaphore, null)
        vkDestroyCommandPool(gameState.graphics.vkDevice, mainCommandPool, null)
    }
}
