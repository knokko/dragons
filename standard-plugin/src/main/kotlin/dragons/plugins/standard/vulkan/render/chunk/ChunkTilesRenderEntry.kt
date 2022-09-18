package dragons.plugins.standard.vulkan.render.chunk

import dragons.vulkan.memory.VulkanBuffer

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
