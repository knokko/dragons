package dsl.pm2.renderer

import org.lwjgl.vulkan.VK10.VK_SUCCESS

fun checkReturnValue(result: Int, functionName: String) {
    if (result != VK_SUCCESS) {
        throw RuntimeException("vk$functionName returned $result")
    }
}
