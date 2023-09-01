package dsl.pm2.renderer

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import com.github.knokko.boiler.exceptions.VulkanFailureException.assertVkSuccess
import com.github.knokko.boiler.instance.BoilerInstance
import com.github.knokko.boiler.pipelines.ShaderInfo

class Pm2PipelineInfo(
    internal val renderPass: Long,
    internal val subpass: Int,
    internal val setBlendState: (MemoryStack, VkPipelineColorBlendStateCreateInfo) -> Unit
) {
    override fun hashCode() = renderPass.toInt() + 131 * subpass + 29 * setBlendState.hashCode()

    override fun equals(other: Any?) = other is Pm2PipelineInfo && this.renderPass == other.renderPass
            && this.subpass == other.subpass && this.setBlendState == other.setBlendState
}

internal fun createGraphicsPipeline(boiler: BoilerInstance, info: Pm2PipelineInfo, pipelineLayout: Long): Long {
    return stackPush().use { stack ->
        val ciPipelines = VkGraphicsPipelineCreateInfo.calloc(1, stack)
        val ciPipeline = ciPipelines[0]
        ciPipeline.`sType$Default`()
        ciPipeline.flags(0)

        val vertexShader = boiler.pipelines.createShaderModule(
            stack, "dsl/pm2/shaders/simple.vert.spv", "Pm2VertexShader"
        )
        val fragmentShader = boiler.pipelines.createShaderModule(
            stack, "dsl/pm2/shaders/simple.frag.spv", "Pm2FragmentShader"
        )
        boiler.pipelines.shaderStages(
            stack, ciPipeline,
            ShaderInfo(VK_SHADER_STAGE_VERTEX_BIT, vertexShader, null),
            ShaderInfo(VK_SHADER_STAGE_FRAGMENT_BIT, fragmentShader, null)
        )
        ciPipeline.pVertexInputState(createVertexInputState(stack))
        boiler.pipelines.simpleInputAssembly(stack, ciPipeline)
        boiler.pipelines.dynamicViewports(stack, ciPipeline, 1)
        boiler.pipelines.simpleRasterization(stack, ciPipeline, VK_CULL_MODE_NONE) // TODO Cull back when switching to 3D
        boiler.pipelines.noDepthStencil(stack, ciPipeline) // TODO Use depthStencil after switching to 3D

        val blendState = VkPipelineColorBlendStateCreateInfo.calloc(stack)
        info.setBlendState(stack, blendState)
        ciPipeline.pColorBlendState(blendState)
        boiler.pipelines.dynamicStates(stack, ciPipeline, VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR)
        boiler.pipelines.noMultisampling(stack, ciPipeline)
        ciPipeline.layout(pipelineLayout)
        ciPipeline.renderPass(info.renderPass)
        ciPipeline.subpass(info.subpass)

        val pPipeline = stack.callocLong(1)
        assertVkSuccess(vkCreateGraphicsPipelines(
            boiler.vkDevice(), VK_NULL_HANDLE, ciPipelines, null, pPipeline
        ), "CreateGraphicsPipelines", "Pm2Pipeline")
        val pipeline = pPipeline[0]
        boiler.debug.name(stack, pipeline, VK_OBJECT_TYPE_PIPELINE, "Pm2Pipeline")

        vkDestroyShaderModule(boiler.vkDevice(), vertexShader, null)
        vkDestroyShaderModule(boiler.vkDevice(), fragmentShader, null)

        pipeline
    }
}

internal fun createPipelineLayout(boiler: BoilerInstance, stack: MemoryStack): Pair<Long, Long> {
    val descriptorBindings = VkDescriptorSetLayoutBinding.calloc(1, stack)
    descriptorBindings.binding(0)
    descriptorBindings.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
    descriptorBindings.descriptorCount(1)
    descriptorBindings.stageFlags(VK_SHADER_STAGE_VERTEX_BIT)

    val descriptorSetLayout = boiler.descriptors.createLayout(stack, descriptorBindings, "Pm2DescriptorSetLayout")

    val pushConstants = VkPushConstantRange.calloc(1, stack)
    val pushCameraMatrix = pushConstants[0]
    pushCameraMatrix.stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
    pushCameraMatrix.offset(0)
    pushCameraMatrix.size(4 + 3 * 2 * 4) // 1 int and 1 3x2 matrix

    val pipelineLayout = boiler.pipelines.createLayout(stack, pushConstants, "Pm2PipelineLayout", descriptorSetLayout)

    return Pair(pipelineLayout, descriptorSetLayout)
}

internal const val STATIC_VERTEX_SIZE = 24

private fun createVertexInputState(stack: MemoryStack): VkPipelineVertexInputStateCreateInfo {
    val bindings = VkVertexInputBindingDescription.calloc(1, stack)
    val staticBinding = bindings[0]
    staticBinding.binding(0)
    staticBinding.stride(STATIC_VERTEX_SIZE)
    staticBinding.inputRate(VK_VERTEX_INPUT_RATE_VERTEX)

    val attributes = VkVertexInputAttributeDescription.calloc(3, stack)
    val attributePosition = attributes[0]
    attributePosition.location(0)
    attributePosition.binding(0)
    attributePosition.format(VK_FORMAT_R32G32_SFLOAT)
    attributePosition.offset(0)

    val attributeColor = attributes[1]
    attributeColor.location(1)
    attributeColor.binding(0)
    attributeColor.format(VK_FORMAT_R32G32B32_SFLOAT)
    attributeColor.offset(8)

    val attributeMatrix = attributes[2]
    attributeMatrix.location(2)
    attributeMatrix.binding(0)
    attributeMatrix.format(VK_FORMAT_R32_SINT)
    attributeMatrix.offset(20)

    val ciVertexInput = VkPipelineVertexInputStateCreateInfo.calloc(stack)
    ciVertexInput.`sType$Default`()
    ciVertexInput.flags(0)
    ciVertexInput.pVertexBindingDescriptions(bindings)
    ciVertexInput.pVertexAttributeDescriptions(attributes)

    return ciVertexInput
}
