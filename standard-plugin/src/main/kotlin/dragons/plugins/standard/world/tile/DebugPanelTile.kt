package dragons.plugins.standard.world.tile

import dragons.plugins.standard.vulkan.model.generator.PANEL_MODEL_GENERATOR
import dragons.plugins.standard.vulkan.panel.Panel
import dragons.plugins.standard.vulkan.render.StandardSceneRenderer
import dragons.plugins.standard.vulkan.render.tile.TileMemoryClaimAgent
import dragons.plugins.standard.vulkan.render.tile.TileRenderer
import dragons.plugins.standard.vulkan.render.tile.TileRendererClaims
import dragons.plugins.standard.vulkan.render.tile.TileRendererFactory
import dragons.plugins.standard.vulkan.util.claimHeightImage
import dragons.plugins.standard.vulkan.util.claimVertexAndIndexBuffer
import dragons.geometry.Angle
import dragons.geometry.Distance
import dragons.geometry.Position
import dragons.geometry.shape.VerticalPlaneShape
import dragons.util.PerformanceStatistics
import dragons.util.getStandardOutputHistory
import dragons.vulkan.memory.VulkanBufferRange
import dragons.vulkan.memory.VulkanImage
import dragons.vulkan.memory.claim.ImageMemoryClaim
import dragons.vulkan.util.assertVkSuccess
import dragons.world.tile.SmallTile
import dragons.world.tile.TileProperties
import dragons.world.tile.TileState
import graviks2d.core.GraviksInstance
import graviks2d.resource.text.TextStyle
import graviks2d.util.Color
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.joml.Matrix4f
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkSemaphoreCreateInfo

private const val PANEL_WIDTH = 4000
private const val PANEL_HEIGHT = 5000

