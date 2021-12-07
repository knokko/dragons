package dragons.plugins.standard.vulkan.model

import dragons.vulkan.memory.VulkanBufferRange

class Model(
    val vertices: VulkanBufferRange,
    val indices: VulkanBufferRange,
    val colorTextures: List<Int>,
    val heightTextures: List<Int>,
    val numTransformationMatrices: Int
) {
}