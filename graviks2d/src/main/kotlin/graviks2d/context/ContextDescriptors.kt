package graviks2d.context

import graviks2d.core.GraviksInstance
import graviks2d.util.assertSuccess
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

internal class ContextDescriptors(
    private val instance: GraviksInstance,
    operationBuffer: Long
) {

    private val descriptorPool: Long
    val descriptorSet: Long

    init {
        stackPush().use { stack ->

            val poolSizes = VkDescriptorPoolSize.calloc(1, stack)
            val poolSize = poolSizes[0]
            poolSize.type(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
            poolSize.descriptorCount(1)

            val ciPool = VkDescriptorPoolCreateInfo.calloc(stack)
            ciPool.`sType$Default`()
            ciPool.maxSets(1)
            ciPool.pPoolSizes(poolSizes)

            val pPool = stack.callocLong(1)
            assertSuccess(
                vkCreateDescriptorPool(this.instance.device, ciPool, null, pPool),
                "vkCreateDescriptorPool"
            )
            this.descriptorPool = pPool[0]

            val aiSet = VkDescriptorSetAllocateInfo.calloc(stack)
            aiSet.`sType$Default`()
            aiSet.descriptorPool(this.descriptorPool)
            aiSet.pSetLayouts(stack.longs(this.instance.pipeline.vkDescriptorSetLayout))

            val pSet = stack.callocLong(1)
            assertSuccess(
                vkAllocateDescriptorSets(this.instance.device, aiSet, pSet),
                "vkAllocateDescriptorSets"
            )
            this.descriptorSet = pSet[0]

            val operationBufferWrites = VkDescriptorBufferInfo.calloc(1, stack)
            val operationBufferWrite = operationBufferWrites[0]
            operationBufferWrite.buffer(operationBuffer)
            operationBufferWrite.offset(0)
            operationBufferWrite.range(VK_WHOLE_SIZE)

            val writes = VkWriteDescriptorSet.calloc(1, stack)
            val write = writes[0]
            write.`sType$Default`()
            write.dstSet(this.descriptorSet)
            write.dstBinding(0)
            write.dstArrayElement(0)
            write.descriptorCount(1)
            write.descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
            write.pBufferInfo(operationBufferWrites)

            vkUpdateDescriptorSets(this.instance.device, writes, null)
        }
    }

    fun destroy() {
        vkDestroyDescriptorPool(this.instance.device, this.descriptorPool, null)
    }
}
