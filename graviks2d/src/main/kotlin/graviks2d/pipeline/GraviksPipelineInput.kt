package graviks2d.pipeline

import graviks2d.util.assertSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

internal fun createGraviksPipelineVertexInput(
    stack: MemoryStack
): VkPipelineVertexInputStateCreateInfo {

    val bindings = VkVertexInputBindingDescription.calloc(1, stack)
    val binding = bindings[0]
    binding.binding(0)
    binding.stride(GraviksVertex.BYTE_SIZE)
    binding.inputRate(VK_VERTEX_INPUT_RATE_VERTEX)

    val attributes = VkVertexInputAttributeDescription.calloc(2, stack)
    val attributePosition = attributes[0]
    attributePosition.location(0)
    attributePosition.binding(0)
    attributePosition.format(VK_FORMAT_R32G32_SFLOAT)
    attributePosition.offset(GraviksVertex.OFFSET_X)

    val attributeOperationIndex = attributes[1]
    attributeOperationIndex.location(1)
    attributeOperationIndex.binding(0)
    attributeOperationIndex.format(VK_FORMAT_R32_SINT)
    attributeOperationIndex.offset(GraviksVertex.OFFSET_OPERATION_INDEX)

    val ciVertexInput = VkPipelineVertexInputStateCreateInfo.calloc(stack)
    ciVertexInput.`sType$Default`()
    ciVertexInput.pVertexBindingDescriptions(bindings)
    ciVertexInput.pVertexAttributeDescriptions(attributes)

    return ciVertexInput
}

internal fun createGraviksPipelineInputAssembly(
    stack: MemoryStack
): VkPipelineInputAssemblyStateCreateInfo {

    val ciInputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
    ciInputAssembly.`sType$Default`()
    ciInputAssembly.topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
    ciInputAssembly.primitiveRestartEnable(false)

    return ciInputAssembly
}

internal fun createGraviksPipelineLayout(
    vkDevice: VkDevice, stack: MemoryStack, maxNumDescriptorImages: Int
): Pair<Long, Long> {

    val setLayoutBindings = VkDescriptorSetLayoutBinding.calloc(4, stack)
    val shaderStorageBinding = setLayoutBindings[0]
    shaderStorageBinding.binding(0)
    shaderStorageBinding.descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
    shaderStorageBinding.descriptorCount(1)
    shaderStorageBinding.stageFlags(VK_SHADER_STAGE_VERTEX_BIT or VK_SHADER_STAGE_FRAGMENT_BIT)
    val textureSamplerBinding = setLayoutBindings[1]
    textureSamplerBinding.binding(1)
    textureSamplerBinding.descriptorType(VK_DESCRIPTOR_TYPE_SAMPLER)
    textureSamplerBinding.descriptorCount(2)
    textureSamplerBinding.stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
    val texturesBinding = setLayoutBindings[2]
    texturesBinding.binding(2)
    texturesBinding.descriptorType(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
    texturesBinding.descriptorCount(maxNumDescriptorImages)
    texturesBinding.stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
    val textTextureBinding = setLayoutBindings[3]
    textTextureBinding.binding(3)
    textTextureBinding.descriptorType(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
    textTextureBinding.descriptorCount(1)
    textTextureBinding.stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)

    val ciSetLayout = VkDescriptorSetLayoutCreateInfo.calloc(stack)
    ciSetLayout.`sType$Default`()
    ciSetLayout.pBindings(setLayoutBindings)

    val pSetLayout = stack.callocLong(1)
    assertSuccess(
        vkCreateDescriptorSetLayout(vkDevice, ciSetLayout, null, pSetLayout),
        "CreateDescriptorSetLayout"
    )

    val ciLayout = VkPipelineLayoutCreateInfo.calloc(stack)
    ciLayout.`sType$Default`()
    ciLayout.pSetLayouts(pSetLayout)

    val pLayout = stack.callocLong(1)
    assertSuccess(
        vkCreatePipelineLayout(vkDevice, ciLayout, null, pLayout),
        "vkCreatePipelineLayout"
    )
    return Pair(pLayout[0], pSetLayout[0])
}
