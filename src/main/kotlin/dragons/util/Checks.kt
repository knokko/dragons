package dragons.util

import org.lwjgl.vulkan.VK12
import org.slf4j.LoggerFactory.getLogger
import java.lang.reflect.Modifier
import kotlin.jvm.Throws
import kotlin.reflect.KClass

@Throws(VulkanFailureException::class)
fun assertVkSuccess(returnCode: Int, functionName: String, functionContext: String? = null) {
    if (returnCode < 0) {
        val contextPart = if (functionContext != null) { "($functionContext) " } else { "" }
        val returnCodeName = getIntConstantName(VK12::class, returnCode, "VK_ERROR_")
        getLogger("Vulkan").error("vk$functionName ${contextPart}returned $returnCode $returnCodeName")
        throw VulkanFailureException("vk$functionName ${contextPart}returned $returnCode $returnCodeName")
    }
}

fun getIntConstantName(
    targetClass: KClass<*>, value: Number,
    prefix: String = "", suffix: String = "", fallback: String = "unknown"
): String {
    val foundField = targetClass.java.fields.find { candidate ->
        if (Modifier.isStatic(candidate.modifiers) && Modifier.isFinal(candidate.modifiers)
                && candidate.name.startsWith(prefix) && candidate.name.endsWith(suffix)) {
            candidate.get(null) == value
        } else {
            false
        }
    }

    return foundField?.name ?: fallback
}

class VulkanFailureException(message: String): Exception(message)
