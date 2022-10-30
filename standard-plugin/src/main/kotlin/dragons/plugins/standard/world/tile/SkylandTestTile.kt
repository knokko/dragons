package dragons.plugins.standard.world.tile

import dragons.plugins.standard.vulkan.model.generator.generateSkylandModel
import dragons.plugins.standard.vulkan.render.StandardSceneRenderer
import dragons.plugins.standard.vulkan.render.tile.TileMemoryClaimAgent
import dragons.plugins.standard.vulkan.render.tile.TileRenderer
import dragons.plugins.standard.vulkan.render.tile.TileRendererClaims
import dragons.plugins.standard.vulkan.render.tile.TileRendererFactory
import dragons.plugins.standard.vulkan.util.claimColorImage
import dragons.plugins.standard.vulkan.util.claimHeightImage
import dragons.plugins.standard.vulkan.util.claimVertexAndIndexBuffer
import dragons.space.Position
import dragons.vulkan.memory.VulkanBufferRange
import dragons.vulkan.memory.VulkanImage
import dragons.world.tile.SmallTile
import dragons.world.tile.TileProperties
import dragons.world.tile.TileState
import kotlinx.coroutines.CompletableDeferred
import org.joml.Matrix4f

class SkylandTestTile(position: Position): TileProperties(position) {

    override fun getPersistentClassID() = "standard-plugin:SkylandTestTile"

    class State: TileState

    @Suppress("unused")
    class Renderer(
        private val vertices: VulkanBufferRange,
        private val indices: VulkanBufferRange,
    ): TileRenderer {

        override fun render(renderer: StandardSceneRenderer, tile: SmallTile, cameraPosition: Position) {
            val renderPosition = tile.properties.position - cameraPosition
            val transformationMatrix = Matrix4f()
                .translate(renderPosition.x.meters, renderPosition.y.meters, renderPosition.z.meters)
                .scale(10f)
            renderer.drawTile(vertices, indices, arrayOf(transformationMatrix))
        }

        class Factory: TileRendererFactory<SkylandTestTile> {
            override fun getTileType() = SkylandTestTile::class

            override fun createClaims(tile: SkylandTestTile) = Claims(tile)
        }

        class Claims(private val tile: SkylandTestTile): TileRendererClaims {

            private val vertices = CompletableDeferred<VulkanBufferRange>()
            private val indices = CompletableDeferred<VulkanBufferRange>()

            override fun claimMemory(agent: TileMemoryClaimAgent) {
                val colorTexture = CompletableDeferred<VulkanImage>()
                val heightTexture = CompletableDeferred<VulkanImage>()

                val graphics = agent.gameState.graphics
                claimVertexAndIndexBuffer(
                    agent.claims, graphics.queueManager, vertices, indices, generateSkylandModel { 0.5f },
                    listOf(agent.claimColorImageIndex(colorTexture)), listOf(agent.claimHeightImageIndex(heightTexture)),
                    "standard plug-in:SkylandTestTile"
                )
                claimColorImage(
                    agent.claims, graphics.queueManager, agent.gameState.classLoader, 1024, 1024, colorTexture,
                    "dragons/plugins/standard/images/testTerrain.jpg", "standard plug-in:SkylandTestTile"
                )
                claimHeightImage(
                    agent.claims, graphics.queueManager, agent.gameState.classLoader, 128, 128, heightTexture,
                    "dragons/plugins/standard/images/testTerrainHeight.png", 0.001f,
                    "standard plug-in:SkylandTestTile"
                )
            }

            override suspend fun createRenderer() = Renderer(
                vertices = this.vertices.await(),
                indices = this.indices.await()
            )

            override fun getMaxNumDrawTileCalls() = 1
        }
    }
}
