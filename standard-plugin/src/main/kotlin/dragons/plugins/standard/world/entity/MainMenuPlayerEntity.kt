package dragons.plugins.standard.world.entity

import dragons.plugins.standard.vulkan.model.generator.dragon.DragonWingProperties
import dragons.plugins.standard.vulkan.model.generator.dragon.createDragonWingGenerator
import dragons.plugins.standard.vulkan.model.matrices.createDragonWingMatrices
import dragons.plugins.standard.vulkan.render.StandardSceneRenderer
import dragons.plugins.standard.vulkan.render.entity.*
import dragons.space.Angle
import dragons.space.Distance
import dragons.space.Position
import dragons.world.entity.Entity
import dragons.world.entity.EntityProperties
import dragons.world.entity.EntityState
import org.joml.Matrix4f
import org.joml.Vector3f

class MainMenuPlayerEntity: EntityProperties() {
    override fun getPersistentClassID() = "standard-plugin:MainMenuPlayer"

    class State(
        position: Position,
    ): EntityState(position) {
        var regularRotation = Angle.degrees(0f)
        var leftHandMatrix: Matrix4f? = null
        var rightHandMatrix: Matrix4f? = null
        var leftHandAimMatrix: Matrix4f? = null
        var rightHandAimMatrix: Matrix4f? = null
    }

    @Suppress("unused")
    class Renderer: EntityRenderer {
        override fun render(renderer: StandardSceneRenderer, entity: Entity, cameraPosition: Position) {
            val state = entity.copyState() as State

            val headToShoulderX = 0.2f
            val headToShoulderY = -0.2f
            val headToShoulderZ = 0.15f

            val leftHandMatrix = state.leftHandMatrix
            if (leftHandMatrix != null) {
                val shoulderMatrix = Matrix4f()
                    .translation(
                        -headToShoulderX * state.regularRotation.cos - headToShoulderZ * state.regularRotation.sin,
                        headToShoulderY,
                        -headToShoulderX * state.regularRotation.sin - headToShoulderZ * -state.regularRotation.cos
                    ).rotateY((Angle.degrees(90f) - state.regularRotation).radians)

                renderer.drawEntity(WING_MESH, createDragonWingMatrices(leftHandMatrix, shoulderMatrix, WING_PROPS))
            }

            val leftHandAimMatrix = state.leftHandAimMatrix
            if (leftHandAimMatrix != null) {
                val forwardPosition = leftHandAimMatrix.translate(0f, 0f, -1f, Matrix4f()).getTranslation(Vector3f())
                renderer.drawEntity(SkylandTestEntity.Renderer.MESH, arrayOf(Matrix4f().translate(forwardPosition).scale(0.1f)))

                val forwardPosition2 = leftHandAimMatrix.translate(0f, 0f, -5f, Matrix4f()).getTranslation(Vector3f())
                renderer.drawEntity(SkylandTestEntity.Renderer.MESH, arrayOf(Matrix4f().translate(forwardPosition2).scale(0.1f)))
            }

            val rightHandMatrix = state.rightHandMatrix
            if (rightHandMatrix != null) {

                val shoulderMatrix = Matrix4f()
                    .translation(
                        headToShoulderX * state.regularRotation.cos - headToShoulderZ * state.regularRotation.sin,
                        headToShoulderY,
                        headToShoulderX * state.regularRotation.sin - headToShoulderZ * -state.regularRotation.cos
                    ).rotateY((Angle.degrees(270f) - state.regularRotation).radians)

                renderer.drawEntity(WING_MESH, createDragonWingMatrices(rightHandMatrix, shoulderMatrix, WING_PROPS))
            }

            val rightHandAimMatrix = state.rightHandAimMatrix
            if (rightHandAimMatrix != null) {
                val forwardPosition = rightHandAimMatrix.translate(0f, 0f, -1f, Matrix4f()).getTranslation(Vector3f())
                renderer.drawEntity(SkylandTestEntity.Renderer.MESH, arrayOf(Matrix4f().translate(forwardPosition).scale(0.1f)))

                val forwardPosition2 = rightHandAimMatrix.translate(0f, 0f, -5f, Matrix4f()).getTranslation(Vector3f())
                renderer.drawEntity(SkylandTestEntity.Renderer.MESH, arrayOf(Matrix4f().translate(forwardPosition2).scale(0.1f)))
            }
        }

        class Factory: EntityRendererFactory<MainMenuPlayerEntity> {
            override fun getEntityType() = MainMenuPlayerEntity::class

            override fun createRenderer(entity: MainMenuPlayerEntity) = Renderer()
        }

        companion object {
            val WING_PROPS = DragonWingProperties(
                baseWingLength = Distance.meters(0.25f),
                wingLaneWidth = Distance.meters(0.1f),
                wingTopLength = Distance.meters(0.125f),
                wingDepth = Distance.milliMeters(30),
                nailLength = Distance.meters(0.15f),
                nailWidth = Distance.meters(0.07f)
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
