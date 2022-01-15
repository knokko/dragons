package dragons.vr.openxr

import dragons.vr.assertXrSuccess
import org.lwjgl.openxr.XR10.XR_TYPE_EXTENSION_PROPERTIES
import org.lwjgl.openxr.XR10.xrEnumerateInstanceExtensionProperties
import org.lwjgl.openxr.XrExtensionProperties
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memPutInt
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
            memPutInt(
                pExtensions[index].address() + XrExtensionProperties.TYPE,
                XR_TYPE_EXTENSION_PROPERTIES
            )
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
