package dragons.plugins.standard.world.entity

import dragons.plugins.standard.vulkan.model.generator.generateSkylandModel
import dragons.plugins.standard.vulkan.render.StandardSceneRenderer
import dragons.plugins.standard.vulkan.render.entity.*
import dragons.space.Position
import dragons.world.entity.Entity
import dragons.world.entity.EntityProperties
import dragons.world.entity.EntityState
import org.joml.Matrix4f

class SkylandTestEntity: EntityProperties() {
    override fun getPersistentClassID() = "standard-plugin:SkylandTestEntity"

    class State(position: Position): EntityState(position)

    @Suppress("unused")
    class Renderer: EntityRenderer {
        override fun render(renderer: StandardSceneRenderer, entity: Entity, cameraPosition: Position) {
            val renderPosition = entity.copyState().position - cameraPosition
            val transformationMatrix = Matrix4f()
                .translate(renderPosition.x.meters, renderPosition.y.meters, renderPosition.z.meters)
                .scale(10f)
            renderer.drawEntity(MESH, arrayOf(transformationMatrix))
        }

        class Factory: EntityRendererFactory<SkylandTestEntity> {
            override fun getEntityType() = SkylandTestEntity::class

            override fun createRenderer(entity: SkylandTestEntity) = Renderer()
        }

        companion object {
            private val MESH = EntityMesh(
                generator = generateSkylandModel { 0.5f },
                colorImages = listOf(
                    ClasspathEntityColorImage("dragons/plugins/standard/images/testTerrain.jpg", 1024, 1024)
                ),
                heightImages = listOf(
                    ClasspathEntityHeightImage("dragons/plugins/standard/images/testTerrainHeight.png", 0.001f, 128, 128)
                ),
                numTransformationMatrices = 1
            )
        }
    }
}