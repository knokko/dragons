package dragons.plugins.standard.state

import dragons.plugins.standard.vulkan.pipeline.BasicGraphicsPipeline
import dragons.vulkan.memory.VulkanBufferRange
import dragons.vulkan.memory.VulkanImage
import java.nio.ByteBuffer

class StandardGraphicsState(
    val basicGraphicsPipeline: BasicGraphicsPipeline,
    val basicRenderPass: Long,
    val basicLeftFramebuffer: Long,
    val basicRightFramebuffer: Long,
    val basicStaticDescriptorPool: Long,
    val basicDynamicDescriptorPool: Long,
    val basicStaticDescriptorSet: Long,
    val basicDynamicDescriptorSet: Long,
    val basicSampler: Long,
    val buffers: StandardGraphicsBuffers,

    val testColorImage: VulkanImage,
    val testHeightImage: VulkanImage,
)

class StandardGraphicsBuffers(
    val transformationMatrixDevice: VulkanBufferRange,
    val transformationMatrixStaging: VulkanBufferRange,
    val transformationMatrixHost: ByteBuffer,
    val cameraDevice: VulkanBufferRange,
    val cameraStaging: VulkanBufferRange,
    val cameraHost: ByteBuffer,
    val indirectDrawDevice: VulkanBufferRange,
    val indirectDrawHost: ByteBuffer,
    val indirectDrawCountDevice: VulkanBufferRange,
    val indirectDrawCountHost: ByteBuffer,
    val vertex: VulkanBufferRange,
    val index: VulkanBufferRange
)
