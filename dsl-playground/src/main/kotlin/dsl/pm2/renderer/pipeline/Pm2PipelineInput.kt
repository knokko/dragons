package dsl.pm2.renderer.pipeline

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo
import org.lwjgl.vulkan.VkVertexInputAttributeDescription
import org.lwjgl.vulkan.VkVertexInputBindingDescription

internal const val STATIC_VERTEX_SIZE = 24

internal fun createVertexInputState(stack: MemoryStack): VkPipelineVertexInputStateCreateInfo {
    val bindings = VkVertexInputBindingDescription.calloc(1, stack)
    val staticBinding = bindings[0]
    staticBinding.binding(0)
    staticBinding.stride(STATIC_VERTEX_SIZE)
    staticBinding.inputRate(VK_VERTEX_INPUT_RATE_VERTEX)

    val attributes = VkVertexInputAttributeDescription.calloc(3, stack)
    val attributePosition = attributes[0]
    attributePosition.location(0)
    attributePosition.binding(0)
    attributePosition.format(VK_FORMAT_R32G32_SFLOAT)
    attributePosition.offset(0)

    val attributeColor = attributes[1]
    attributeColor.location(1)
    attributeColor.binding(0)
    attributeColor.format(VK_FORMAT_R32G32B32_SFLOAT)
    attributeColor.offset(8)

    val attributeMatrix = attributes[2]
    attributeMatrix.location(2)
    attributeMatrix.binding(0)
    attributeMatrix.format(VK_FORMAT_R32_SINT)
    attributeMatrix.offset(20)

    val ciVertexInput = VkPipelineVertexInputStateCreateInfo.calloc(stack)
    ciVertexInput.`sType$Default`()
    ciVertexInput.flags(0)
    ciVertexInput.pVertexBindingDescriptions(bindings)
    ciVertexInput.pVertexAttributeDescriptions(attributes)

    return ciVertexInput
}

internal fun createInputAssemblyState(stack: MemoryStack): VkPipelineInputAssemblyStateCreateInfo {
    val inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
    inputAssembly.`sType$Default`()
    inputAssembly.flags(0)
    inputAssembly.topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
    inputAssembly.primitiveRestartEnable(false)

    return inputAssembly
}
