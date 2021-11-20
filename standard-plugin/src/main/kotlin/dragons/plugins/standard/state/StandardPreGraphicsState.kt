package dragons.plugins.standard.state

import dragons.plugins.standard.vulkan.pipeline.BasicGraphicsPipeline
import dragons.vulkan.memory.VulkanBufferRange
import dragons.vulkan.memory.VulkanImage
import kotlinx.coroutines.CompletableDeferred
import java.nio.ByteBuffer

class StandardPreGraphicsState(
    val basicGraphicsPipeline: CompletableDeferred<BasicGraphicsPipeline> = CompletableDeferred(),
    val basicRenderPass: CompletableDeferred<Long> = CompletableDeferred(),
    val basicStaticDescriptorPool: CompletableDeferred<Long> = CompletableDeferred(),
    val basicDynamicDescriptorPool: CompletableDeferred<Long> = CompletableDeferred(),
    val basicStaticDescriptorSet: CompletableDeferred<Long> = CompletableDeferred(),
    val basicDynamicDescriptorSet: CompletableDeferred<Long> = CompletableDeferred(),
    val basicSampler: CompletableDeferred<Long> = CompletableDeferred(),

    val transformationMatrixDeviceBuffer: CompletableDeferred<VulkanBufferRange> = CompletableDeferred(),
    val transformationMatrixStagingBuffer: CompletableDeferred<Pair<ByteBuffer, VulkanBufferRange>> = CompletableDeferred(),
    val cameraDeviceBuffer: CompletableDeferred<VulkanBufferRange> = CompletableDeferred(),
    val cameraStagingBuffer: CompletableDeferred<Pair<ByteBuffer, VulkanBufferRange>> = CompletableDeferred(),
    val indirectDrawingBuffer: CompletableDeferred<Pair<ByteBuffer, VulkanBufferRange>> = CompletableDeferred(),
    val indirectDrawCountBuffer: CompletableDeferred<Pair<ByteBuffer, VulkanBufferRange>> = CompletableDeferred(),
    val vertexBuffer: CompletableDeferred<VulkanBufferRange> = CompletableDeferred(),
    val indexBuffer: CompletableDeferred<VulkanBufferRange> = CompletableDeferred(),

    val testColorImage: CompletableDeferred<VulkanImage> = CompletableDeferred(),
    val testHeightImage: CompletableDeferred<VulkanImage> = CompletableDeferred()
)
