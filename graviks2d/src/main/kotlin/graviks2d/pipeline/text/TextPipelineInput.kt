package graviks2d.pipeline.text

import graviks2d.util.assertSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

internal fun createTextPipelineVertexInput(
    stack: MemoryStack
): VkPipelineVertexInputStateCreateInfo {

    val bindings = VkVertexInputBindingDescription.calloc(1, stack)
    val binding = bindings[0]
    binding.binding(0)
    binding.stride(TextVertex.BYTE_SIZE)
    binding.inputRate(VK_VERTEX_INPUT_RATE_VERTEX)

    val attributes = VkVertexInputAttributeDescription.calloc(2, stack)
    val attributePosition = attributes[0]
    attributePosition.location(0)
    attributePosition.binding(0)
    attributePosition.format(VK_FORMAT_R32G32_SFLOAT)
    attributePosition.offset(TextVertex.OFFSET_X)

    val attributeOperationIndex = attributes[1]
    attributeOperationIndex.location(1)
    attributeOperationIndex.binding(0)
    attributeOperationIndex.format(VK_FORMAT_R32_SINT)
    attributeOperationIndex.offset(TextVertex.OFFSET_OPERATION)

    val ciVertexInput = VkPipelineVertexInputStateCreateInfo.calloc(stack)
    ciVertexInput.`sType$Default`()
    ciVertexInput.pVertexBindingDescriptions(bindings)
    ciVertexInput.pVertexAttributeDescriptions(attributes)

    return ciVertexInput
}

internal fun createTextPipelineLayout(vkDevice: VkDevice, stack: MemoryStack): Long {

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
