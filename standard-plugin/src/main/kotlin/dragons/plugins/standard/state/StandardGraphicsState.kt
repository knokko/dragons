package dragons.plugins.standard.state

import dragons.plugins.standard.vulkan.pipeline.BasicGraphicsPipeline
import dragons.vulkan.memory.VulkanBufferRange
import java.nio.ByteBuffer

class StandardGraphicsState(
    val basicGraphicsPipeline: BasicGraphicsPipeline,
    val basicRenderPass: Long,
    val basicLeftFramebuffer: Long,
    val basicRightFramebuffer: Long,
    val basicStaticDescriptorPool: Long,
    val basicStaticDescriptorSet: Long,
    val basicSampler: Long,
    val buffers: StandardGraphicsBuffers
)

class StandardGraphicsBuffers(
    val transformationMatrixDevice: VulkanBufferRange,
    val transformationMatrixStaging: VulkanBufferRange,
    val transformationMatrixHost: ByteBuffer,
    val entityMeshDevice: VulkanBufferRange,
    val entityMeshStaging: VulkanBufferRange,
    val entityMeshHost: ByteBuffer,
    val cameraDevice: VulkanBufferRange,
    val cameraStaging: VulkanBufferRange,
    val cameraHost: ByteBuffer,
    val indirectDrawDevice: VulkanBufferRange,
    val indirectDrawHost: ByteBuffer
)
