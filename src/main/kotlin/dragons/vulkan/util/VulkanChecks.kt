package dragons.vulkan.util

import dragons.util.getIntConstantName
import org.lwjgl.vulkan.VK12
import org.slf4j.LoggerFactory
import kotlin.jvm.Throws

@Throws(VulkanFailureException::class)
fun assertVkSuccess(returnCode: Int, functionName: String, functionContext: String? = null) {
    if (returnCode < 0) {
        val contextPart = if (functionContext != null) { "($functionContext) " } else { "" }
        val returnCodeName = getIntConstantName(VK12::class, returnCode, "VK_ERROR_")
        LoggerFactory.getLogger("Vulkan").error("vk$functionName ${contextPart}returned $returnCode ($returnCodeName)")
        throw VulkanFailureException("vk$functionName ${contextPart}returned $returnCode ($returnCodeName)")
    }
}

class VulkanFailureException(message: String): Exception(message)
