package dragons.plugins.standard.vulkan.render.entity

import dragons.plugin.interfaces.PluginInterface
import dragons.plugins.standard.vulkan.render.StandardSceneRenderer
import dragons.space.Position
import dragons.world.entity.Entity
import dragons.world.entity.EntityProperties
import org.lwjgl.vulkan.VkDevice
import java.util.*
import kotlin.reflect.KClass

interface EntityRendererFactory<T: EntityProperties>: PluginInterface {
    fun getEntityType(): KClass<T>

    fun createRenderer(entity: T): EntityRenderer
}

interface EntityRenderer {
    fun render(renderer: StandardSceneRenderer, entity: Entity, cameraPosition: Position)

    fun getWaitSemaphores(entity: Entity): Collection<Pair<Long, Int>> = Collections.emptyList()

    fun destroy(vkDevice: VkDevice) {}
}
