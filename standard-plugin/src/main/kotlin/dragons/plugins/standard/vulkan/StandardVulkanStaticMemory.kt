package dragons.plugins.standard.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.vulkan.VulkanStaticMemoryUser
import dragons.vulkan.memory.claim.UninitializedBufferMemoryClaim
import org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT
import org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT
import org.lwjgl.vulkan.VkDrawIndexedIndirectCommand

const val MAX_NUM_TRANSFORMATION_MATRICES = 100_000
const val MAX_NUM_INDIRECT_DRAW_CALLS = 100_000

class StandardVulkanStaticMemory: VulkanStaticMemoryUser {
    override fun claimStaticMemory(pluginInstance: PluginInstance, agent: VulkanStaticMemoryUser.Agent) {
        // TODO Claim vertex buffer and index buffer

        // Transformation matrix buffer
        agent.uninitializedBuffers.add(UninitializedBufferMemoryClaim(
            4 * 16 * MAX_NUM_TRANSFORMATION_MATRICES, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT
        ))
        // Indirect drawing buffer
        agent.uninitializedBuffers.add(UninitializedBufferMemoryClaim(
            VkDrawIndexedIndirectCommand.SIZEOF * MAX_NUM_INDIRECT_DRAW_CALLS, VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT
        ))

        // TODO Also claim persistent staging memory for the transformation matrices and indirect draw calls
    }
}