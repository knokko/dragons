package dragons.vr.openxr

import org.lwjgl.openxr.XR10.xrEnumerateInstanceExtensionProperties
import org.lwjgl.openxr.XrExtensionProperties
import org.lwjgl.system.MemoryStack.stackPush
import org.slf4j.Logger
import java.nio.ByteBuffer

internal fun getAvailableOpenXrExtensions(logger: Logger): Set<String> {
    return stackPush().use { stack ->
        val pNumExtensions = stack.callocInt(1)
        assertXrSuccess(
            xrEnumerateInstanceExtensionProperties(null as ByteBuffer?, pNumExtensions, null),
            "EnumerateInstanceExtensionProperties", "count"
        )
        val numExtensions = pNumExtensions[0]

        val pExtensions = XrExtensionProperties.calloc(numExtensions, stack)
        for (index in 0 until numExtensions) {
            pExtensions[index].`type$Default`()
        }
        assertXrSuccess(
            xrEnumerateInstanceExtensionProperties(null as ByteBuffer?, pNumExtensions, pExtensions),
            "EnumerateInstanceExtensionProperties", "extensions"
        )

        val extensions = mutableSetOf<String>()
        for (index in 0 until numExtensions) {
            extensions.add(pExtensions[index].extensionNameString())
        }

        logger.info("${extensions.size} OpenXR extensions are supported:")
        for (extension in extensions) {
            logger.info(extension)
        }
        extensions
    }
}
