package graviks2d.pipeline.text

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import troll.exceptions.VulkanFailureException.assertVkSuccess
import troll.instance.TrollInstance

internal fun createTextCountPipelineVertexInput(
    stack: MemoryStack
): VkPipelineVertexInputStateCreateInfo {

    val bindings = VkVertexInputBindingDescription.calloc(1, stack)
    val binding = bindings[0]
    binding.binding(0)
    binding.stride(TextCountVertex.BYTE_SIZE)
    binding.inputRate(VK_VERTEX_INPUT_RATE_VERTEX)

    val attributes = VkVertexInputAttributeDescription.calloc(2, stack)
    val attributePosition = attributes[0]
    attributePosition.location(0)
    attributePosition.binding(0)
    attributePosition.format(VK_FORMAT_R32G32_SFLOAT)
    attributePosition.offset(TextCountVertex.OFFSET_X)

    val attributeOperationIndex = attributes[1]
    attributeOperationIndex.location(1)
    attributeOperationIndex.binding(0)
    attributeOperationIndex.format(VK_FORMAT_R32_SINT)
    attributeOperationIndex.offset(TextCountVertex.OFFSET_OPERATION)

    val ciVertexInput = VkPipelineVertexInputStateCreateInfo.calloc(stack)
    ciVertexInput.`sType$Default`()
    ciVertexInput.pVertexBindingDescriptions(bindings)
    ciVertexInput.pVertexAttributeDescriptions(attributes)

    return ciVertexInput
}

internal fun createTextOddPipelineVertexInput(
    stack: MemoryStack
): VkPipelineVertexInputStateCreateInfo {
    val bindings = VkVertexInputBindingDescription.calloc(1, stack)
    val binding = bindings[0]
    binding.binding(0)
    binding.stride(8)
    binding.inputRate(VK_VERTEX_INPUT_RATE_VERTEX)

    val attributes = VkVertexInputAttributeDescription.calloc(1, stack)
    val attributePosition = attributes[0]
    attributePosition.location(0)
    attributePosition.binding(0)
    attributePosition.format(VK_FORMAT_R32G32_SFLOAT)
    attributePosition.offset(0)

    val ciVertexInput = VkPipelineVertexInputStateCreateInfo.calloc(stack)
    ciVertexInput.`sType$Default`()
    ciVertexInput.pVertexBindingDescriptions(bindings)
    ciVertexInput.pVertexAttributeDescriptions(attributes)

    return ciVertexInput
}

internal fun createTextOddPipelineDescriptors(
    troll: TrollInstance, stack: MemoryStack, descriptorSetLayout: Long, countImageView: Long
): Pair<Long, Long> {
    val poolSizes = VkDescriptorPoolSize.calloc(1, stack)
    val poolSize = poolSizes[0]
    poolSize.type(VK_DESCRIPTOR_TYPE_INPUT_ATTACHMENT)
    poolSize.descriptorCount(1)

    val ciDescriptorPool = VkDescriptorPoolCreateInfo.calloc(stack)
    ciDescriptorPool.`sType$Default`()
    ciDescriptorPool.maxSets(1)
    ciDescriptorPool.pPoolSizes(poolSizes)

    val pDescriptorPool = stack.callocLong(1)
    assertVkSuccess(
        vkCreateDescriptorPool(troll.vkDevice(), ciDescriptorPool, null, pDescriptorPool),
        "vkCreateDescriptorPool", "GraviksTextDescriptorPool"
    )
    val descriptorPool = pDescriptorPool[0]

    val descriptorSet = troll.descriptors.allocate(
        stack, 1, descriptorPool, "GraviksTextOdd", descriptorSetLayout
    )[0]

    val descriptorImages = VkDescriptorImageInfo.calloc(1, stack)
    val descriptorImage = descriptorImages[0]
    descriptorImage.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
    descriptorImage.imageView(countImageView)
    descriptorImage.sampler(VK_NULL_HANDLE)

    val descriptorWrites = VkWriteDescriptorSet.calloc(1, stack)
    val descriptorWrite = descriptorWrites[0]
    descriptorWrite.`sType$Default`()
    descriptorWrite.dstSet(descriptorSet)
    descriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_INPUT_ATTACHMENT)
    descriptorWrite.descriptorCount(1)
    descriptorWrite.dstBinding(0)
    descriptorWrite.pImageInfo(descriptorImages)

    vkUpdateDescriptorSets(troll.vkDevice(), descriptorWrites, null)

    return Pair(descriptorPool, descriptorSet)
}