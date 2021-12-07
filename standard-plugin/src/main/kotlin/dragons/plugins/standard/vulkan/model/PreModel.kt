package dragons.plugins.standard.vulkan.model

import dragons.vulkan.memory.VulkanBufferRange
import kotlinx.coroutines.CompletableDeferred

class PreModel(
    val vertices: CompletableDeferred<VulkanBufferRange> = CompletableDeferred(),
    val indices: CompletableDeferred<VulkanBufferRange> = CompletableDeferred(),
    val colorTextures: List<Int>,
    val heightTextures: List<Int>,
    val numTransformationMatrices: Int
) {
    suspend fun await(): Model {
        return Model(
            vertices = this.vertices.await(),
            indices = this.indices.await(),
            colorTextures = this.colorTextures,
            heightTextures = this.heightTextures,
            numTransformationMatrices = this.numTransformationMatrices
        )
    }
}
