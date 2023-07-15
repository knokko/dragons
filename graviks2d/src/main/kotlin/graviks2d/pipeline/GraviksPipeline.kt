package graviks2d.pipeline

import graviks2d.context.TARGET_COLOR_FORMAT
import graviks2d.core.GraviksInstance
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo
import troll.exceptions.VulkanFailureException.assertVkSuccess
import troll.pipelines.ShaderInfo


internal class GraviksPipeline(
    val instance: GraviksInstance
) {

    val vkPipeline: Long
    val vkPipelineLayout: Long
    val vkDescriptorSetLayout: Long
    val vkRenderPass: Long

    init {
        stackPush().use { stack ->

            val setLayoutBindings = VkDescriptorSetLayoutBinding.calloc(4, stack)
            val shaderStorageBinding = setLayoutBindings[0]
            shaderStorageBinding.binding(0)
            shaderStorageBinding.descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
            shaderStorageBinding.descriptorCount(1)
            shaderStorageBinding.stageFlags(VK_SHADER_STAGE_VERTEX_BIT or VK_SHADER_STAGE_FRAGMENT_BIT)
            val textureSamplerBinding = setLayoutBindings[1]
            textureSamplerBinding.binding(1)
            textureSamplerBinding.descriptorType(VK_DESCRIPTOR_TYPE_SAMPLER)
            textureSamplerBinding.descriptorCount(2)
            textureSamplerBinding.stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
            val texturesBinding = setLayoutBindings[2]
            texturesBinding.binding(2)
            texturesBinding.descriptorType(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
            texturesBinding.descriptorCount(instance.maxNumDescriptorImages)
            texturesBinding.stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
            val textTextureBinding = setLayoutBindings[3]
            textTextureBinding.binding(3)
            textTextureBinding.descriptorType(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
            textTextureBinding.descriptorCount(1)
            textTextureBinding.stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
            this.vkDescriptorSetLayout = instance.troll.descriptors.createLayout(
                stack, setLayoutBindings, "GraviksBase"
            )
            this.vkPipelineLayout = instance.troll.pipelines.createLayout(
                stack, null, "GraviksBase", vkDescriptorSetLayout
            )

            val renderPass = createGraviksRenderPass(
                instance.troll.vkDevice(), TARGET_COLOR_FORMAT, stack
            )
            this.vkRenderPass = renderPass

            val vertexShader = instance.troll.pipelines.createShaderModule(
                stack, "graviks2d/shaders/basic.vert.spv", "GraviksBasicVertexShader"
            )
            val fragmentShader = instance.troll.pipelines.createShaderModule(
                stack, "graviks2d/shaders/basic.frag.spv", "GraviksBasicFragmentShader"
            )

            val ciPipelines = VkGraphicsPipelineCreateInfo.calloc(1, stack)
            val ciPipeline = ciPipelines[0]
            ciPipeline.`sType$Default`()
            instance.troll.pipelines.shaderStages(
                stack, ciPipeline,
                ShaderInfo(VK_SHADER_STAGE_VERTEX_BIT, vertexShader, null),
                ShaderInfo(VK_SHADER_STAGE_FRAGMENT_BIT, fragmentShader, null)
            )
            ciPipeline.pVertexInputState(createGraviksPipelineVertexInput(stack))
            instance.troll.pipelines.simpleInputAssembly(stack, ciPipeline)
            instance.troll.pipelines.dynamicViewports(stack, ciPipeline, 1)
            instance.troll.pipelines.dynamicStates(
                stack, ciPipeline, VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR
            )
            instance.troll.pipelines.simpleRasterization(stack, ciPipeline, VK_CULL_MODE_NONE)
            instance.troll.pipelines.noMultisampling(stack, ciPipeline)
            instance.troll.pipelines.simpleColorBlending(stack, ciPipeline, 1)
            ciPipeline.layout(vkPipelineLayout)
            ciPipeline.renderPass(vkRenderPass)
            ciPipeline.subpass(0)

            val pPipeline = stack.callocLong(1)
            assertVkSuccess(
                vkCreateGraphicsPipelines(instance.troll.vkDevice(), VK_NULL_HANDLE, ciPipelines, null, pPipeline),
                "vkCreateGraphicsPipeline", "GraviksCore"
            )
            this.vkPipeline = pPipeline[0]

            vkDestroyShaderModule(instance.troll.vkDevice(), vertexShader, null)
            vkDestroyShaderModule(instance.troll.vkDevice(), fragmentShader, null)
        }
    }

    fun destroy() {
        vkDestroyPipeline(this.instance.troll.vkDevice(), this.vkPipeline, null)
        vkDestroyPipelineLayout(this.instance.troll.vkDevice(), this.vkPipelineLayout, null)
        vkDestroyDescriptorSetLayout(this.instance.troll.vkDevice(), this.vkDescriptorSetLayout, null)
        vkDestroyRenderPass(this.instance.troll.vkDevice(), this.vkRenderPass, null)
    }
}