class DebugPanelTile(
    position: Position,
    private val rotation: Angle
): TileProperties(position, VerticalPlaneShape(Distance.meters(30), Distance.meters(37.5f), rotation)) {
    override fun getPersistentClassID() = "standard-plugin:DebugPanelTile"

    class State: TileState

    @Suppress("unused")
    class Renderer(
        private val vertices: VulkanBufferRange,
        private val indices: VulkanBufferRange,
        private val panel: Panel,
        private val panelSemaphore: Long
    ): TileRenderer {

        private var lastStandardOutputHistory = emptyList<String>()
        private var submissionMarker: CompletableDeferred<Unit>? = null
        private var isFirstRender = true

        override fun render(renderer: StandardSceneRenderer, tile: SmallTile, cameraPosition: Position) {
            val scaleX = 60f
            val aspectRatio = PANEL_WIDTH.toFloat() / PANEL_HEIGHT.toFloat()
            val scaleY = scaleX / aspectRatio

            val properties = tile.properties as DebugPanelTile

            val renderPosition = properties.position - cameraPosition

            val transformationMatrix = Matrix4f()
                .translate(renderPosition.x.meters.toFloat(), renderPosition.y.meters.toFloat(), renderPosition.z.meters.toFloat())
                .rotateY(properties.rotation.radians).scale(scaleX, scaleY, 1f)

            renderer.drawTile(vertices, indices, arrayOf(transformationMatrix))

            val newStandardOutputHistory = getStandardOutputHistory(50)

            if (lastStandardOutputHistory != newStandardOutputHistory || isFirstRender) {

                panel.execute {
                    val backgroundColor = Color.rgbInt(200, 0, 0)
                    it.fillRect(0.1f, 0.1f, 0.9f, 0.9f, backgroundColor)
                    it.fillRect(0f, 0f, 1f, 0.1f, Color.rgbInt(0, 100, 0))
                    it.fillRect(0f, 0.9f, 1f, 1f, Color.rgbInt(0, 255, 0))
                    it.fillRect(0f, 0f, 0.1f, 1f, Color.rgbInt(0, 0, 100))
                    it.fillRect(0.9f, 0f, 1f, 1f, Color.rgbInt(0, 0, 255))

                    val style = TextStyle(fillColor = Color.BLACK, font = null)
                    for ((index, line) in newStandardOutputHistory.withIndex()) {
                        it.drawString(
                            0.11f, 0.885f - index * 0.015f, 0.89f, 0.9f - index * 0.015f,
                            line, style, backgroundColor
                        )
                    }

                    val stats = PerformanceStatistics.get()

                    val infoLines = arrayOf(
                        "${stats.fps} FPS",
                        "Heap memory: ${stats.onHeapMemory}",
                        "Managed off-heap memory: ${stats.managedOffHeapMemory}",
                        "Total memory: ${stats.totalMemory}"
                    )

                    for ((index, infoLine) in infoLines.withIndex()) {
                        it.drawString(
                            0.4f, 0.89f - index * 0.015f, 1f, 0.9f - index * 0.015f,
                            infoLine, style, backgroundColor
                        )
                    }
                }

                lastStandardOutputHistory = newStandardOutputHistory

                val submissionMarker = CompletableDeferred<Unit>()
                panel.updateImage(panelSemaphore, submissionMarker)
                this.submissionMarker = submissionMarker
            }

            isFirstRender = false
        }

        override fun getWaitSemaphores(tile: SmallTile): Collection<Pair<Long, Int>> {
            return if (submissionMarker != null) {
                runBlocking { submissionMarker!!.await() }
                val result: List<Pair<Long, Int>> = listOf(Pair(panelSemaphore, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT))

                submissionMarker = null
                result
            } else {
                emptyList()
            }
        }

        override fun destroy(vkDevice: VkDevice) {
            panel.destroy()
            vkDestroySemaphore(vkDevice, panelSemaphore, null)
        }

        class Factory: TileRendererFactory<DebugPanelTile> {
            override fun getTileType() = DebugPanelTile::class

            override fun createClaims(tile: DebugPanelTile) = Claims(tile)
        }

        class Claims(private val tile: DebugPanelTile): TileRendererClaims {

            private val vertices = CompletableDeferred<VulkanBufferRange>()
            private val indices = CompletableDeferred<VulkanBufferRange>()

            private val panelImage = CompletableDeferred<VulkanImage>()
            private lateinit var graviksInstance: GraviksInstance
            private var panelSemaphore: Long? = null

            override fun claimMemory(agent: TileMemoryClaimAgent) {
                graviksInstance = agent.gameState.graphics.graviksInstance

                val zeroHeightTexture = CompletableDeferred<VulkanImage>()

                agent.claims.images.add(
                    ImageMemoryClaim(
                        width = PANEL_WIDTH, height = PANEL_HEIGHT,
                        queueFamily = agent.gameState.graphics.queueManager.generalQueueFamily,
                        bytesPerPixel = 4, imageFormat = VK_FORMAT_R8G8B8A8_UNORM, tiling = VK_IMAGE_TILING_OPTIMAL,
                        imageUsage = VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT,
                        initialLayout = VK_IMAGE_LAYOUT_UNDEFINED, aspectMask = VK_IMAGE_ASPECT_COLOR_BIT,
                        storeResult = panelImage, sharingID = null, prefill = null
                    )
                )

                claimHeightImage(
                    agent.claims, agent.gameState.graphics.queueManager, 1, 1,
                    zeroHeightTexture, "standard plug-in: zero height"
                ) { _, _ -> 0f }

                claimVertexAndIndexBuffer(
                    agent.claims, agent.gameState.graphics.queueManager, vertices, indices, PANEL_MODEL_GENERATOR,
                    listOf(agent.claimColorImageIndex(panelImage)), listOf(agent.claimHeightImageIndex(zeroHeightTexture)),
                    "standard plug-in: DebugPanelTile"
                )

                this.panelSemaphore = stackPush().use { stack ->
                    val ciSemaphore = VkSemaphoreCreateInfo.calloc(stack)
                    ciSemaphore.`sType$Default`()

                    val pSemaphore = stack.callocLong(1)
                    assertVkSuccess(
                        vkCreateSemaphore(agent.gameState.graphics.vkDevice, ciSemaphore, null, pSemaphore),
                        "CreateSemaphore", "standard-plugin debug panel"
                    )
                    pSemaphore[0]
                }
            }

            override fun getMaxNumDrawTileCalls() = 1

            override suspend fun createRenderer() = Renderer(
                vertices = vertices.await(),
                indices = indices.await(),
                panel = Panel(graviksInstance, panelImage.await()),
                panelSemaphore = panelSemaphore!!
            )
        }
    }
}
