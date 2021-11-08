package dragons.plugins.standard.state

import dragons.plugins.standard.vulkan.pipeline.BasicGraphicsPipeline
import dragons.vulkan.memory.VulkanBufferRange
import java.nio.ByteBuffer

class StandardGraphicsState(
    val basicGraphicsPipeline: BasicGraphicsPipeline,
    val basicRenderPass: Long,
    val basicLeftFramebuffer: Long,
    val basicRightFramebuffer: Long,

    val transformationMatrixDeviceBuffer: VulkanBufferRange,
    val transformationMatrixStagingBuffer: VulkanBufferRange,
    val transformationMatrixHostBuffer: ByteBuffer,
    val indirectDrawDeviceBuffer: VulkanBufferRange,
    val indirectDrawHostBuffer: ByteBuffer,
    val indirectDrawCountDeviceBuffer: VulkanBufferRange,
    val indirectDrawCountHostBuffer: ByteBuffer,
    val vertexBuffer: VulkanBufferRange,
    val indexBuffer: VulkanBufferRange
)
