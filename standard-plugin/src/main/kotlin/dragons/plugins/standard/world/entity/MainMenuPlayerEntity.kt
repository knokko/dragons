package dragons.plugins.standard.world.entity

import dragons.plugins.standard.vulkan.model.generator.dragon.DragonWingProperties
import dragons.plugins.standard.vulkan.model.generator.dragon.createDragonWingGenerator
import dragons.plugins.standard.vulkan.model.matrices.createDragonWingMatrices
import dragons.plugins.standard.vulkan.render.StandardSceneRenderer
import dragons.plugins.standard.vulkan.render.entity.*
import dragons.space.Distance
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
                renderer.drawEntity(WING_MESH, createDragonWingMatrices(leftHandMatrix, WING_PROPS))
            }

            val rightHandMatrix = state.rightHandMatrix
            if (rightHandMatrix != null) {
                renderer.drawEntity(WING_MESH, createDragonWingMatrices(rightHandMatrix, WING_PROPS))
            }
        }

        class Factory: EntityRendererFactory<MainMenuPlayerEntity> {
            override fun getEntityType() = MainMenuPlayerEntity::class

            override fun createRenderer(entity: MainMenuPlayerEntity) = Renderer()
        }

        companion object {
            val WING_PROPS = DragonWingProperties(
                baseWingLength = Distance.meters(0.5f),
                wingLaneWidth = Distance.meters(0.2f),
                wingTopLength = Distance.meters(0.3f),
                wingDepth = Distance.milliMeters(30),
                nailLength = Distance.meters(0.3f),
                nailWidth = Distance.meters(0.15f)
            )

            private val WING_MESH = EntityMesh(
                generator = createDragonWingGenerator(WING_PROPS, listOf(0, 1, 2, 3, 4), false),
                colorImages = listOf(
                    ClasspathEntityColorImage("dragons/plugins/standard/images/testTerrain.jpg", 1024, 1024)
                ),
                heightImages = listOf(
                    ClasspathEntityHeightImage("dragons/plugins/standard/images/testTerrainHeight.png", 0.0f, 128, 128)
                ),
                numTransformationMatrices = 5
            )
        }
    }
}
