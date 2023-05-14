package dsl.pm2.renderer.pipeline

import dsl.pm2.renderer.Pm2Instance
import dsl.pm2.renderer.checkReturnValue
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo
import org.lwjgl.vulkan.VkShaderModuleCreateInfo

internal fun createShaderStages(
    stack: MemoryStack, vertexShaderModule: Long, fragmentShaderModule: Long
): VkPipelineShaderStageCreateInfo.Buffer {
    val stages = VkPipelineShaderStageCreateInfo.calloc(2, stack)
    populateVertexShader(stages[0], vertexShaderModule, stack)
    populateFragmentShader(stages[1], fragmentShaderModule, stack)
    return stages
}

private fun populateVertexShader(ciShader: VkPipelineShaderStageCreateInfo, vertexShaderModule: Long, stack: MemoryStack) {
    ciShader.`sType$Default`()
    ciShader.stage(VK_SHADER_STAGE_VERTEX_BIT)
    ciShader.module(vertexShaderModule)
    ciShader.pName(stack.UTF8("main"))
    ciShader.pSpecializationInfo(null) // Note: I will probably use this later
}

private fun populateFragmentShader(ciShader: VkPipelineShaderStageCreateInfo, fragmentShaderModule: Long, stack: MemoryStack) {
    ciShader.`sType$Default`()
    ciShader.stage(VK_SHADER_STAGE_FRAGMENT_BIT)
    ciShader.module(fragmentShaderModule)
    ciShader.pName(stack.UTF8("main"))
    ciShader.pSpecializationInfo(null)
}

private fun createShaderModule(vkDevice: VkDevice, stack: MemoryStack, extension: String): Long {
    val resourcePath = "dsl/pm2/shaders/simple.$extension/.spv"
    val inputStream = Pm2Instance::class.java.classLoader.getResourceAsStream(resourcePath)!!
    val byteArray = inputStream.readAllBytes()
    inputStream.close()

    val byteBuffer = memAlloc(byteArray.size)
    byteBuffer.put(0, byteArray)

    val ciModule = VkShaderModuleCreateInfo.calloc(stack)
    ciModule.`sType$Default`()
    ciModule.pCode(byteBuffer)

    val pModule = stack.callocLong(1)
    checkReturnValue(vkCreateShaderModule(vkDevice, ciModule, null, pModule))

    memFree(byteBuffer)
    return pModule[0]
}

internal fun createVertexShaderModule(vkDevice: VkDevice, stack: MemoryStack) = createShaderModule(vkDevice, stack, "vert")

internal fun createFragmentShaderModule(vkDevice: VkDevice, stack: MemoryStack) = createShaderModule(vkDevice, stack, "frag")
