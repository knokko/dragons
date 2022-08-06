package dragons.plugins.standard.vulkan.render.tile

import dragons.vulkan.memory.VulkanBuffer
import dragons.vulkan.memory.VulkanBufferRange

class ChunkTilesRenderEntry(
    val dynamicDescriptorSet: Long,
    val vertexBuffer: VulkanBuffer,
    val indexBuffer: VulkanBuffer,

    val indirectDrawIndex: Int,
    val indirectCountIndex: Int,
    val maxNumIndirectDrawCalls: Int,
) {

    var currentDrawCount = 0
}
