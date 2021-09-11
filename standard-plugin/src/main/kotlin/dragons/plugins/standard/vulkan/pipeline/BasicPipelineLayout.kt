package dragons.plugins.standard.vulkan.pipeline

import dragons.vulkan.util.assertVkSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo

const val MAX_NUM_DESCRIPTOR_IMAGES = 1000

fun createBasicPipelineLayout(vkDevice: VkDevice, stack: MemoryStack): Pair<Long, Long> {
    val bindings = VkDescriptorSetLayoutBinding.calloc(5, stack)
    val ciCameraMatrix = bindings[0]
    ciCameraMatrix.binding(0)
    ciCameraMatrix.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
    ciCameraMatrix.descriptorCount(1)
    ciCameraMatrix.stageFlags(VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT or VK_SHADER_STAGE_FRAGMENT_BIT)

    val ciSampler = bindings[1]
    ciSampler.binding(1)
    ciSampler.descriptorType(VK_DESCRIPTOR_TYPE_SAMPLER)
    ciSampler.descriptorCount(1)
    ciSampler.stageFlags(VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT or VK_SHADER_STAGE_FRAGMENT_BIT)

    val ciColorImage = bindings[2]
    ciColorImage.binding(2)
    ciColorImage.descriptorType(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
    ciColorImage.descriptorCount(MAX_NUM_DESCRIPTOR_IMAGES)
    ciColorImage.stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)

    val ciHeightImage = bindings[3]
    ciHeightImage.binding(3)
    ciHeightImage.descriptorType(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
    ciHeightImage.descriptorCount(MAX_NUM_DESCRIPTOR_IMAGES)
    ciHeightImage.stageFlags(VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT or VK_SHADER_STAGE_FRAGMENT_BIT)

    val ciTransformationMatrices = bindings[4]
    ciTransformationMatrices.binding(4)
    ciTransformationMatrices.descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
    ciTransformationMatrices.descriptorCount(1)
    ciTransformationMatrices.stageFlags(VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT or VK_SHADER_STAGE_FRAGMENT_BIT)

    val ciSetLayouts = VkDescriptorSetLayoutCreateInfo.calloc(stack)
    ciSetLayouts.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
    ciSetLayouts.pBindings(bindings)

    val pSetLayouts = stack.callocLong(1)
    assertVkSuccess(
        vkCreateDescriptorSetLayout(vkDevice, ciSetLayouts, null, pSetLayouts),
        "CreateDescriptorSetLayout", "basic"
    )

    val ciLayout = VkPipelineLayoutCreateInfo.calloc(stack)
    ciLayout.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
    ciLayout.pSetLayouts(pSetLayouts)
    ciLayout.pPushConstantRanges(null)

    val pLayout = stack.callocLong(1)
    assertVkSuccess(
        vkCreatePipelineLayout(vkDevice, ciLayout, null, pLayout),
        "CreatePipelineLayout", "basic"
    )

    return Pair(pSetLayouts[0], pLayout[0])
}
