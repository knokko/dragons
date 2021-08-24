package dragons.util

import java.lang.reflect.Modifier
import kotlin.reflect.KClass

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
