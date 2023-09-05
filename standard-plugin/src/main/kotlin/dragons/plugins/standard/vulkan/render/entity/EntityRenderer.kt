package dragons.plugins.standard.vulkan.render.entity

import com.github.knokko.boiler.sync.WaitSemaphore
import dragons.plugins.standard.vulkan.render.StandardSceneRenderer
import dragons.geometry.Position
import dragons.world.entity.Entity
import dragons.world.entity.EntityProperties
import knokko.plugin.MagicPluginInterface
import org.lwjgl.vulkan.VkDevice
import java.util.*
import kotlin.reflect.KClass

interface EntityRendererFactory<T: EntityProperties>: MagicPluginInterface {
    fun getEntityType(): KClass<T>

    fun createRenderer(entity: T): EntityRenderer
}

interface EntityRenderer {
    fun render(renderer: StandardSceneRenderer, entity: Entity, cameraPosition: Position)

    fun getWaitSemaphores(entity: Entity): Collection<WaitSemaphore> = Collections.emptyList()

    fun destroy(vkDevice: VkDevice) {}
}
