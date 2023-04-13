package graviks2d.pipeline.text

import graviks2d.pipeline.createGraviksPipelineDynamics
import graviks2d.pipeline.createGraviksPipelineInputAssembly
import graviks2d.pipeline.createGraviksPipelineRasterization
import graviks2d.pipeline.createGraviksPipelineViewport
import graviks2d.util.assertSuccess
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo

const val TEXT_COLOR_FORMAT = VK_FORMAT_R8_UNORM

internal class TextPipeline(
    val vkDevice: VkDevice
) {
    val countPipeline: Long
    val countPipelineLayout: Long
    val oddPipeline: Long
    val oddPipelineLayout: Long
    val oddDescriptorSetLayout: Long
    val vkRenderPass: Long

    init {
        // TODO Pipeline cache
        stackPush().use { stack ->

            this.countPipelineLayout = createTextCountPipelineLayout(vkDevice, stack)
            val (oddPipelineLayout, oddDescriptorSetLayout) = createTextOddPipelineLayout(vkDevice, stack)
            this.oddPipelineLayout = oddPipelineLayout
            this.oddDescriptorSetLayout = oddDescriptorSetLayout
            val renderPass = createTextRenderPass(vkDevice, stack)
            this.vkRenderPass = renderPass

            val ciPipelines = VkGraphicsPipelineCreateInfo.calloc(2, stack)
            val ciCountPipeline = ciPipelines[0]

            // Note: the following is shared with the regular graviks pipeline:
            // inputAssembly, viewportState, dynamicState, rasterizationState
            ciCountPipeline.`sType$Default`()
            val countStages = createTextCountShaderStages(vkDevice, stack)
            ciCountPipeline.pStages(countStages)
            ciCountPipeline.pVertexInputState(createTextCountPipelineVertexInput(stack))
            ciCountPipeline.pInputAssemblyState(createGraviksPipelineInputAssembly(stack))
            ciCountPipeline.pViewportState(createGraviksPipelineViewport(stack))
            ciCountPipeline.pDynamicState(createGraviksPipelineDynamics(stack))
            ciCountPipeline.pRasterizationState(createGraviksPipelineRasterization(stack))
            ciCountPipeline.pMultisampleState(createTextPipelineMultisampleState(stack))
            ciCountPipeline.pColorBlendState(createTextCountPipelineColorBlend(stack))
            ciCountPipeline.layout(this.countPipelineLayout)
            ciCountPipeline.renderPass(vkRenderPass)
            ciCountPipeline.subpass(0)

            val ciOddPipeline = ciPipelines[1]
            ciOddPipeline.`sType$Default`()
            val oddStages = createTextOddShaderStages(vkDevice, stack)
            ciOddPipeline.pStages(oddStages)
            ciOddPipeline.pVertexInputState(createTextOddPipelineVertexInput(stack))
            ciOddPipeline.pInputAssemblyState(createGraviksPipelineInputAssembly(stack))
            ciOddPipeline.pViewportState(createGraviksPipelineViewport(stack))
            ciOddPipeline.pDynamicState(createGraviksPipelineDynamics(stack))
            ciOddPipeline.pRasterizationState(createGraviksPipelineRasterization(stack))
            ciOddPipeline.pMultisampleState(createTextPipelineMultisampleState(stack))
            ciOddPipeline.pColorBlendState(createTextOddPipelineColorBlend(stack))
            ciOddPipeline.layout(this.oddPipelineLayout)
            ciOddPipeline.renderPass(this.vkRenderPass)
            ciOddPipeline.subpass(1)

            val pPipelines = stack.callocLong(2)
            assertSuccess(
                vkCreateGraphicsPipelines(vkDevice, VK_NULL_HANDLE, ciPipelines, null, pPipelines),
                "vkCreateGraphicsPipeline"
            )
            this.countPipeline = pPipelines[0]
            this.oddPipeline = pPipelines[1]

            vkDestroyShaderModule(vkDevice, countStages[0].module(), null)
            vkDestroyShaderModule(vkDevice, countStages[1].module(), null)
            vkDestroyShaderModule(vkDevice, oddStages[0].module(), null)
            vkDestroyShaderModule(vkDevice, oddStages[1].module(), null)
        }
    }

    fun destroy() {
        vkDestroyPipeline(this.vkDevice, this.countPipeline, null)
        vkDestroyPipelineLayout(this.vkDevice, this.countPipelineLayout, null)
        vkDestroyPipeline(this.vkDevice, this.oddPipeline, null)
        vkDestroyPipelineLayout(this.vkDevice, this.oddPipelineLayout, null)
        vkDestroyDescriptorSetLayout(this.vkDevice, this.oddDescriptorSetLayout, null)
        vkDestroyRenderPass(this.vkDevice, this.vkRenderPass, null)
    }
}
