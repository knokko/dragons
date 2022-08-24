package dragons.plugins.standard.vulkan.pipeline

import dragons.plugins.standard.vulkan.vertex.BasicVertex
import dragons.util.mallocBundledResource
import dragons.vulkan.util.assertVkSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.*

fun createBasicShaders(vkDevice: VkDevice, stack: MemoryStack): VkPipelineShaderStageCreateInfo.Buffer {
    val specializationData = stack.calloc(4)
    specializationData.putInt(0, MAX_NUM_DESCRIPTOR_IMAGES)

    val specializationMapping = VkSpecializationMapEntry.calloc(1, stack)
    specializationMapping.constantID(0)
    specializationMapping.offset(0)
    specializationMapping.size(4)

    val specializationInfo = VkSpecializationInfo.calloc(stack)
    specializationInfo.pMapEntries(specializationMapping)
    specializationInfo.pData(specializationData)

    val ciShaderStages = VkPipelineShaderStageCreateInfo.calloc(4, stack)

    val vertexStage = ciShaderStages[0]
    vertexStage.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
    vertexStage.flags(0)
    vertexStage.stage(VK_SHADER_STAGE_VERTEX_BIT)
    vertexStage.module(createShaderModule(vkDevice, stack, "vert", "basic"))
    vertexStage.pName(stack.UTF8("main"))

    val tessControlStage = ciShaderStages[1]
    tessControlStage.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
    tessControlStage.flags(0)
    tessControlStage.stage(VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT)
    tessControlStage.module(createShaderModule(vkDevice, stack, "tesc", "basic"))
    tessControlStage.pName(stack.UTF8("main"))

    val tessEvalStage = ciShaderStages[2]
    tessEvalStage.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
    tessEvalStage.flags(0)
    tessEvalStage.stage(VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT)
    tessEvalStage.module(createShaderModule(vkDevice, stack, "tese", "basic"))
    tessEvalStage.pName(stack.UTF8("main"))
    tessEvalStage.pSpecializationInfo(specializationInfo)

    val fragmentStage = ciShaderStages[3]
    fragmentStage.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
    fragmentStage.flags(0)
    fragmentStage.stage(VK_SHADER_STAGE_FRAGMENT_BIT)
    fragmentStage.module(createShaderModule(vkDevice, stack, "frag", "basic"))
    fragmentStage.pName(stack.UTF8("main"))
    fragmentStage.pSpecializationInfo(specializationInfo)

    return ciShaderStages
}

private fun createShaderModule(vkDevice: VkDevice, stack: MemoryStack, stage: String, name: String): Long {
    val shaderResourcePath = "dragons/plugins/standard/vulkan/shaders/$name.$stage.spv"
    val shaderByteCode = mallocBundledResource(shaderResourcePath, BasicVertex::class.java.classLoader)
        ?: throw Error("Can't find shader resource: $shaderResourcePath")

    val ciShaderModule = VkShaderModuleCreateInfo.calloc(stack)
    ciShaderModule.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
    ciShaderModule.pCode(shaderByteCode)

    val pShaderModule = stack.callocLong(1)
    assertVkSuccess(
        vkCreateShaderModule(vkDevice, ciShaderModule, null, pShaderModule),
        "CreateShaderModule", stage
    )

    memFree(shaderByteCode)
    return pShaderModule[0]
}
