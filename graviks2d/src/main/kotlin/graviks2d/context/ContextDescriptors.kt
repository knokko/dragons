package graviks2d.context

import graviks2d.core.GraviksInstance
import graviks2d.util.assertSuccess
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

internal class ContextDescriptors(
    private val instance: GraviksInstance,
    private val operationBuffer: Long,
    private val textAtlasImageView: Long
) {

    private val descriptorPool: Long
    val descriptorSet: Long

    init {
        stackPush().use { stack ->

            val poolSizes = VkDescriptorPoolSize.calloc(4, stack)
            val sizeShaderStorage = poolSizes[0]
            sizeShaderStorage.type(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
            sizeShaderStorage.descriptorCount(1)
            val sizeTextureSampler = poolSizes[1]
            sizeTextureSampler.type(VK_DESCRIPTOR_TYPE_SAMPLER)
            sizeTextureSampler.descriptorCount(2)
            val sizeTextures = poolSizes[2]
            sizeTextures.type(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
            sizeTextures.descriptorCount(instance.maxNumDescriptorImages)
            val sizeTextTexture = poolSizes[3]
            sizeTextTexture.type(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
            sizeTextTexture.descriptorCount(1)

            val ciPool = VkDescriptorPoolCreateInfo.calloc(stack)
            ciPool.`sType$Default`()
            ciPool.maxSets(1)
            ciPool.pPoolSizes(poolSizes)

            val pPool = stack.callocLong(1)
            assertSuccess(
                vkCreateDescriptorPool(this.instance.troll.vkDevice(), ciPool, null, pPool),
                "vkCreateDescriptorPool"
            )
            this.descriptorPool = pPool[0]

            val aiSet = VkDescriptorSetAllocateInfo.calloc(stack)
            aiSet.`sType$Default`()
            aiSet.descriptorPool(this.descriptorPool)
            aiSet.pSetLayouts(stack.longs(this.instance.pipeline.vkDescriptorSetLayout))

            val pSet = stack.callocLong(1)
            assertSuccess(
                vkAllocateDescriptorSets(this.instance.troll.vkDevice(), aiSet, pSet),
                "vkAllocateDescriptorSets"
            )
            this.descriptorSet = pSet[0]
        }
        this.updateDescriptors(emptyArray())
    }

    fun updateDescriptors(imageViews: Array<Long>) {
        stackPush().use { stack ->
            val operationBufferDescriptors = VkDescriptorBufferInfo.calloc(1, stack)
            val operationBufferDescriptor = operationBufferDescriptors[0]
            operationBufferDescriptor.buffer(operationBuffer)
            operationBufferDescriptor.offset(0)
            operationBufferDescriptor.range(VK_WHOLE_SIZE)

            val textureSamplerDescriptors = VkDescriptorImageInfo.calloc(2, stack)
            textureSamplerDescriptors[0].sampler(instance.textureSampler)
            textureSamplerDescriptors[1].sampler(instance.smoothTextureSampler)

            val texturesDescriptors = VkDescriptorImageInfo.calloc(instance.maxNumDescriptorImages, stack)
            for (textureIndex in 0 until instance.maxNumDescriptorImages) {
                val textureDescriptor = texturesDescriptors[textureIndex]
                if (textureIndex < imageViews.size) {
                    textureDescriptor.imageView(imageViews[textureIndex])
                } else {
                    textureDescriptor.imageView(instance.dummyImage.vkImageView)
                }
                textureDescriptor.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
            }

            val textTextureDescriptors = VkDescriptorImageInfo.calloc(1, stack)
            textTextureDescriptors.imageView(textAtlasImageView)
            textTextureDescriptors.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)

            val writes = VkWriteDescriptorSet.calloc(4, stack)
            val operationWrite = writes[0]
            operationWrite.`sType$Default`()
            operationWrite.dstSet(this.descriptorSet)
            operationWrite.dstBinding(0)
            operationWrite.dstArrayElement(0)
            operationWrite.descriptorCount(1)
            operationWrite.descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
            operationWrite.pBufferInfo(operationBufferDescriptors)
            val textureSamplerWrite = writes[1]
            textureSamplerWrite.`sType$Default`()
            textureSamplerWrite.dstSet(this.descriptorSet)
            textureSamplerWrite.dstBinding(1)
            textureSamplerWrite.dstArrayElement(0)
            textureSamplerWrite.descriptorCount(2)
            textureSamplerWrite.descriptorType(VK_DESCRIPTOR_TYPE_SAMPLER)
            textureSamplerWrite.pImageInfo(textureSamplerDescriptors)
            val texturesWrite = writes[2]
            texturesWrite.`sType$Default`()
            texturesWrite.dstSet(this.descriptorSet)
            texturesWrite.dstBinding(2)
            texturesWrite.dstArrayElement(0)
            texturesWrite.descriptorCount(instance.maxNumDescriptorImages)
            texturesWrite.descriptorType(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
            texturesWrite.pImageInfo(texturesDescriptors)
            val textTextureWrite = writes[3]
            textTextureWrite.`sType$Default`()
            textTextureWrite.dstSet(this.descriptorSet)
            textTextureWrite.dstBinding(3)
            textTextureWrite.dstArrayElement(0)
            textTextureWrite.descriptorCount(1)
            textTextureWrite.descriptorType(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
            textTextureWrite.pImageInfo(textTextureDescriptors)

            vkUpdateDescriptorSets(this.instance.troll.vkDevice(), writes, null)
        }
    }

    fun destroy() {
        vkDestroyDescriptorPool(this.instance.troll.vkDevice(), this.descriptorPool, null)
    }
}
