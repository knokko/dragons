package dragons.plugins.standard.vulkan.render.chunk

import dragons.plugins.standard.state.StandardGraphicsState
import dragons.plugins.standard.vulkan.pipeline.createBasicDynamicDescriptorPool
import dragons.state.StaticGraphicsState
import dragons.vulkan.util.assertVkSuccess
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.vkAllocateDescriptorSets
import org.lwjgl.vulkan.VK10.vkDestroyDescriptorPool
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo
import java.util.concurrent.ArrayBlockingQueue

internal class ChunkLoaderDescriptors(
    private val graphicsState: StaticGraphicsState,
    private val pluginGraphics: StandardGraphicsState
) {
    private val descriptorPool = createBasicDynamicDescriptorPool(graphicsState.vkDevice, CAPACITY)
    private val descriptorSets = ArrayBlockingQueue<Long>(CAPACITY)

    init {
        stackPush().use { stack ->

            val pSetLayouts = stack.callocLong(CAPACITY)
            for (index in 0 until CAPACITY) {
                pSetLayouts.put(index, pluginGraphics.basicGraphicsPipeline.dynamicDescriptorSetLayout)
            }

            val aiDescriptorSets = VkDescriptorSetAllocateInfo.calloc(stack)
            aiDescriptorSets.`sType$Default`()
            aiDescriptorSets.descriptorPool(this.descriptorPool)
            aiDescriptorSets.pSetLayouts(pSetLayouts)

            val pDescriptorSets = stack.callocLong(CAPACITY)
            assertVkSuccess(
                vkAllocateDescriptorSets(graphicsState.vkDevice, aiDescriptorSets, pDescriptorSets),
                "AllocateDescriptorSets", "standard plug-in: ChunkLoaderDescriptors"
            )

            for (index in 0 until CAPACITY) {
                this.descriptorSets.add(pDescriptorSets[index])
            }
        }
    }

    fun borrowDescriptorSet() = this.descriptorSets.take()

    fun returnDescriptorSet(descriptorSet: Long) = this.descriptorSets.add(descriptorSet)

    fun destroy() {
        vkDestroyDescriptorPool(graphicsState.vkDevice, this.descriptorPool, null)
    }

    companion object {
        // This would allow loading 300 non-empty chunks at the same time, which should be sufficient
        private const val CAPACITY = 300
    }
}
