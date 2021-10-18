package dragons.plugins.debug.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.vulkan.VulkanStaticMemoryUser
import dragons.vulkan.memory.VulkanBufferRange
import dragons.vulkan.memory.claim.BufferMemoryClaim
import kotlinx.coroutines.CompletableDeferred
import org.lwjgl.system.MemoryUtil.memAddress
import org.lwjgl.vulkan.VK12.*

class TestStaticMemory: VulkanStaticMemoryUser {
    override fun claimStaticMemory(pluginInstance: PluginInstance, agent: VulkanStaticMemoryUser.Agent) {
        if (pluginInstance.gameInitProps.mainParameters.testParameters.staticMemory) {
            for (queueFamily in agent.queueManager.allQueueFamilies) {
                // TODO Await this
                val prefilledIndirectResult = CompletableDeferred<VulkanBufferRange>()
                agent.claims.buffers.add(
                    BufferMemoryClaim(
                        size = 200,
                        usageFlags = VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT,
                        dstAccessMask = VK_ACCESS_INDIRECT_COMMAND_READ_BIT,
                        queueFamily = queueFamily,
                        storeResult = prefilledIndirectResult
                    ) { byteBuffer ->
                        println("Prefilled indirect buffer start is ${memAddress(byteBuffer)}")
                        assert(byteBuffer.capacity() == 200)
                    }
                )

                val prefilledStorageResult = CompletableDeferred<VulkanBufferRange>()
            }
        }
    }
}