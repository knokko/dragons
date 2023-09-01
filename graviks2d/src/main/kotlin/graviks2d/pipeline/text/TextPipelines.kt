package graviks2d.pipeline.text

import com.github.knokko.boiler.exceptions.VulkanFailureException.assertVkSuccess
import com.github.knokko.boiler.instance.BoilerInstance
import com.github.knokko.boiler.pipelines.ShaderInfo
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo

const val TEXT_COLOR_FORMAT = VK_FORMAT_R8_UNORM

internal class TextPipeline(
    val boiler: BoilerInstance
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

            this.countPipelineLayout = boiler.pipelines.createLayout(stack, null, "GraviksTextCount")
            this.oddDescriptorSetLayout = boiler.descriptors.createLayout(stack, oddLayoutBindings, "GraviksTextOdd")
            this.oddPipelineLayout = boiler.pipelines.createLayout(stack, null ,"GraviksTextOdd", oddDescriptorSetLayout)
            val renderPass = createTextRenderPass(boiler, stack)
            this.vkRenderPass = renderPass

            val textCountVertexShader = boiler.pipelines.createShaderModule(
                stack, "graviks2d/shaders/textCount.vert.spv", "TextCountVertex"
            )
            val textCountFragmentShader = boiler.pipelines.createShaderModule(
                stack, "graviks2d/shaders/textCount.frag.spv", "TextCountFragment"
            )

            val ciPipelines = VkGraphicsPipelineCreateInfo.calloc(2, stack)
            val ciCountPipeline = ciPipelines[0]

            // Note: the following is shared with the regular graviks pipeline:
            // inputAssembly, viewportState, dynamicState, rasterizationState
            ciCountPipeline.`sType$Default`()
            boiler.pipelines.shaderStages(
                stack, ciCountPipeline,
                ShaderInfo(VK_SHADER_STAGE_VERTEX_BIT, textCountVertexShader, null),
                ShaderInfo(VK_SHADER_STAGE_FRAGMENT_BIT, textCountFragmentShader, null)
            )
            ciCountPipeline.pVertexInputState(createTextCountPipelineVertexInput(stack))
            boiler.pipelines.simpleInputAssembly(stack, ciCountPipeline)
            boiler.pipelines.dynamicViewports(stack, ciCountPipeline, 1)
            boiler.pipelines.dynamicStates(
                stack, ciCountPipeline, VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR
            )
            boiler.pipelines.simpleRasterization(stack, ciCountPipeline, VK_CULL_MODE_NONE)
            // Note: instead of using multisampling, the text renderer simply claims bigger space for some characters and
            // downscales them when drawing
            boiler.pipelines.noMultisampling(stack, ciCountPipeline)
            ciCountPipeline.pColorBlendState(createTextCountPipelineColorBlend(stack))
            ciCountPipeline.layout(this.countPipelineLayout)
            ciCountPipeline.renderPass(vkRenderPass)
            ciCountPipeline.subpass(0)

            val textOddVertexShader = boiler.pipelines.createShaderModule(
                stack, "graviks2d/shaders/textOdd.vert.spv", "TextOddVertex"
            )
            val textOddFragmentShader = boiler.pipelines.createShaderModule(
                stack, "graviks2d/shaders/textOdd.frag.spv", "TextOddFragment"
            )

            val ciOddPipeline = ciPipelines[1]
            ciOddPipeline.`sType$Default`()
            boiler.pipelines.shaderStages(
                stack, ciOddPipeline,
                ShaderInfo(VK_SHADER_STAGE_VERTEX_BIT, textOddVertexShader, null),
                ShaderInfo(VK_SHADER_STAGE_FRAGMENT_BIT, textOddFragmentShader, null)
            )
            ciOddPipeline.pVertexInputState(createTextOddPipelineVertexInput(stack))
            boiler.pipelines.simpleInputAssembly(stack, ciOddPipeline)
            boiler.pipelines.dynamicViewports(stack, ciOddPipeline, 1)
            boiler.pipelines.dynamicStates(
                stack, ciOddPipeline, VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR
            )
            boiler.pipelines.simpleRasterization(stack, ciOddPipeline, VK_CULL_MODE_NONE)
            boiler.pipelines.noMultisampling(stack, ciOddPipeline)
            ciOddPipeline.pColorBlendState(createTextOddPipelineColorBlend(stack))
            ciOddPipeline.layout(this.oddPipelineLayout)
            ciOddPipeline.renderPass(this.vkRenderPass)
            ciOddPipeline.subpass(1)

            val pPipelines = stack.callocLong(2)
            assertVkSuccess(
                vkCreateGraphicsPipelines(boiler.vkDevice(), VK_NULL_HANDLE, ciPipelines, null, pPipelines),
                "vkCreateGraphicsPipeline", "GraviksTextPipeline"
            )
            this.countPipeline = pPipelines[0]
            this.oddPipeline = pPipelines[1]

            vkDestroyShaderModule(boiler.vkDevice(), textCountVertexShader, null)
            vkDestroyShaderModule(boiler.vkDevice(), textCountFragmentShader, null)
            vkDestroyShaderModule(boiler.vkDevice(), textOddVertexShader, null)
            vkDestroyShaderModule(boiler.vkDevice(), textOddFragmentShader, null)
        }
    }

    fun destroy() {
        vkDestroyPipeline(boiler.vkDevice(), this.countPipeline, null)
        vkDestroyPipelineLayout(boiler.vkDevice(), this.countPipelineLayout, null)
        vkDestroyPipeline(boiler.vkDevice(), this.oddPipeline, null)
        vkDestroyPipelineLayout(boiler.vkDevice(), this.oddPipelineLayout, null)
        vkDestroyDescriptorSetLayout(boiler.vkDevice(), this.oddDescriptorSetLayout, null)
        vkDestroyRenderPass(boiler.vkDevice(), this.vkRenderPass, null)
    }
}
