package dragons.plugins.standard.world.entity

import dragons.plugins.standard.vulkan.render.StandardSceneRenderer
import dragons.plugins.standard.vulkan.render.entity.EntityRenderer
import dragons.plugins.standard.vulkan.render.entity.EntityRendererFactory
import dragons.space.Position
import dragons.world.entity.Entity
import dragons.world.entity.EntityProperties
import dragons.world.entity.EntityState
import org.joml.Matrix4f

class MainMenuPlayerEntity: EntityProperties() {
    override fun getPersistentClassID() = "standard-plugin:MainMenuPlayer"

    class State(
        position: Position,
    ): EntityState(position) {
        var leftHandMatrix: Matrix4f? = null
        var rightHandMatrix: Matrix4f? = null
    }

    @Suppress("unused")
    class Renderer: EntityRenderer {
        override fun render(renderer: StandardSceneRenderer, entity: Entity, cameraPosition: Position) {
            val state = entity.copyState() as State

            val leftHandMatrix = state.leftHandMatrix
            if (leftHandMatrix != null) {
                renderer.drawEntity(SkylandTestEntity.Renderer.MESH, arrayOf(leftHandMatrix))
            }

            val rightHandMatrix = state.rightHandMatrix
            if (rightHandMatrix != null) {
                renderer.drawEntity(SkylandTestEntity.Renderer.MESH, arrayOf(rightHandMatrix))
            }
        }

        class Factory: EntityRendererFactory<MainMenuPlayerEntity> {
            override fun getEntityType() = MainMenuPlayerEntity::class

            override fun createRenderer(entity: MainMenuPlayerEntity) = Renderer()
        }
    }
}
