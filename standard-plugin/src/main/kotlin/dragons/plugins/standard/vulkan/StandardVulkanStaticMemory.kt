package dragons.plugins.standard.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.vulkan.VulkanStaticMemoryUser
import dragons.plugins.standard.state.StandardPluginState
import dragons.plugins.standard.vulkan.vertex.BasicVertex
import dragons.vulkan.memory.VulkanBufferRange
import dragons.vulkan.memory.claim.BufferMemoryClaim
import dragons.vulkan.memory.claim.StagingBufferMemoryClaim
import kotlinx.coroutines.CompletableDeferred
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkDrawIndexedIndirectCommand
import java.nio.ByteBuffer

const val MAX_NUM_TRANSFORMATION_MATRICES = 100_000
const val MAX_NUM_INDIRECT_DRAW_CALLS = 100_000

class StandardVulkanStaticMemory: VulkanStaticMemoryUser {

    override fun claimStaticMemory(pluginInstance: PluginInstance, agent: VulkanStaticMemoryUser.Agent) {

        val preGraphics = (pluginInstance.state as StandardPluginState).preGraphics
        val vertexFrequency = 10

        // Testing terrain vertex buffer
        agent.claims.buffers.add(BufferMemoryClaim(
            size = BasicVertex.SIZE * vertexFrequency * vertexFrequency,
            usageFlags = VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
            dstAccessMask = VK_ACCESS_VERTEX_ATTRIBUTE_READ_BIT,
            dstPipelineStageMask = VK_PIPELINE_STAGE_VERTEX_INPUT_BIT,
            queueFamily = agent.queueManager.generalQueueFamily,
            storeResult = preGraphics.vertexBuffer
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
        agent.claims.buffers.add(BufferMemoryClaim(
            size = 4 * 6 * (vertexFrequency - 1) * (vertexFrequency - 1),
            usageFlags = VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
            dstAccessMask = VK_ACCESS_INDEX_READ_BIT,
            dstPipelineStageMask = VK_PIPELINE_STAGE_VERTEX_INPUT_BIT,
            queueFamily = agent.queueManager.generalQueueFamily,
            storeResult = preGraphics.indexBuffer
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

        // Transformation matrix buffers
        agent.claims.buffers.add(BufferMemoryClaim(
            size = 4 * 16 * MAX_NUM_TRANSFORMATION_MATRICES,
            usageFlags = VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
            queueFamily = agent.queueManager.generalQueueFamily,
            storeResult = preGraphics.transformationMatrixDeviceBuffer,
            prefill = null
        ))
        agent.claims.stagingBuffers.add(StagingBufferMemoryClaim(
            size = 4 * 16 * MAX_NUM_TRANSFORMATION_MATRICES,
            queueFamily = agent.queueManager.generalQueueFamily,
            storeResult = preGraphics.transformationMatrixStagingBuffer
        ))

        // Indirect drawing buffer
        agent.claims.stagingBuffers.add(StagingBufferMemoryClaim(
            size = VkDrawIndexedIndirectCommand.SIZEOF * MAX_NUM_INDIRECT_DRAW_CALLS,
            queueFamily = agent.queueManager.generalQueueFamily,
            storeResult = preGraphics.indirectDrawingBuffer
        ))
        agent.claims.stagingBuffers.add(StagingBufferMemoryClaim(
            size = 4, queueFamily = agent.queueManager.generalQueueFamily, storeResult = preGraphics.indirectDrawCountBuffer
        ))
    }
}
