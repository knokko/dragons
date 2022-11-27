package dragons.plugins.standard.world.tile

import dragons.plugins.standard.vulkan.model.generator.dragon.DragonWingProperties
import dragons.plugins.standard.vulkan.model.generator.dragon.createDragonWingGenerator
import dragons.plugins.standard.vulkan.model.generator.generateSkylandModel
import dragons.plugins.standard.vulkan.model.matrices.createDragonWingMatrices
import dragons.plugins.standard.vulkan.render.StandardSceneRenderer
import dragons.plugins.standard.vulkan.render.tile.TileMemoryClaimAgent
import dragons.plugins.standard.vulkan.render.tile.TileRenderer
import dragons.plugins.standard.vulkan.render.tile.TileRendererClaims
import dragons.plugins.standard.vulkan.render.tile.TileRendererFactory
import dragons.plugins.standard.vulkan.util.claimColorImage
import dragons.plugins.standard.vulkan.util.claimHeightImage
import dragons.plugins.standard.vulkan.util.claimVertexAndIndexBuffer
import dragons.space.Distance
import dragons.space.Position
import dragons.vulkan.memory.VulkanBufferRange
import dragons.vulkan.memory.VulkanImage
import dragons.world.tile.SmallTile
import dragons.world.tile.TileProperties
import dragons.world.tile.TileState
import kotlinx.coroutines.CompletableDeferred
import org.joml.Matrix4f
import org.joml.Random

val props = DragonWingProperties(
    baseWingLength = Distance.meters(0.5f),
    wingLaneWidth = Distance.meters(0.2f),
    wingTopLength = Distance.meters(0.3f),
    wingDepth = Distance.milliMeters(30),
    nailLength = Distance.meters(0.3f),
    nailWidth = Distance.meters(0.15f)
)

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
            val rng = Random()
            val transformationMatrix = Matrix4f()
                .translate(renderPosition.x.meters, renderPosition.y.meters, renderPosition.z.meters)
                .rotateX(6f * rng.nextFloat())
                .rotateY(6f * rng.nextFloat())
                .rotateZ(6f * rng.nextFloat())
                .scale(100f)
            renderer.drawTile(vertices, indices, createDragonWingMatrices(transformationMatrix, props))
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
//                claimVertexAndIndexBuffer(
//                    agent.claims, graphics.queueManager, vertices, indices, generateSkylandModel { 0.5f },
//                    listOf(agent.claimColorImageIndex(colorTexture)), listOf(agent.claimHeightImageIndex(heightTexture)),
//                    "standard plug-in:SkylandTestTile"
//                )

                claimVertexAndIndexBuffer(
                    agent.claims, graphics.queueManager, vertices, indices, createDragonWingGenerator(
                        props, listOf(0, 1, 2, 3, 4), false
                    ),
                    listOf(agent.claimColorImageIndex(colorTexture)), listOf(agent.claimHeightImageIndex(heightTexture)),
                    "standard plug-in:SkylandTestTile"
                )
                claimColorImage(
                    agent.claims, graphics.queueManager, agent.gameState.classLoader, 1024, 1024, colorTexture,
                    "dragons/plugins/standard/images/testTerrain.jpg", "standard plug-in:SkylandTestTile"
                )
//                claimHeightImage(
//                    agent.claims, graphics.queueManager, agent.gameState.classLoader, 128, 128, heightTexture,
//                    "dragons/plugins/standard/images/testTerrainHeight.png", 0.001f,
//                    "standard plug-in:SkylandTestTile"
//                )
                claimHeightImage(
                    agent.claims, graphics.queueManager, 128, 128, heightTexture,
                    "standard plug-in:SkylandTestTile"
                ) { _, _ -> 0f }
            }

            override suspend fun createRenderer() = Renderer(
                vertices = this.vertices.await(),
                indices = this.indices.await()
            )

            override fun getMaxNumDrawTileCalls() = 1
        }
    }
}
