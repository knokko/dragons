package graviks2d.pipeline

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_DYNAMIC_STATE_SCISSOR
import org.lwjgl.vulkan.VK10.VK_DYNAMIC_STATE_VIEWPORT
import org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo

internal fun createGraviksPipelineViewport(
    stack: MemoryStack
): VkPipelineViewportStateCreateInfo {

    val ciViewport = VkPipelineViewportStateCreateInfo.calloc(stack)
    ciViewport.`sType$Default`()
    ciViewport.viewportCount(1)
    ciViewport.pViewports(null)
    ciViewport.scissorCount(1)
    ciViewport.pScissors(null)

    return ciViewport
}

internal fun createGraviksPipelineDynamics(
    stack: MemoryStack
): VkPipelineDynamicStateCreateInfo {

    val ciDynamic = VkPipelineDynamicStateCreateInfo.calloc(stack)
    ciDynamic.`sType$Default`()
    ciDynamic.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR))

    return ciDynamic
}
