package dragons.plugins.standard.vulkan.render.chunk

import dragons.plugins.standard.state.StandardPluginState
import dragons.plugins.standard.vulkan.pipeline.updateBasicDynamicDescriptorSet
import dragons.plugins.standard.vulkan.render.tile.*
import dragons.plugins.standard.vulkan.render.tile.StandardTileRenderer
import dragons.state.StaticGameState
import dragons.vulkan.memory.VulkanImage
import dragons.vulkan.memory.scope.MemoryScopeClaims
import dragons.vulkan.memory.scope.packMemoryClaims
import dragons.world.chunk.ChunkLocation
import dragons.world.realm.Realm
import dragons.world.tile.TileProperties
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import java.util.*
import kotlin.reflect.KClass

internal class ChunkLoader(
    private val gameState: StaticGameState,
    private val pluginState: StandardPluginState,
    private val tileRenderer: StandardTileRenderer
) {

    private val descriptors = ChunkLoaderDescriptors(gameState.graphics, pluginState.graphics)
    private val renderFactoryMap = mutableMapOf<KClass<TileProperties>, TileRendererFactory<TileProperties>>()

    init {
        for ((rawTileRendererFactory, _) in gameState.pluginManager.getImplementations(TileRendererFactory::class)) {
            @Suppress("UNCHECKED_CAST")
            val tileRendererFactory = rawTileRendererFactory as TileRendererFactory<TileProperties>
            renderFactoryMap[tileRendererFactory.getTileType()] = tileRendererFactory
        }
    }

    fun loadChunk(realm: Realm, location: ChunkLocation, loadedChunks: MutableMap<ChunkLocation, ChunkEntry>) {
        val chunk = realm.getChunk(location)

        val tileRendererClaims = mutableMapOf<UUID, TileRendererClaims>()
        for (tile in chunk.getAllTiles()) {
            val tileRenderFactory = renderFactoryMap[tile.properties::class]
                ?: throw UnsupportedOperationException("Don't know how to render tile class ${tile.properties::class} (${tile.properties.getPersistentClassID()})")
            tileRendererClaims[tile.id] = tileRenderFactory.createClaims(tile.properties)
        }

        val maxNumIndirectDrawCalls = tileRendererClaims.values.sumOf { it.getMaxNumDrawTileCalls() }

        val requestedColorImages = mutableListOf<CompletableDeferred<VulkanImage>>()
        val requestedHeightImages = mutableListOf<CompletableDeferred<VulkanImage>>()
        val allClaims = tileRendererClaims.values.map { claimer ->
            val claims = MemoryScopeClaims()
            val agent = TileMemoryClaimAgent(
                gameState, claims,
                colorImages = requestedColorImages,
                heightImages = requestedHeightImages
            )
            claimer.claimMemory(agent)
            agent.claims
        }

        val tileRenderers = mutableMapOf<UUID, TileRenderer>()

        // TODO Refactor this to be truly async
        val graphics = gameState.graphics
        val (scope, colorImages, heightImages) = runBlocking {
            val scope = packMemoryClaims(
                graphics.vkDevice, graphics.queueManager, graphics.memoryInfo,
                gameState.coroutineScope, allClaims, location.toString()
            )

            for ((id, claims) in tileRendererClaims) {
                tileRenderers[id] = claims.createRenderer()
            }

            val colorImages = requestedColorImages.map { it.await() }
            val heightImages = requestedHeightImages.map { it.await() }
            Triple(scope, colorImages, heightImages)
        }

        val vertexAndIndexBuffer = scope.deviceBuffers[graphics.queueManager.generalQueueFamily]
        var descriptorSet: Long? = null
        if (vertexAndIndexBuffer != null) {
            descriptorSet = descriptors.borrowDescriptorSet()
            updateBasicDynamicDescriptorSet(
                graphics.vkDevice, descriptorSet, colorImages = colorImages, heightImages = heightImages
            )

            tileRenderer.addChunk(
                vertexBuffer = vertexAndIndexBuffer,
                indexBuffer = vertexAndIndexBuffer,
                dynamicDescriptorSet = descriptorSet,
                maxNumIndirectDrawCalls = maxNumIndirectDrawCalls
            )
        }
        loadedChunks[location] = ChunkEntry(tiles = tileRenderers, memoryScope = scope, descriptorSet = descriptorSet)
    }

    fun unloadChunk(location: ChunkLocation, loadedChunks: MutableMap<ChunkLocation, ChunkEntry>) {
        val chunkEntry = loadedChunks.remove(location) ?: throw IllegalStateException("Unloading chunk that is not loaded")
        chunkEntry.destroy(gameState.graphics.vkDevice)
        if (chunkEntry.descriptorSet != null) {
            descriptors.returnDescriptorSet(chunkEntry.descriptorSet)
        }
    }

    fun destroy() {
        descriptors.destroy()
    }
}
