package dragons.plugins.standard.vulkan.render.tile

import dragons.plugin.interfaces.PluginInterface
import dragons.plugins.standard.vulkan.render.StandardSceneRenderer
import dragons.geometry.Position
import dragons.world.tile.SmallTile
import dragons.world.tile.TileProperties
import org.lwjgl.vulkan.VkDevice
import java.util.*
import kotlin.reflect.KClass

/**
 * A `TileRendererFactory<T>` tells the game how to render tiles of type `T`. You should create a class that
 * implements `TileRendererFactory` for each tile class.
 *
 * This class must have either no constructors or 1 public constructor that doesn't take any parameters. You should
 * *not* create any instances of this class because the standard plug-in will do that for you: it will create exactly
 * 1 instance of the class and call its `createClaims` method once per tile.
 */
interface TileRendererFactory<T: TileProperties>: PluginInterface {
    fun getTileType(): KClass<T>

    fun createClaims(tile: T): TileRendererClaims
}

/**
 * `TileRendererFactory`s will create 1 instance of this class per tile. This class has a method `claimMemory` that
 * will be called *before* `createRenderer`. You should claim any vertex, index, etc... buffers in the `claimMemory`
 * method and `.await()` them in the `createRenderer` method.
 */
interface TileRendererClaims {
    fun claimMemory(agent: TileMemoryClaimAgent)

    fun getMaxNumDrawTileCalls(): Int

    suspend fun createRenderer(): TileRenderer
}

/**
 * Each instance of `TileRenderer` is responsible for rendering 1 instance of a tile.
 */
interface TileRenderer {
    fun render(renderer: StandardSceneRenderer, tile: SmallTile, cameraPosition: Position)

    fun getWaitSemaphores(tile: SmallTile): Collection<Pair<Long, Int>> = Collections.emptyList()

    fun destroy(vkDevice: VkDevice) {}
}
