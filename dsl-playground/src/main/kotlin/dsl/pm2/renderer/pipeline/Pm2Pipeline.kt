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
)

internal class Pm2Pipeline(
    val vkPipeline: Long,
    val vkPipelineLayout: Long
) {
    fun destroy(vkDevice: VkDevice) {
        vkDestroyPipeline(vkDevice, vkPipeline, null)
        vkDestroyPipelineLayout(vkDevice, vkPipelineLayout, null)
    }
}

internal fun createGraphicsPipeline(vkDevice: VkDevice, info: Pm2PipelineInfo): Pm2Pipeline {
    return stackPush().use { stack ->
        val layout = createPipelineLayout(vkDevice, stack)

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
        ciPipeline.layout(layout)
        ciPipeline.renderPass(info.renderPass)
        ciPipeline.subpass(info.subpass)

        val pPipeline = stack.callocLong(1)
        checkReturnValue(vkCreateGraphicsPipelines(
            vkDevice, VK_NULL_HANDLE, ciPipeline, null, pPipeline
        ), "CreateGraphicsPipelines")

        vkDestroyShaderModule(vkDevice, vertexShaderModule, null)
        vkDestroyShaderModule(vkDevice, fragmentShaderModule, null)

        Pm2Pipeline(pPipeline[0], layout)
    }
}

private fun createPipelineLayout(vkDevice: VkDevice, stack: MemoryStack): Long {
    // TODO Maybe create just 1 pipeline layout and reuse that
    val ciLayout = VkPipelineLayoutCreateInfo.calloc(stack)
    ciLayout.`sType$Default`()
    ciLayout.setLayoutCount(0)
    ciLayout.pSetLayouts(null)
    ciLayout.pPushConstantRanges(null)

    val pLayout = stack.callocLong(1)
    checkReturnValue(vkCreatePipelineLayout(vkDevice, ciLayout, null, pLayout), "CreatePipelineLayout")
    return pLayout[0]
}

private fun createDynamicState(stack: MemoryStack): VkPipelineDynamicStateCreateInfo {
    val pDynamicState = VkPipelineDynamicStateCreateInfo.calloc(stack)
    pDynamicState.`sType$Default`()
    pDynamicState.flags(0)
    pDynamicState.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR))

    return pDynamicState
}
