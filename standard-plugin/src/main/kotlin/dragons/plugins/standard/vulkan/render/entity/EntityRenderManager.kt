package dragons.plugins.standard.vulkan.render.entity

import dragons.geometry.*
import dragons.geometry.Vector
import dragons.plugin.PluginManager
import dragons.plugins.standard.vulkan.render.StandardSceneRenderer
import dragons.world.entity.EntityProperties
import dragons.world.realm.Realm
import org.lwjgl.vulkan.VkDevice
import com.github.knokko.boiler.sync.WaitSemaphore
import java.util.*
import kotlin.collections.ArrayList
import kotlin.reflect.KClass

class EntityRenderManager(
    pluginManager: PluginManager
) {
    private val renderFactoryMap = mutableMapOf<KClass<EntityProperties>, EntityRendererFactory<EntityProperties>>()

    init {
        for ((rawEntityRendererFactory, _) in pluginManager.getImplementations(EntityRendererFactory::class)) {
            @Suppress("UNCHECKED_CAST")
            val entityRendererFactory = rawEntityRendererFactory as EntityRendererFactory<EntityProperties>
            renderFactoryMap[entityRendererFactory.getEntityType()] = entityRendererFactory
        }
    }

    private val loadedEntities = mutableMapOf<UUID, EntityEntry>()
    // TODO Eventually unload entities
    private var chosenEntities: Collection<UUID>? = null

    fun renderEntities(sceneRenderer: StandardSceneRenderer, realm: Realm, cameraPosition: Position) {
        val renderDistance = Distance.meters(100)
        val renderDistanceVector = Vector(renderDistance, renderDistance, renderDistance)
        this.chosenEntities = realm.queryEntityIDsBetween(
            BoundingBox(
            cameraPosition - renderDistanceVector, cameraPosition + renderDistanceVector
        )
        )

        for (id in this.chosenEntities!!) {

            val entity = realm.getEntity(id)
            val entry = this.loadedEntities.computeIfAbsent(id) {
                val rendererFactory = this.renderFactoryMap[entity.properties::class]
                    ?: throw UnsupportedOperationException("Don't know how to render entity class $${entity.properties::class.java}")
                EntityEntry(rendererFactory.createRenderer(entity.properties))
            }

            entry.renderer.render(sceneRenderer, entity, cameraPosition)
        }
    }

    fun getWaitSemaphores(realm: Realm): Collection<WaitSemaphore> {
        val waitSemaphores = ArrayList<WaitSemaphore>()

        for (id in chosenEntities!!) {
            val entry = loadedEntities[id]!!
            waitSemaphores.addAll(entry.renderer.getWaitSemaphores(realm.getEntity(id)))
        }

        return waitSemaphores
    }

    fun destroy(vkDevice: VkDevice) {
        for (entry in loadedEntities.values) {
            entry.destroy(vkDevice)
        }
    }
}

private class EntityEntry(
    val renderer: EntityRenderer
) {
    fun destroy(vkDevice: VkDevice) {
        renderer.destroy(vkDevice)
    }
}
