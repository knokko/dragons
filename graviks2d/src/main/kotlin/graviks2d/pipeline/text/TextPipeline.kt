package graviks2d.pipeline.text

import graviks2d.pipeline.createGraviksPipelineDynamics
import graviks2d.pipeline.createGraviksPipelineInputAssembly
import graviks2d.pipeline.createGraviksPipelineRasterization
import graviks2d.pipeline.createGraviksPipelineViewport
import graviks2d.util.assertSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo

const val TEXT_COLOR_FORMAT = VK_FORMAT_R8_UNORM

internal class TextPipeline(
    val vkDevice: VkDevice
) {
    val vkPipeline: Long
    val vkPipelineLayout: Long
    val vkRenderPass: Long

    init {
        MemoryStack.stackPush().use { stack ->

            this.vkPipelineLayout = createTextPipelineLayout(vkDevice, stack)
            val renderPass = createTextRenderPass(vkDevice, stack)
            this.vkRenderPass = renderPass

            val ciPipelines = VkGraphicsPipelineCreateInfo.calloc(1, stack)
            val ciPipeline = ciPipelines[0]

            // Note: the following is shared with the regular graviks pipeline:
            // inputAssembly, viewportState, dynamicState, rasterizationState
            ciPipeline.`sType$Default`()
            ciPipeline.pStages(createTextShaderStages(vkDevice, stack))
            ciPipeline.pVertexInputState(createTextPipelineVertexInput(stack))
            ciPipeline.pInputAssemblyState(createGraviksPipelineInputAssembly(stack))
            ciPipeline.pViewportState(createGraviksPipelineViewport(stack))
            ciPipeline.pDynamicState(createGraviksPipelineDynamics(stack))
            ciPipeline.pRasterizationState(createGraviksPipelineRasterization(stack))
            ciPipeline.pMultisampleState(createTextPipelineMultisampleState(stack))
            ciPipeline.pDepthStencilState(createTextPipelineDepthState(stack))
            ciPipeline.pColorBlendState(createTextPipelineColorBlend(stack))
            ciPipeline.layout(vkPipelineLayout)
            ciPipeline.renderPass(vkRenderPass)
            ciPipeline.subpass(0)

            val pPipeline = stack.callocLong(1)
            assertSuccess(
                vkCreateGraphicsPipelines(vkDevice, VK_NULL_HANDLE, ciPipelines, null, pPipeline),
                "vkCreateGraphicsPipeline"
            )
            this.vkPipeline = pPipeline[0]

            vkDestroyShaderModule(vkDevice, ciPipeline.pStages()[0].module(), null)
            vkDestroyShaderModule(vkDevice, ciPipeline.pStages()[1].module(), null)
        }
    }

    fun destroy() {
        vkDestroyPipeline(vkDevice, this.vkPipeline, null)
        vkDestroyPipelineLayout(vkDevice, this.vkPipelineLayout, null)
        vkDestroyRenderPass(vkDevice, this.vkRenderPass, null)
    }
}
