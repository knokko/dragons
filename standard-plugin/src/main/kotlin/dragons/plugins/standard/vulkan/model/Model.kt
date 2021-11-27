package dragons.plugins.standard.vulkan.model

import dragons.vulkan.memory.VulkanBufferRange

class Model(
    val vertices: VulkanBufferRange,
    val indices: VulkanBufferRange,
    val baseColorTextureIndex: Int,
    val numColorTextures: Int,
    val baseHeightTextureIndex: Int,
    val numHeightTextures: Int,
    val numTransformationMatrices: Int
) {
}