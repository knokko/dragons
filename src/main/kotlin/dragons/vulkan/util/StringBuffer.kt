package dragons.vulkan.util

import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memUTF8
import org.lwjgl.vulkan.VkExtensionProperties
import org.lwjgl.vulkan.VkLayerProperties

fun encodeStrings(strings: Collection<String>, destStack: MemoryStack): PointerBuffer {
    val pResult = destStack.callocPointer(strings.size)
    for ((index, string) in strings.withIndex()) {
        pResult.put(index, destStack.UTF8(string))
    }
    return pResult
}

fun decodeStringsToSet(stringBuffer: PointerBuffer): Set<String> {
    val result = mutableSetOf<String>()
    for (index in stringBuffer.position() until stringBuffer.limit()) {
        result.add(memUTF8(stringBuffer[index]))
    }
    return result
}

fun extensionBufferToSet(extensionBuffer: VkExtensionProperties.Buffer): Set<String> {
    val result = mutableSetOf<String>()
    for (extension in extensionBuffer) {
        result.add(extension.extensionNameString())
    }
    return result
}

fun layerBufferToSet(layerBuffer: VkLayerProperties.Buffer): Set<String> {
    val result = mutableSetOf<String>()
    for (layer in layerBuffer) {
        result.add(layer.layerNameString())
    }
    return result
}
