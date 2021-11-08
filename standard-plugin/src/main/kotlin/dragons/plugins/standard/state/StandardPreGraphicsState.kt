package dragons.plugins.standard.state

import dragons.plugins.standard.vulkan.pipeline.BasicGraphicsPipeline
import dragons.vulkan.memory.VulkanBufferRange
import kotlinx.coroutines.CompletableDeferred
import java.nio.ByteBuffer

class StandardPreGraphicsState(
    val basicGraphicsPipeline: CompletableDeferred<BasicGraphicsPipeline> = CompletableDeferred(),
    val basicRenderPass: CompletableDeferred<Long> = CompletableDeferred(),

    val transformationMatrixDeviceBuffer: CompletableDeferred<VulkanBufferRange> = CompletableDeferred(),
    val transformationMatrixStagingBuffer: CompletableDeferred<Pair<ByteBuffer, VulkanBufferRange>> = CompletableDeferred(),
    val indirectDrawingBuffer: CompletableDeferred<Pair<ByteBuffer, VulkanBufferRange>> = CompletableDeferred(),
    val indirectDrawCountBuffer: CompletableDeferred<Pair<ByteBuffer, VulkanBufferRange>> = CompletableDeferred(),
    val vertexBuffer: CompletableDeferred<VulkanBufferRange> = CompletableDeferred(),
    val indexBuffer: CompletableDeferred<VulkanBufferRange> = CompletableDeferred()
)
