package dragons.plugins.standard.vulkan

import dragons.plugin.interfaces.vulkan.VulkanStaticMemoryUser
import dragons.plugins.standard.state.StandardPluginState
import dragons.plugins.standard.vulkan.render.entity.EntityMeshTracker
import dragons.vulkan.memory.claim.BufferMemoryClaim
import dragons.vulkan.memory.claim.StagingBufferMemoryClaim
import knokko.plugin.PluginInstance
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkDrawIndexedIndirectCommand

const val MAX_NUM_TRANSFORMATION_MATRICES = 100_000
const val MAX_NUM_INDIRECT_DRAW_CALLS = 100_000
const val ENTITY_MESH_BUFFER_SIZE = 64_000_000

@Suppress("unused")
class StandardVulkanStaticMemory: VulkanStaticMemoryUser {

    override fun claimStaticMemory(pluginInstance: PluginInstance, agent: VulkanStaticMemoryUser.Agent) {
        val preGraphics = (pluginInstance.state as StandardPluginState).preGraphics

        // Camera buffers
        agent.claims.buffers.add(BufferMemoryClaim(
            // 2 4x4 float matrices
            size = 2 * 64,
            usageFlags = VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT or VK_BUFFER_USAGE_TRANSFER_DST_BIT,
            alignment = agent.vkPhysicalDeviceLimits.minUniformBufferOffsetAlignment().toInt(),
            queueFamily = agent.queueManager.generalQueueFamily,
            storeResult = preGraphics.cameraDeviceBuffer,
            prefill = null
        ))
        agent.claims.stagingBuffers.add(StagingBufferMemoryClaim(
            size = 2 * 64,
            // No special alignment is needed because the staging buffer is only used for copying
            alignment = 1,
            queueFamily = agent.queueManager.generalQueueFamily,
            storeResult = preGraphics.cameraStagingBuffer
        ))

        // Transformation matrix buffers
        agent.claims.buffers.add(BufferMemoryClaim(
            size = 4 * 16 * MAX_NUM_TRANSFORMATION_MATRICES,
            usageFlags = VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
            alignment = agent.vkPhysicalDeviceLimits.minStorageBufferOffsetAlignment().toInt(),
            queueFamily = agent.queueManager.generalQueueFamily,
            storeResult = preGraphics.transformationMatrixDeviceBuffer,
            prefill = null
        ))
        agent.claims.stagingBuffers.add(StagingBufferMemoryClaim(
            size = 4 * 16 * MAX_NUM_TRANSFORMATION_MATRICES,
            queueFamily = agent.queueManager.generalQueueFamily,
            // No special alignment is needed because the staging buffer is only used for copying
            alignment = 1,
            storeResult = preGraphics.transformationMatrixStagingBuffer
        ))

        // Entity mesh buffers
        agent.claims.buffers.add(BufferMemoryClaim(
            size = ENTITY_MESH_BUFFER_SIZE,
            usageFlags = VK_BUFFER_USAGE_VERTEX_BUFFER_BIT or VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
            alignment = EntityMeshTracker.ALIGNMENT.toInt(),
            queueFamily = agent.queueManager.generalQueueFamily,
            storeResult = preGraphics.entityMeshDeviceBuffer,
            prefill = null
        ))
        agent.claims.stagingBuffers.add(StagingBufferMemoryClaim(
            size = ENTITY_MESH_BUFFER_SIZE,
            // TODO Try the transfer queue family
            queueFamily = agent.queueManager.generalQueueFamily,
            alignment = EntityMeshTracker.ALIGNMENT.toInt(),
            storeResult = preGraphics.entityMeshStagingBuffer
        ))

        // Indirect drawing buffer
        agent.claims.stagingBuffers.add(StagingBufferMemoryClaim(
            size = VkDrawIndexedIndirectCommand.SIZEOF * MAX_NUM_INDIRECT_DRAW_CALLS,
            usageFlags = VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT,
            // vkCmdDrawIndexedIndirectCountKHR wants the offset to be a multiple of 4
            alignment = 4,
            queueFamily = agent.queueManager.generalQueueFamily,
            storeResult = preGraphics.indirectDrawingBuffer
        ))
    }
}
