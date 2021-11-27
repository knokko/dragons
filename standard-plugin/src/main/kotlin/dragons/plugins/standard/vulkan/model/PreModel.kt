package dragons.plugins.standard.vulkan.model

import dragons.vulkan.memory.VulkanBufferRange
import kotlinx.coroutines.CompletableDeferred

class PreModel(
    val vertices: CompletableDeferred<VulkanBufferRange> = CompletableDeferred(),
    val indices: CompletableDeferred<VulkanBufferRange> = CompletableDeferred(),
    val baseColorTextureIndex: Int,
    val numColorTextures: Int,
    val baseHeightTextureIndex: Int,
    val numHeightTextures: Int,
    val numTransformationMatrices: Int
) {
    suspend fun await(): Model {
        return Model(
            vertices = this.vertices.await(),
            indices = this.indices.await(),
            baseColorTextureIndex = this.baseColorTextureIndex,
            numColorTextures = this.numColorTextures,
            baseHeightTextureIndex = this.baseHeightTextureIndex,
            numHeightTextures = this.numHeightTextures,
            numTransformationMatrices = this.numTransformationMatrices
        )
    }
}
