package dragons.plugins.standard.vulkan.render.chunk

import dragons.plugins.standard.state.StandardPluginState
import dragons.plugins.standard.vulkan.render.StandardSceneRenderer
import dragons.plugins.standard.vulkan.render.tile.*
import dragons.plugins.standard.vulkan.render.tile.StandardTileRenderer
import dragons.geometry.Position
import dragons.state.StaticGameState
import dragons.vulkan.memory.scope.MemoryScope
import dragons.world.chunk.ChunkLocation
import dragons.world.realm.Realm
import org.lwjgl.vulkan.VkDevice
import troll.sync.WaitSemaphore
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

internal class ChunkRenderManager(
    gameState: StaticGameState, pluginState: StandardPluginState,
    tileRenderer: StandardTileRenderer
) {

    private val chunkLoader = ChunkLoader(gameState, pluginState, tileRenderer)
    private val loadedChunks = mutableMapOf<ChunkLocation, ChunkEntry>()

    private var chosenChunks: Collection<ChunkLocation>? = null

    fun chooseAndLoadChunks(realm: Realm, cameraPosition: Position) {
        if (this.chosenChunks != null) throw IllegalStateException("You must render after each call to chooseAndLoadChunks()")

        // TODO Reset all loaded chunks when the realm is not identical to the previous realm
        // TODO Level of detail for each chunk location

        val chosenChunks = HashSet<ChunkLocation>()
        val centerLocation = ChunkLocation(cameraPosition)
        val radius = 2
        for (x in centerLocation.chunkX - radius .. centerLocation.chunkX + radius) {
            for (y in centerLocation.chunkY - radius .. centerLocation.chunkY + radius) {
                for (z in centerLocation.chunkZ - radius .. centerLocation.chunkZ + radius) {
                    chosenChunks.add(ChunkLocation(x, y, z))
                }
            }
        }

        for (chunk in chosenChunks) {
            if (!loadedChunks.containsKey(chunk)) {
                // TODO I might want to load chunks asynchronously
                chunkLoader.loadChunk(realm, chunk, loadedChunks)
            }
        }

        // TODO Eventually clean up loaded chunks that have not been rendered recently

        this.chosenChunks = chosenChunks
    }

    fun renderChunks(sceneRenderer: StandardSceneRenderer, realm: Realm, cameraPosition: Position) {
        for (location in this.chosenChunks!!) {
            val chunk = realm.getChunk(location)
            val chunkEntry = loadedChunks[location]!!
            for ((tileID, tileRenderer) in chunkEntry.tiles) {
                tileRenderer.render(sceneRenderer, chunk.getTile(tileID), cameraPosition)
            }
        }
    }

    fun getWaitSemaphores(realm: Realm): Collection<WaitSemaphore> {
        val waitSemaphores = ArrayList<WaitSemaphore>()

        for (location in this.chosenChunks!!) {
            val chunk = realm.getChunk(location)
            val chunkEntry = loadedChunks[location]!!
            for ((tileID, tileRenderer) in chunkEntry.tiles) {
                waitSemaphores.addAll(tileRenderer.getWaitSemaphores(chunk.getTile(tileID)))
            }
        }

        this.chosenChunks = null

        return waitSemaphores
    }

    fun destroy(vkDevice: VkDevice) {
        for (chunkEntry in loadedChunks.values) {
            chunkEntry.destroy(vkDevice)
        }
        chunkLoader.destroy()
    }
}

internal class ChunkEntry(
    val tiles: Map<UUID, TileRenderer>,
    private val memoryScope: MemoryScope,
    val descriptorSet: Long?
) {
    fun destroy(vkDevice: VkDevice) {
        memoryScope.destroy(vkDevice)
        for (tileRenderer in tiles.values) {
            tileRenderer.destroy(vkDevice)
        }
    }
}
