package dragons.plugins.standard.vulkan.render.tile

import dragons.plugin.interfaces.PluginInterface
import dragons.plugins.standard.vulkan.render.StandardSceneRenderer
import dragons.world.tile.TileProperties
import dragons.world.tile.TileState
import org.joml.Vector3f
import org.lwjgl.vulkan.VkDevice
import java.util.*
import kotlin.reflect.KClass

interface TileRendererFactory<T: TileProperties>: PluginInterface {
    fun getTileType(): KClass<T>

    fun createClaims(tile: T): TileRendererClaims
}

interface TileRendererClaims {
    fun claimMemory(agent: TileMemoryClaimAgent)

    fun getMaxNumDrawTileCalls(): Int

    suspend fun createRenderer(): TileRenderer
}

interface TileRenderer {
    // TODO Maybe lazily obtain state because many renderers don't even need it
    fun render(renderer: StandardSceneRenderer, state: TileState, negativeCameraPosition: Vector3f)

    fun getWaitSemaphores(state: TileState): Collection<Pair<Long, Int>> = Collections.emptyList()

    fun destroy(vkDevice: VkDevice) {}
}
