package graviks2d.pipeline.text

import graviks2d.pipeline.createGraviksShaderModule
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo

internal fun createTextCountShaderStages(
    vkDevice: VkDevice, stack: MemoryStack
): VkPipelineShaderStageCreateInfo.Buffer {
    val ciStages = VkPipelineShaderStageCreateInfo.calloc(2)

    val ciVertexStage = ciStages[0]
    ciVertexStage.`sType$Default`()
    ciVertexStage.stage(VK_SHADER_STAGE_VERTEX_BIT)
    ciVertexStage.module(createGraviksShaderModule(vkDevice, stack, "graviks2d/shaders/textCount.vert.spv"))
    ciVertexStage.pName(stack.UTF8("main"))

    val ciFragmentStage = ciStages[1]
    ciFragmentStage.`sType$Default`()
    ciFragmentStage.stage(VK_SHADER_STAGE_FRAGMENT_BIT)
    ciFragmentStage.module(createGraviksShaderModule(vkDevice, stack, "graviks2d/shaders/textCount.frag.spv"))
    ciFragmentStage.pName(stack.UTF8("main"))

    return ciStages
}

internal fun createTextOddShaderStages(
    vkDevice: VkDevice, stack: MemoryStack
): VkPipelineShaderStageCreateInfo.Buffer {
    val ciStages = VkPipelineShaderStageCreateInfo.calloc(2)

    val ciVertexStage = ciStages[0]
    ciVertexStage.`sType$Default`()
    ciVertexStage.stage(VK_SHADER_STAGE_VERTEX_BIT)
    ciVertexStage.module(createGraviksShaderModule(vkDevice, stack, "graviks2d/shaders/textOdd.vert.spv"))
    ciVertexStage.pName(stack.UTF8("main"))

    val ciFragmentStage = ciStages[1]
    ciFragmentStage.`sType$Default`()
    ciFragmentStage.stage(VK_SHADER_STAGE_FRAGMENT_BIT)
    ciFragmentStage.module(createGraviksShaderModule(vkDevice, stack, "graviks2d/shaders/textOdd.frag.spv"))
    ciFragmentStage.pName(stack.UTF8("main"))

    return ciStages
}
