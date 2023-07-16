package dragons.vulkan.util

import dragons.util.getIntConstantName
import org.lwjgl.vulkan.VK10
import org.slf4j.LoggerFactory
import kotlin.jvm.Throws

// TODO Migrate this to troll-engine
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
