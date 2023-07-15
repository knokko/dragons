package graviks2d.pipeline

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

    val attributes = VkVertexInputAttributeDescription.calloc(3, stack)
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

    val attributeScissorIndex = attributes[2]
    attributeScissorIndex.location(2)
    attributeScissorIndex.binding(0)
    attributeScissorIndex.format(VK_FORMAT_R32_SINT)
    attributeScissorIndex.offset(GraviksVertex.OFFSET_SCISSOR_INDEX)

    val ciVertexInput = VkPipelineVertexInputStateCreateInfo.calloc(stack)
    ciVertexInput.`sType$Default`()
    ciVertexInput.pVertexBindingDescriptions(bindings)
    ciVertexInput.pVertexAttributeDescriptions(attributes)

    return ciVertexInput
}
