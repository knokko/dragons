package dragons.plugins.standard.vulkan.pipeline

import dragons.plugins.standard.vulkan.vertex.BasicVertex
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.*

fun createBasicVertexInputState(stack: MemoryStack): VkPipelineVertexInputStateCreateInfo {

    val bindings = VkVertexInputBindingDescription.calloc(1, stack)
    val binding = bindings[0]
    binding.binding(0)
    binding.stride(BasicVertex.SIZE)
    binding.inputRate(VK_VERTEX_INPUT_RATE_VERTEX)

    val attributes = VkVertexInputAttributeDescription.calloc(9, stack)
    val attributePosition = attributes[0]
    attributePosition.location(0)
    attributePosition.binding(0)
    attributePosition.format(VK_FORMAT_R32G32B32_SFLOAT)
    attributePosition.offset(BasicVertex.OFFSET_BASE_POSITION)

    val attributeNormal = attributes[1]
    attributeNormal.location(1)
    attributeNormal.binding(0)
    attributeNormal.format(VK_FORMAT_R32G32B32_SFLOAT)
    attributeNormal.offset(BasicVertex.OFFSET_BASE_NORMAL)

    val attributeColorTexCoordinates = attributes[2]
    attributeColorTexCoordinates.location(2)
    attributeColorTexCoordinates.binding(0)
    attributeColorTexCoordinates.format(VK_FORMAT_R32G32_SFLOAT)
    attributeColorTexCoordinates.offset(BasicVertex.OFFSET_COLOR_TEXTURE_COORDINATES)

    val attributeHeightTexCoordinates = attributes[3]
    attributeHeightTexCoordinates.location(3)
    attributeHeightTexCoordinates.binding(0)
    attributeHeightTexCoordinates.format(VK_FORMAT_R32G32_SFLOAT)
    attributeHeightTexCoordinates.offset(BasicVertex.OFFSET_HEIGHT_TEXTURE_COORDINATES)

    val attributeMatrixIndex = attributes[4]
    attributeMatrixIndex.location(4)
    attributeMatrixIndex.binding(0)
    attributeMatrixIndex.format(VK_FORMAT_R32_SINT)
    attributeMatrixIndex.offset(BasicVertex.OFFSET_MATRIX_INDEX)

    val attributeMaterialIndex = attributes[5]
    attributeMaterialIndex.location(5)
    attributeMaterialIndex.binding(0)
    attributeMaterialIndex.format(VK_FORMAT_R32_SINT)
    attributeMaterialIndex.offset(BasicVertex.OFFSET_MATERIAL_INDEX)

    val attributeDeltaFactor = attributes[6]
    attributeDeltaFactor.location(6)
    attributeDeltaFactor.binding(0)
    attributeDeltaFactor.format(VK_FORMAT_R32G32_SFLOAT)
    attributeDeltaFactor.offset(BasicVertex.OFFSET_DELTA_FACTOR)

    val attributeColorTextureIndex = attributes[7]
    attributeColorTextureIndex.location(7)
    attributeColorTextureIndex.binding(0)
    attributeColorTextureIndex.format(VK_FORMAT_R32_SINT)
    attributeColorTextureIndex.offset(BasicVertex.OFFSET_COLOR_TEXTURE_INDEX)

    val attributeHeightTextureIndex = attributes[8]
    attributeHeightTextureIndex.location(8)
    attributeHeightTextureIndex.binding(0)
    attributeHeightTextureIndex.format(VK_FORMAT_R32_SINT)
    attributeHeightTextureIndex.offset(BasicVertex.OFFSET_HEIGHT_TEXTURE_INDEX)


    val ciVertexInput = VkPipelineVertexInputStateCreateInfo.calloc(stack)
    ciVertexInput.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
    ciVertexInput.pVertexBindingDescriptions(bindings)
    ciVertexInput.pVertexAttributeDescriptions(attributes)

    return ciVertexInput
}

fun createBasicInputAssembly(stack: MemoryStack): VkPipelineInputAssemblyStateCreateInfo {
    val ciAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
    ciAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
    ciAssembly.topology(VK_PRIMITIVE_TOPOLOGY_PATCH_LIST)
    ciAssembly.primitiveRestartEnable(false)

    return ciAssembly
}
