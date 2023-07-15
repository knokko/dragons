package graviks2d.pipeline.text

import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo
import troll.exceptions.VulkanFailureException.assertVkSuccess
import troll.instance.TrollInstance
import troll.pipelines.ShaderInfo

const val TEXT_COLOR_FORMAT = VK_FORMAT_R8_UNORM

internal class TextPipeline(
    val troll: TrollInstance
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

            val oddLayoutBindings = VkDescriptorSetLayoutBinding.calloc(1, stack)
            val oddLayoutBinding = oddLayoutBindings[0]
            oddLayoutBinding.binding(0)
            oddLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_INPUT_ATTACHMENT)
            oddLayoutBinding.descriptorCount(1)
            oddLayoutBinding.stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)

            this.countPipelineLayout = troll.pipelines.createLayout(stack, null, "GraviksTextCount")
            this.oddDescriptorSetLayout = troll.descriptors.createLayout(stack, oddLayoutBindings, "GraviksTextOdd")
            this.oddPipelineLayout = troll.pipelines.createLayout(stack, null ,"GraviksTextOdd", oddDescriptorSetLayout)
            val renderPass = createTextRenderPass(troll.vkDevice(), stack)
            this.vkRenderPass = renderPass

            val textCountVertexShader = troll.pipelines.createShaderModule(
                stack, "graviks2d/shaders/textCount.vert.spv", "TextCountVertex"
            )
            val textCountFragmentShader = troll.pipelines.createShaderModule(
                stack, "graviks2d/shaders/textCount.frag.spv", "TextCountFragment"
            )

            val ciPipelines = VkGraphicsPipelineCreateInfo.calloc(2, stack)
            val ciCountPipeline = ciPipelines[0]

            // Note: the following is shared with the regular graviks pipeline:
            // inputAssembly, viewportState, dynamicState, rasterizationState
            ciCountPipeline.`sType$Default`()
            troll.pipelines.shaderStages(
                stack, ciCountPipeline,
                ShaderInfo(VK_SHADER_STAGE_VERTEX_BIT, textCountVertexShader, null),
                ShaderInfo(VK_SHADER_STAGE_FRAGMENT_BIT, textCountFragmentShader, null)
            )
            ciCountPipeline.pVertexInputState(createTextCountPipelineVertexInput(stack))
            troll.pipelines.simpleInputAssembly(stack, ciCountPipeline)
            troll.pipelines.dynamicViewports(stack, ciCountPipeline, 1)
            troll.pipelines.dynamicStates(
                stack, ciCountPipeline, VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR
            )
            troll.pipelines.simpleRasterization(stack, ciCountPipeline, VK_CULL_MODE_NONE)
            // Note: instead of using multisampling, the text renderer simply claims bigger space for some characters and
            // downscales them when drawing
            troll.pipelines.noMultisampling(stack, ciCountPipeline)
            ciCountPipeline.pColorBlendState(createTextCountPipelineColorBlend(stack))
            ciCountPipeline.layout(this.countPipelineLayout)
            ciCountPipeline.renderPass(vkRenderPass)
            ciCountPipeline.subpass(0)

            val textOddVertexShader = troll.pipelines.createShaderModule(
                stack, "graviks2d/shaders/textOdd.vert.spv", "TextOddVertex"
            )
            val textOddFragmentShader = troll.pipelines.createShaderModule(
                stack, "graviks2d/shaders/textOdd.frag.spv", "TextOddFragment"
            )

            val ciOddPipeline = ciPipelines[1]
            ciOddPipeline.`sType$Default`()
            troll.pipelines.shaderStages(
                stack, ciOddPipeline,
                ShaderInfo(VK_SHADER_STAGE_VERTEX_BIT, textOddVertexShader, null),
                ShaderInfo(VK_SHADER_STAGE_FRAGMENT_BIT, textOddFragmentShader, null)
            )
            ciOddPipeline.pVertexInputState(createTextOddPipelineVertexInput(stack))
            troll.pipelines.simpleInputAssembly(stack, ciOddPipeline)
            troll.pipelines.dynamicViewports(stack, ciOddPipeline, 1)
            troll.pipelines.dynamicStates(
                stack, ciOddPipeline, VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR
            )
            troll.pipelines.simpleRasterization(stack, ciOddPipeline, VK_CULL_MODE_NONE)
            troll.pipelines.noMultisampling(stack, ciOddPipeline)
            ciOddPipeline.pColorBlendState(createTextOddPipelineColorBlend(stack))
            ciOddPipeline.layout(this.oddPipelineLayout)
            ciOddPipeline.renderPass(this.vkRenderPass)
            ciOddPipeline.subpass(1)

            val pPipelines = stack.callocLong(2)
            assertVkSuccess(
                vkCreateGraphicsPipelines(troll.vkDevice(), VK_NULL_HANDLE, ciPipelines, null, pPipelines),
                "vkCreateGraphicsPipeline", "GraviksTextPipeline"
            )
            this.countPipeline = pPipelines[0]
            this.oddPipeline = pPipelines[1]

            vkDestroyShaderModule(troll.vkDevice(), textCountVertexShader, null)
            vkDestroyShaderModule(troll.vkDevice(), textCountFragmentShader, null)
            vkDestroyShaderModule(troll.vkDevice(), textOddVertexShader, null)
            vkDestroyShaderModule(troll.vkDevice(), textOddFragmentShader, null)
        }
    }

    fun destroy() {
        vkDestroyPipeline(troll.vkDevice(), this.countPipeline, null)
        vkDestroyPipelineLayout(troll.vkDevice(), this.countPipelineLayout, null)
        vkDestroyPipeline(troll.vkDevice(), this.oddPipeline, null)
        vkDestroyPipelineLayout(troll.vkDevice(), this.oddPipelineLayout, null)
        vkDestroyDescriptorSetLayout(troll.vkDevice(), this.oddDescriptorSetLayout, null)
        vkDestroyRenderPass(troll.vkDevice(), this.vkRenderPass, null)
    }
}
