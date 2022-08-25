package dragons.plugins.standard.state

import dragons.plugins.standard.vulkan.pipeline.BasicGraphicsPipeline
import dragons.vulkan.memory.VulkanBufferRange
import kotlinx.coroutines.CompletableDeferred
import java.nio.ByteBuffer

class StandardPreGraphicsState {
    val basicGraphicsPipeline = CompletableDeferred<BasicGraphicsPipeline>()
    val basicRenderPass = CompletableDeferred<Long>()
    val basicStaticDescriptorPool = CompletableDeferred<Long>()
    val basicStaticDescriptorSet = CompletableDeferred<Long>()
    val basicSampler = CompletableDeferred<Long>()

    val transformationMatrixDeviceBuffer = CompletableDeferred<VulkanBufferRange>()
    val transformationMatrixStagingBuffer = CompletableDeferred<Pair<ByteBuffer, VulkanBufferRange>>()
    val cameraDeviceBuffer = CompletableDeferred<VulkanBufferRange>()
    val cameraStagingBuffer = CompletableDeferred<Pair<ByteBuffer, VulkanBufferRange>>()
    val indirectDrawingBuffer = CompletableDeferred<Pair<ByteBuffer, VulkanBufferRange>>()
}
