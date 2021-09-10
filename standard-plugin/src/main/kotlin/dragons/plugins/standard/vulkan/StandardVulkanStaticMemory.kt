package dragons.plugins.standard.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.vulkan.VulkanStaticMemoryUser
import dragons.plugins.standard.vulkan.vertex.BasicVertex
import dragons.vulkan.memory.VulkanBufferRange
import dragons.vulkan.memory.claim.PrefilledBufferMemoryClaim
import dragons.vulkan.memory.claim.StagingBufferMemoryClaim
import dragons.vulkan.memory.claim.UninitializedBufferMemoryClaim
import kotlinx.coroutines.CompletableDeferred
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkDrawIndexedIndirectCommand
import java.nio.ByteBuffer

const val MAX_NUM_TRANSFORMATION_MATRICES = 100_000
const val MAX_NUM_INDIRECT_DRAW_CALLS = 100_000

class StandardVulkanStaticMemory: VulkanStaticMemoryUser {

    private val getTransformationMatrixDeviceBuffer = CompletableDeferred<VulkanBufferRange>()
    private val getIndirectDrawingDeviceBuffer = CompletableDeferred<VulkanBufferRange>()

    private val getTransformationMatrixStagingBuffer = CompletableDeferred<Pair<ByteBuffer, VulkanBufferRange>>()
    private val getIndirectDrawingStagingBuffer = CompletableDeferred<Pair<ByteBuffer, VulkanBufferRange>>()

    private val getVertexBuffer = CompletableDeferred<VulkanBufferRange>()
    private val getIndexBuffer = CompletableDeferred<VulkanBufferRange>()

    override fun claimStaticMemory(pluginInstance: PluginInstance, agent: VulkanStaticMemoryUser.Agent) {

        val vertexFrequency = 10

        // Testing terrain vertex buffer
        agent.prefilledBuffers.add(PrefilledBufferMemoryClaim(
            BasicVertex.SIZE * vertexFrequency * vertexFrequency,
            VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
            agent.queueManager.generalQueueFamily,
            getVertexBuffer
        ) { destBuffer ->
            val vertices = BasicVertex.createArray(destBuffer, 0, (vertexFrequency * vertexFrequency).toLong())
            for (indexZ in 0 until vertexFrequency) {
                for (indexX in 0 until vertexFrequency) {
                    val vertexIndex = indexX + vertexFrequency * indexZ
                    val x = indexX.toFloat() / (vertexFrequency - 1).toFloat()
                    val z = indexZ.toFloat() / (vertexFrequency - 1).toFloat()

                    val vertex = vertices[vertexIndex]
                    vertex.position.x = x
                    vertex.position.y = 0f
                    vertex.position.z = z
                    vertex.normal.x = 0f
                    vertex.normal.y = 1f
                    vertex.normal.z = 0f
                    vertex.colorTextureCoordinates.x = x
                    vertex.colorTextureCoordinates.y = z
                    vertex.heightTextureCoordinates.x = x
                    vertex.heightTextureCoordinates.y = z
                    vertex.matrixIndex = 0
                    vertex.materialIndex = BasicVertex.MATERIAL_TERRAIN
                    vertex.deltaFactor = 1f
                }
            }
        })

        // Testing terrain index buffer
        agent.prefilledBuffers.add(PrefilledBufferMemoryClaim(
            4 * 6 * (vertexFrequency - 1) * (vertexFrequency - 1),
            VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
            agent.queueManager.generalQueueFamily,
            getIndexBuffer
        ) { destBuffer ->
            for (lowZ in 0 until vertexFrequency - 1) {
                val highZ = lowZ + 1
                for (lowX in 0 until vertexFrequency - 1) {
                    val highX = lowX + 1

                    val indexLL = lowX + vertexFrequency * lowZ
                    val indexHL = highX + vertexFrequency * lowZ
                    val indexLH = lowX + vertexFrequency * highZ
                    val indexHH = highX + vertexFrequency * highZ

                    destBuffer.putInt(indexLH)
                    destBuffer.putInt(indexHH)
                    destBuffer.putInt(indexHL)

                    destBuffer.putInt(indexHL)
                    destBuffer.putInt(indexLL)
                    destBuffer.putInt(indexLH)
                }
            }
        })

        // Transformation matrix buffer
        agent.uninitializedBuffers.add(UninitializedBufferMemoryClaim(
            4 * 16 * MAX_NUM_TRANSFORMATION_MATRICES,
            VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
            agent.queueManager.generalQueueFamily,
            getTransformationMatrixDeviceBuffer
        ))
        agent.stagingBuffers.add(StagingBufferMemoryClaim(
            4 * 16 * MAX_NUM_TRANSFORMATION_MATRICES,
                agent.queueManager.generalQueueFamily,
                getTransformationMatrixStagingBuffer
        ))

        // Indirect drawing buffer
        agent.uninitializedBuffers.add(UninitializedBufferMemoryClaim(
            VkDrawIndexedIndirectCommand.SIZEOF * MAX_NUM_INDIRECT_DRAW_CALLS,
            VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT,
            agent.queueManager.generalQueueFamily,
            getIndirectDrawingDeviceBuffer
        ))
        agent.stagingBuffers.add(
            StagingBufferMemoryClaim(
            VkDrawIndexedIndirectCommand.SIZEOF * MAX_NUM_INDIRECT_DRAW_CALLS,
                agent.queueManager.generalQueueFamily,
                getIndirectDrawingStagingBuffer
        ))
    }
}
