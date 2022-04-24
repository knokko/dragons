package dragons.vulkan.util

import dragons.util.getIntConstantName
import org.lwjgl.util.vma.Vma
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.VK_TIMEOUT
import org.slf4j.LoggerFactory
import kotlin.jvm.Throws

@Throws(VulkanFailureException::class)
fun assertVkSuccess(returnCode: Int, functionName: String, functionContext: String? = null) {
    // I don't consider timeout a success
    if (returnCode < 0 || returnCode == VK_TIMEOUT) {
        val contextPart = if (functionContext != null) { "($functionContext) " } else { "" }
        val returnCodeName = if (returnCode == VK_TIMEOUT) "VK_TIMEOUT" else getIntConstantName(VK10::class, returnCode, "VK_ERROR_")
        LoggerFactory.getLogger("Vulkan").error("vk$functionName ${contextPart}returned $returnCode ($returnCodeName)")
        throw VulkanFailureException("vk$functionName ${contextPart}returned $returnCode ($returnCodeName)")
    }
}

@Throws(VulkanFailureException::class)
fun assertVmaSuccess(returnCode: Int, functionName: String, functionContext: String? = null) {
    if (returnCode != 0) {
        val contextPart = if (functionContext != null) { "($functionContext) " } else { "" }
        val returnCodeName = getIntConstantName(VK10::class, returnCode, "VK_ERROR_")
        LoggerFactory.getLogger("Vulkan").error("vma$functionName ${contextPart}returned $returnCode ($returnCodeName)")
        throw VulkanFailureException("vma$functionName ${contextPart}returned $returnCode ($returnCodeName)")
    }
}

class VulkanFailureException(message: String): Exception(message)
