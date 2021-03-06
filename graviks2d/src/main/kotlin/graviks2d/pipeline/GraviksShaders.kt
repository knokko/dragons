package graviks2d.pipeline

import graviks2d.util.assertSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memCalloc
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

internal fun createGraviksShaderStages(
    vkDevice: VkDevice, maxNumDescriptorImages: Int, stack: MemoryStack
): VkPipelineShaderStageCreateInfo.Buffer {
    val ciStages = VkPipelineShaderStageCreateInfo.calloc(2)

    val ciVertexStage = ciStages[0]
    ciVertexStage.`sType$Default`()
    ciVertexStage.stage(VK_SHADER_STAGE_VERTEX_BIT)
    ciVertexStage.module(createGraviksShaderModule(vkDevice, stack, "graviks2d/shaders/basic.vert.spv"))
    ciVertexStage.pName(stack.UTF8("main"))

    val specializationData = stack.calloc(4)
    specializationData.putInt(0, maxNumDescriptorImages)

    val specializationMapping = VkSpecializationMapEntry.calloc(1, stack)
    specializationMapping.constantID(0)
    specializationMapping.offset(0)
    specializationMapping.size(4)

    val fragmentSpecializationInfo = VkSpecializationInfo.calloc(stack)
    fragmentSpecializationInfo.pMapEntries(specializationMapping)
    fragmentSpecializationInfo.pData(specializationData)

    val ciFragmentStage = ciStages[1]
    ciFragmentStage.`sType$Default`()
    ciFragmentStage.stage(VK_SHADER_STAGE_FRAGMENT_BIT)
    ciFragmentStage.module(createGraviksShaderModule(vkDevice, stack, "graviks2d/shaders/basic.frag.spv"))
    ciFragmentStage.pName(stack.UTF8("main"))
    ciFragmentStage.pSpecializationInfo(fragmentSpecializationInfo)

    return ciStages
}

internal fun createGraviksShaderModule(
    vkDevice: VkDevice, stack: MemoryStack, path: String
): Long {

    val shaderInput = GraviksPipeline::class.java.classLoader.getResourceAsStream(path)!!
    val shaderByteArray = shaderInput.readAllBytes()
    shaderInput.close()

    val shaderByteBuffer = memCalloc(shaderByteArray.size)
    shaderByteBuffer.put(0, shaderByteArray)

    val ciModule = VkShaderModuleCreateInfo.calloc(stack)
    ciModule.`sType$Default`()
    ciModule.pCode(shaderByteBuffer)

    val pModule = stack.callocLong(1)
    assertSuccess(
        vkCreateShaderModule(vkDevice, ciModule, null, pModule),
        "vkCreateShaderModule"
    )

    memFree(shaderByteBuffer)
    return pModule[0]
}

internal const val OP_CODE_FILL_RECT = 1
internal const val OP_CODE_DRAW_IMAGE_BOTTOM_LEFT = 2
internal const val OP_CODE_DRAW_IMAGE_BOTTOM_RIGHT = 3
internal const val OP_CODE_DRAW_IMAGE_TOP_RIGHT = 4
internal const val OP_CODE_DRAW_IMAGE_TOP_LEFT = 5
internal const val OP_CODE_DRAW_TEXT = 6
internal const val OP_CODE_DRAW_ROUNDED_RECT = 7
