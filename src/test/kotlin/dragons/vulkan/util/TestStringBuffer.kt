package dragons.vulkan.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memUTF8
import org.lwjgl.vulkan.VkExtensionProperties
import org.lwjgl.vulkan.VkLayerProperties

private val testStrings = setOf("Hello, World!", "HeyThere", "", " ")

class TestStringBuffer {

    @Test
    fun testEncodeAndDecodeStrings() {
        stackPush().use { stack ->
            assertEquals(testStrings, decodeStringsToSet(encodeStrings(testStrings, stack)))
        }
    }

    @Test
    fun testExtensionBufferToSet() {
        stackPush().use { stack ->
            val extensionProperties = VkExtensionProperties.callocStack(testStrings.size, stack)
            for ((index, testString) in testStrings.withIndex()) {
                memUTF8(testString, true, extensionProperties[index].extensionName())
            }
            assertEquals(testStrings, extensionBufferToSet(extensionProperties))
        }
    }

    @Test
    fun testLayerBufferToSet() {
        stackPush().use { stack ->
            val layerProperties = VkLayerProperties.callocStack(testStrings.size, stack)
            for ((index, testString) in testStrings.withIndex()) {
                memUTF8(testString, true, layerProperties[index].layerName())
            }
            assertEquals(testStrings, layerBufferToSet(layerProperties))
        }
    }
}
