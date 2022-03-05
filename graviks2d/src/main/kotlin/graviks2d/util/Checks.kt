package graviks2d.util

import org.lwjgl.vulkan.VK10.VK_TIMEOUT

internal fun assertSuccess(result: Int, functionName: String) {
    if (result < 0 || result == VK_TIMEOUT) {
        throw RuntimeException("$functionName returned $result")
    }
}
