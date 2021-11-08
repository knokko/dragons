package dragons.plugins.standard.vulkan.pipeline

import dragons.vulkan.RenderImageInfo
import dragons.vulkan.util.assertVkSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.*
import org.slf4j.LoggerFactory.getLogger

class BasicGraphicsPipeline(
    val handle: Long,
    val descriptorSetLayout: Long,
    val pipelineLayout: Long
)

fun destroyBasicGraphicsPipeline(vkDevice: VkDevice, basicPipeline: BasicGraphicsPipeline) {
    vkDestroyPipeline(vkDevice, basicPipeline.handle, null)
    vkDestroyPipelineLayout(vkDevice, basicPipeline.pipelineLayout, null)
    vkDestroyDescriptorSetLayout(vkDevice, basicPipeline.descriptorSetLayout, null)
}

fun createBasicGraphicsPipeline(
    vkDevice: VkDevice, basicRenderPass: Long, renderImageInfo: RenderImageInfo,
    width: Int, height: Int
): BasicGraphicsPipeline {
    stackPush().use { stack ->

        val (basicDescriptorSetLayout, basicPipelineLayout) = createBasicPipelineLayout(vkDevice, stack)

        val ciPipelines = VkGraphicsPipelineCreateInfo.calloc(1, stack)
        createBasicGraphicsPipeline(
            vkDevice, basicRenderPass, basicPipelineLayout, renderImageInfo, ciPipelines[0], width, height, stack
        )

        val logger = getLogger("Vulkan")
        val pPipelines = stack.callocLong(ciPipelines.capacity())
        logger.info("Creating basic graphics pipeline...")
        assertVkSuccess(
            vkCreateGraphicsPipelines(vkDevice, VK_NULL_HANDLE, ciPipelines, null, pPipelines),
            "CreateGraphicsPipelines"
        )
        logger.info("Created basic graphics pipeline")

        vkDestroyShaderModule(vkDevice, ciPipelines[0].pStages()[0].module(), null)
        vkDestroyShaderModule(vkDevice, ciPipelines[0].pStages()[1].module(), null)
        vkDestroyShaderModule(vkDevice, ciPipelines[0].pStages()[2].module(), null)
        vkDestroyShaderModule(vkDevice, ciPipelines[0].pStages()[3].module(), null)

        return BasicGraphicsPipeline(
            handle = pPipelines[0],
            descriptorSetLayout = basicDescriptorSetLayout,
            pipelineLayout = basicPipelineLayout
        )
    }
}

fun createBasicGraphicsPipeline(
    vkDevice: VkDevice, basicRenderPass: Long, basicPipelineLayout: Long, renderImageInfo: RenderImageInfo,
    ciPipeline: VkGraphicsPipelineCreateInfo, width: Int, height: Int, stack: MemoryStack
) {
    ciPipeline.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
    ciPipeline.flags(0)
    ciPipeline.pStages(createBasicShaders(vkDevice, stack))
    ciPipeline.pVertexInputState(createBasicVertexInputState(stack))
    ciPipeline.pInputAssemblyState(createBasicInputAssembly(stack))
    ciPipeline.pTessellationState(createTessellationState(stack))
    ciPipeline.pViewportState(createBasicViewportState(stack, width, height))
    ciPipeline.pRasterizationState(createBasicRasterizationState(stack))
    ciPipeline.pMultisampleState(createBasicMultisampleState(renderImageInfo, stack))
    ciPipeline.pDepthStencilState(createBasicDepthStencilState(stack))
    ciPipeline.pColorBlendState(createBasicColorBlendState(stack))
    ciPipeline.layout(basicPipelineLayout)
    ciPipeline.renderPass(basicRenderPass)
    ciPipeline.subpass(0)
}

fun createTessellationState(stack: MemoryStack): VkPipelineTessellationStateCreateInfo {
    val ciTess = VkPipelineTessellationStateCreateInfo.calloc(stack)
    ciTess.sType(VK_STRUCTURE_TYPE_PIPELINE_TESSELLATION_STATE_CREATE_INFO)
    ciTess.patchControlPoints(3)

    return ciTess
}
