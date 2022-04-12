package graviks2d.pipeline.text

import graviks2d.util.assertSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

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

internal fun createTextCountPipelineLayout(vkDevice: VkDevice, stack: MemoryStack): Long {

    val ciLayout = VkPipelineLayoutCreateInfo.calloc(stack)
    ciLayout.`sType$Default`()
    ciLayout.pSetLayouts(null)
    ciLayout.pPushConstantRanges(null)

    val pLayout = stack.callocLong(1)
    assertSuccess(
        vkCreatePipelineLayout(vkDevice, ciLayout, null, pLayout),
        "vkCreatePipelineLayout"
    )
    return pLayout[0]
}

internal fun createTextOddPipelineLayout(
    vkDevice: VkDevice, stack: MemoryStack
): Pair<Long, Long> {
    val setLayoutBindings = VkDescriptorSetLayoutBinding.calloc(1, stack)
    val setLayoutBinding = setLayoutBindings[0]
    setLayoutBinding.binding(0)
    setLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_INPUT_ATTACHMENT)
    setLayoutBinding.descriptorCount(1)
    setLayoutBinding.stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)

    val ciSetLayout = VkDescriptorSetLayoutCreateInfo.calloc(stack)
    ciSetLayout.`sType$Default`()
    ciSetLayout.pBindings(setLayoutBindings)

    val pSetLayout = stack.callocLong(1)
    assertSuccess(
        vkCreateDescriptorSetLayout(vkDevice, ciSetLayout, null, pSetLayout),
        "vkCreateDescriptorSetLayout"
    )
    val setLayout = pSetLayout[0]

    val ciPipelineLayout = VkPipelineLayoutCreateInfo.calloc(stack)
    ciPipelineLayout.`sType$Default`()
    ciPipelineLayout.pSetLayouts(pSetLayout)
    ciPipelineLayout.pPushConstantRanges(null)

    val pPipelineLayout = stack.callocLong(1)
    assertSuccess(
        vkCreatePipelineLayout(vkDevice, ciPipelineLayout, null, pPipelineLayout),
        "vkCreatePipelineLayout"
    )
    val pipelineLayout = pPipelineLayout[0]
    return Pair(pipelineLayout, setLayout)
}

internal fun createTextOddPipelineDescriptors(
    vkDevice: VkDevice, stack: MemoryStack, descriptorSetLayout: Long, countImageView: Long
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
    assertSuccess(
        vkCreateDescriptorPool(vkDevice, ciDescriptorPool, null, pDescriptorPool),
        "vkCreateDescriptorPool"
    )
    val descriptorPool = pDescriptorPool[0]

    val aiDescriptorSet = VkDescriptorSetAllocateInfo.calloc(stack)
    aiDescriptorSet.`sType$Default`()
    aiDescriptorSet.descriptorPool(descriptorPool)
    aiDescriptorSet.pSetLayouts(stack.longs(descriptorSetLayout))

    val pDescriptorSet = stack.callocLong(1)
    assertSuccess(
        vkAllocateDescriptorSets(vkDevice, aiDescriptorSet, pDescriptorSet),
        "vkAllocateDescriptorSets"
    )
    val descriptorSet = pDescriptorSet[0]

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

    vkUpdateDescriptorSets(vkDevice, descriptorWrites, null)

    return Pair(descriptorPool, descriptorSet)
}