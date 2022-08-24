package dragons.plugins.standard.vulkan.pipeline

import dragons.vulkan.util.assertVkSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.*

const val MAX_NUM_DESCRIPTOR_IMAGES = 1000

fun createBasicPipelineLayout(vkDevice: VkDevice, stack: MemoryStack): Triple<Long, Long, Long> {
    val staticBindings = VkDescriptorSetLayoutBinding.calloc(3, stack)
    val dynamicBindings = VkDescriptorSetLayoutBinding.calloc(2, stack)
    val ciCamera = staticBindings[0]
    ciCamera.binding(0)
    ciCamera.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
    ciCamera.descriptorCount(1)
    ciCamera.stageFlags(VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT or VK_SHADER_STAGE_FRAGMENT_BIT)

    val ciSampler = staticBindings[1]
    ciSampler.binding(1)
    ciSampler.descriptorType(VK_DESCRIPTOR_TYPE_SAMPLER)
    ciSampler.descriptorCount(1)
    ciSampler.stageFlags(VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT or VK_SHADER_STAGE_FRAGMENT_BIT)

    val ciTransformationMatrices = staticBindings[2]
    ciTransformationMatrices.binding(2)
    ciTransformationMatrices.descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
    ciTransformationMatrices.descriptorCount(1)
    ciTransformationMatrices.stageFlags(VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT or VK_SHADER_STAGE_FRAGMENT_BIT)

    val ciColorImages = dynamicBindings[0]
    ciColorImages.binding(0)
    ciColorImages.descriptorType(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
    ciColorImages.descriptorCount(MAX_NUM_DESCRIPTOR_IMAGES)
    ciColorImages.stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)

    val ciHeightImages = dynamicBindings[1]
    ciHeightImages.binding(1)
    ciHeightImages.descriptorType(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
    ciHeightImages.descriptorCount(MAX_NUM_DESCRIPTOR_IMAGES)
    ciHeightImages.stageFlags(VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT or VK_SHADER_STAGE_FRAGMENT_BIT)

    val ciStaticSetLayout = VkDescriptorSetLayoutCreateInfo.calloc(stack)
    ciStaticSetLayout.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
    ciStaticSetLayout.pBindings(staticBindings)

    val pSetLayouts = stack.callocLong(1)
    assertVkSuccess(
        vkCreateDescriptorSetLayout(vkDevice, ciStaticSetLayout, null, pSetLayouts),
        "CreateDescriptorSetLayout", "standard plug-in: basic static"
    )
    val staticSetLayout = pSetLayouts[0]

    val ciDynamicSetLayout = VkDescriptorSetLayoutCreateInfo.calloc(stack)
    ciDynamicSetLayout.`sType$Default`()
    ciDynamicSetLayout.pBindings(dynamicBindings)

    assertVkSuccess(
        vkCreateDescriptorSetLayout(vkDevice, ciDynamicSetLayout, null, pSetLayouts),
        "CreateDescriptorSetLayout", "standard plug-in: basic dynamic"
    )
    val dynamicSetLayout = pSetLayouts[0]

    val setLayouts = stack.longs(staticSetLayout, dynamicSetLayout)

    val pushConstants = VkPushConstantRange.calloc(1, stack)
    val pushEyeIndex = pushConstants[0]
    pushEyeIndex.stageFlags(VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT)
    pushEyeIndex.offset(0)
    pushEyeIndex.size(4)

    val ciLayout = VkPipelineLayoutCreateInfo.calloc(stack)
    ciLayout.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
    ciLayout.pSetLayouts(setLayouts)
    ciLayout.pPushConstantRanges(pushConstants)

    val pLayout = stack.callocLong(1)
    assertVkSuccess(
        vkCreatePipelineLayout(vkDevice, ciLayout, null, pLayout),
        "CreatePipelineLayout", "standard plug-in: basic"
    )

    return Triple(staticSetLayout, dynamicSetLayout, pLayout[0])
}
