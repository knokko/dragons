package dragons.plugins.standard.vulkan.pipeline

import dragons.vulkan.util.assertVkSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.*

// NOTE: This must match the sizes hardcoded in the fragment shader and tessellation evaluation shader
const val MAX_NUM_DESCRIPTOR_IMAGES = 100

fun createBasicPipelineLayout(vkDevice: VkDevice, stack: MemoryStack): Pair<Long, Long> {
    val bindings = VkDescriptorSetLayoutBinding.calloc(5, stack)
    val ciCamera = bindings[0]
    ciCamera.binding(0)
    ciCamera.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
    ciCamera.descriptorCount(1)
    ciCamera.stageFlags(VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT or VK_SHADER_STAGE_FRAGMENT_BIT)

    val ciSampler = bindings[1]
    ciSampler.binding(1)
    ciSampler.descriptorType(VK_DESCRIPTOR_TYPE_SAMPLER)
    ciSampler.descriptorCount(1)
    ciSampler.stageFlags(VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT or VK_SHADER_STAGE_FRAGMENT_BIT)

    val ciColorImages = bindings[2]
    ciColorImages.binding(2)
    ciColorImages.descriptorType(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
    ciColorImages.descriptorCount(MAX_NUM_DESCRIPTOR_IMAGES)
    ciColorImages.stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)

    val ciHeightImages = bindings[3]
    ciHeightImages.binding(3)
    ciHeightImages.descriptorType(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
    ciHeightImages.descriptorCount(MAX_NUM_DESCRIPTOR_IMAGES)
    ciHeightImages.stageFlags(VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT or VK_SHADER_STAGE_FRAGMENT_BIT)

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

    val pushConstants = VkPushConstantRange.calloc(1, stack)
    val pushEyeIndex = pushConstants[0]
    pushEyeIndex.stageFlags(VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT)
    pushEyeIndex.offset(0)
    pushEyeIndex.size(4)

    val ciLayout = VkPipelineLayoutCreateInfo.calloc(stack)
    ciLayout.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
    ciLayout.pSetLayouts(pSetLayouts)
    ciLayout.pPushConstantRanges(pushConstants)

    val pLayout = stack.callocLong(1)
    assertVkSuccess(
        vkCreatePipelineLayout(vkDevice, ciLayout, null, pLayout),
        "CreatePipelineLayout", "basic"
    )

    return Pair(pSetLayouts[0], pLayout[0])
}
