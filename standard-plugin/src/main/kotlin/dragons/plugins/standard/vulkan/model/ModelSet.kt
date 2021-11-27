package dragons.plugins.standard.vulkan.model

import dragons.plugins.standard.vulkan.pipeline.MAX_NUM_DESCRIPTOR_IMAGES
import dragons.vulkan.memory.VulkanBuffer

class ModelSet(
    models: Collection<Model>
) {
    val vertexBuffer: VulkanBuffer
    val indexBuffer: VulkanBuffer

    init {
        if (models.isEmpty()) throw IllegalArgumentException("A ModelSet needs at least 1 Model")
        this.vertexBuffer = models.first().vertices.buffer
        this.indexBuffer = models.first().indices.buffer

        for (model in models) {
            if (model.vertices.buffer != this.vertexBuffer) {
                throw IllegalArgumentException("All models in a ModelSet must use the same vertex buffer")
            }
            if (model.indices.buffer != this.indexBuffer) {
                throw IllegalArgumentException("All models in a ModelSet must use the same index buffer")
            }
            if (model.baseColorTextureIndex + model.numColorTextures > MAX_NUM_DESCRIPTOR_IMAGES) {
                throw IllegalArgumentException("Can't use more than $MAX_NUM_DESCRIPTOR_IMAGES color images")
            }
            if (model.baseHeightTextureIndex + model.numHeightTextures > MAX_NUM_DESCRIPTOR_IMAGES) {
                throw IllegalArgumentException("Can't use more than $MAX_NUM_DESCRIPTOR_IMAGES height images")
            }
        }
    }
}
