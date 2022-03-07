package graviks2d.pipeline

import graviks2d.context.TARGET_COLOR_FORMAT
import graviks2d.core.GraviksInstance
import graviks2d.util.assertSuccess
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo


internal class GraviksPipeline(
    val instance: GraviksInstance
) {

    val vkPipeline: Long
    val vkPipelineLayout: Long
    val vkDescriptorSetLayout: Long
    val vkRenderPass: Long

    val depthStencilFormat: Int

    init {
        stackPush().use { stack ->

            val (pipelineLayout, descriptorSetLayout) = createGraviksPipelineLayout(instance.device, stack)
            this.vkPipelineLayout = pipelineLayout
            this.vkDescriptorSetLayout = descriptorSetLayout
            val (renderPass, depthFormat) = createGraviksRenderPass(
                instance.physicalDevice, instance.device, TARGET_COLOR_FORMAT, stack
            )
            this.vkRenderPass = renderPass
            this.depthStencilFormat = depthFormat

            val ciPipelines = VkGraphicsPipelineCreateInfo.calloc(1, stack)
            val ciPipeline = ciPipelines[0]
            ciPipeline.`sType$Default`()
            ciPipeline.pStages(createGraviksShaderStages(instance.device, stack))
            ciPipeline.pVertexInputState(createGraviksPipelineVertexInput(stack))
            ciPipeline.pInputAssemblyState(createGraviksPipelineInputAssembly(stack))
            ciPipeline.pViewportState(createGraviksPipelineViewport(stack))
            ciPipeline.pDynamicState(createGraviksPipelineDynamics(stack))
            ciPipeline.pRasterizationState(createGraviksPipelineRasterization(stack))
            ciPipeline.pMultisampleState(createGraviksPipelineMultisampleState(stack))
            ciPipeline.pDepthStencilState(createGraviksPipelineDepthState(stack))
            ciPipeline.pColorBlendState(createGraviksPipelineColorBlend(stack))
            ciPipeline.layout(pipelineLayout)
            ciPipeline.renderPass(vkRenderPass)
            ciPipeline.subpass(0)

            val pPipeline = stack.callocLong(1)
            assertSuccess(
                vkCreateGraphicsPipelines(instance.device, VK_NULL_HANDLE, ciPipelines, null, pPipeline),
                "vkCreateGraphicsPipeline"
            )
            this.vkPipeline = pPipeline[0]

            vkDestroyShaderModule(instance.device, ciPipeline.pStages()[0].module(), null)
            vkDestroyShaderModule(instance.device, ciPipeline.pStages()[1].module(), null)
        }
    }

    fun destroy() {
        vkDestroyPipeline(this.instance.device, this.vkPipeline, null)
        vkDestroyPipelineLayout(this.instance.device, this.vkPipelineLayout, null)
        vkDestroyDescriptorSetLayout(this.instance.device, this.vkDescriptorSetLayout, null)
        vkDestroyRenderPass(this.instance.device, this.vkRenderPass, null)
    }
}
