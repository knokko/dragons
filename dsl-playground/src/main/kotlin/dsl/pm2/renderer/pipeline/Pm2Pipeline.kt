package dsl.pm2.renderer.pipeline

import dsl.pm2.renderer.checkReturnValue
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

class Pm2PipelineInfo(
    internal val renderPass: Long,
    internal val subpass: Int,
    internal val setBlendState: (MemoryStack, VkPipelineColorBlendStateCreateInfo) -> Unit
) {
    override fun hashCode() = renderPass.toInt() + 131 * subpass + 29 * setBlendState.hashCode()

    override fun equals(other: Any?) = other is Pm2PipelineInfo && this.renderPass == other.renderPass
            && this.subpass == other.subpass && this.setBlendState == other.setBlendState
}

internal fun createGraphicsPipeline(vkDevice: VkDevice, info: Pm2PipelineInfo, pipelineLayout: Long): Long {
    return stackPush().use { stack ->
        val ciPipeline = VkGraphicsPipelineCreateInfo.calloc(1, stack)
        ciPipeline.`sType$Default`()
        ciPipeline.flags(0)

        val vertexShaderModule = createVertexShaderModule(vkDevice, stack)
        val fragmentShaderModule = createFragmentShaderModule(vkDevice, stack)
        ciPipeline.pStages(createShaderStages(stack, vertexShaderModule, fragmentShaderModule))

        ciPipeline.pVertexInputState(createVertexInputState(stack))
        ciPipeline.pInputAssemblyState(createInputAssemblyState(stack))
        ciPipeline.pViewportState(createViewportState(stack))
        ciPipeline.pRasterizationState(createRasterizationState(stack))
        ciPipeline.pDepthStencilState(createDepthStencilState(stack))

        val blendState = VkPipelineColorBlendStateCreateInfo.calloc(stack)
        info.setBlendState(stack, blendState)
        ciPipeline.pColorBlendState(blendState)
        ciPipeline.pDynamicState(createDynamicState(stack))
        ciPipeline.pMultisampleState(createMultisampleState(stack))
        ciPipeline.layout(pipelineLayout)
        ciPipeline.renderPass(info.renderPass)
        ciPipeline.subpass(info.subpass)

        val pPipeline = stack.callocLong(1)
        checkReturnValue(vkCreateGraphicsPipelines(
            vkDevice, VK_NULL_HANDLE, ciPipeline, null, pPipeline
        ), "CreateGraphicsPipelines")

        vkDestroyShaderModule(vkDevice, vertexShaderModule, null)
        vkDestroyShaderModule(vkDevice, fragmentShaderModule, null)

        pPipeline[0]
    }
}

internal fun createPipelineLayout(vkDevice: VkDevice, stack: MemoryStack): Pair<Long, Long> {
    val descriptorBindings = VkDescriptorSetLayoutBinding.calloc(1, stack)
    descriptorBindings.binding(0)
    descriptorBindings.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
    descriptorBindings.descriptorCount(1)
    descriptorBindings.stageFlags(VK_SHADER_STAGE_VERTEX_BIT)

    val ciDescriptorLayout = VkDescriptorSetLayoutCreateInfo.calloc(stack)
    ciDescriptorLayout.`sType$Default`()
    ciDescriptorLayout.flags(0)
    ciDescriptorLayout.pBindings(descriptorBindings)

    val pDescriptorLayout = stack.callocLong(1)
    checkReturnValue(vkCreateDescriptorSetLayout(
            vkDevice, ciDescriptorLayout, null, pDescriptorLayout
    ), "CreateDescriptorSetLayout")

    val descriptorSetLayout = pDescriptorLayout[0]

    val pushConstants = VkPushConstantRange.calloc(1, stack)
    val pushCameraMatrix = pushConstants[0]
    pushCameraMatrix.stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
    pushCameraMatrix.offset(0)
    pushCameraMatrix.size(4 + 3 * 2 * 4) // 1 int and 1 3x2 matrix

    // TODO Maybe create just 1 pipeline layout and reuse that
    val ciLayout = VkPipelineLayoutCreateInfo.calloc(stack)
    ciLayout.`sType$Default`()
    ciLayout.setLayoutCount(0)
    ciLayout.pSetLayouts(stack.longs(descriptorSetLayout))
    ciLayout.pPushConstantRanges(pushConstants)

    val pLayout = stack.callocLong(1)
    checkReturnValue(vkCreatePipelineLayout(vkDevice, ciLayout, null, pLayout), "CreatePipelineLayout")
    return Pair(pLayout[0], descriptorSetLayout)
}

private fun createDynamicState(stack: MemoryStack): VkPipelineDynamicStateCreateInfo {
    val pDynamicState = VkPipelineDynamicStateCreateInfo.calloc(stack)
    pDynamicState.`sType$Default`()
    pDynamicState.flags(0)
    pDynamicState.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR))

    return pDynamicState
}
