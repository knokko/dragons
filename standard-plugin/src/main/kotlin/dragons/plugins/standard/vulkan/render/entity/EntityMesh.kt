package dragons.plugins.standard.vulkan.render.entity

import dragons.plugins.standard.vulkan.model.generator.ModelGenerator
import java.util.*

class EntityMesh(
    val generator: ModelGenerator,
    val colorImages: List<EntityColorImage>,
    val heightImages: List<EntityHeightImage>,
    val numTransformationMatrices: Int,
) {
    val id = UUID.randomUUID()!!
}
