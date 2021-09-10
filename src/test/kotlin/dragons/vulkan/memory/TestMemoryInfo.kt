package dragons.vulkan.memory

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memPutInt
import org.lwjgl.system.MemoryUtil.memPutLong
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkMemoryHeap
import org.lwjgl.vulkan.VkMemoryType.HEAPINDEX
import org.lwjgl.vulkan.VkMemoryType.PROPERTYFLAGS
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties

class TestMemoryInfo {

    private class MemoryType(val propertyFlags: Int, val heapIndex: Int)
    private class MemoryHeap(val size: Long)

    private fun populateMemoryProperties(dest: VkPhysicalDeviceMemoryProperties, types: List<MemoryType>, heaps: List<MemoryHeap>) {
        if (types.size > VK_MAX_MEMORY_TYPES) {
            throw IllegalArgumentException("Too many memory types")
        }
        if (heaps.size > VK_MAX_MEMORY_HEAPS) {
            throw IllegalArgumentException("Too many memory heaps")
        }

        memPutInt(dest.address() + VkPhysicalDeviceMemoryProperties.MEMORYTYPECOUNT, types.size)
        for ((index, memoryType) in types.withIndex()) {
            if (memoryType.heapIndex >= heaps.size) {
                throw IllegalArgumentException("The heap index of memory type $index is out of range")
            }
            val destType = dest.memoryTypes(index)
            memPutInt(destType.address() + PROPERTYFLAGS, memoryType.propertyFlags)
            memPutInt(destType.address() + HEAPINDEX, memoryType.heapIndex)
        }

        memPutInt(dest.address() + VkPhysicalDeviceMemoryProperties.MEMORYHEAPCOUNT, heaps.size)
        for ((index, memoryHeap) in heaps.withIndex()) {
            val destHeap = dest.memoryHeaps(index)
            memPutLong(destHeap.address() + VkMemoryHeap.SIZE, memoryHeap.size)
            // The unit test currently doesn't use heap flags
        }
    }

    @Test
    fun testChooseMemoryTypeIndexOnlyAllowed() {
        stackPush().use { stack ->

            val memoryProperties = VkPhysicalDeviceMemoryProperties.calloc(stack)
            populateMemoryProperties(memoryProperties, listOf(
                // This one will not be in the allowed memory type indices
                MemoryType(
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
                    or VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, 0
                ),
                // This one doesn't have all required properties
                MemoryType(
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, 0
                ),
                // This one uses a heap that is too small
                MemoryType(
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
                            or VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, 1
                ),
                // Only this one is ok
                MemoryType(
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT or VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT, 0
                )
            ), listOf(
                MemoryHeap(1_000_000_000), MemoryHeap(10_000_000)
            ))

            val memoryInfo = MemoryInfo(memoryProperties)

            assertEquals(3, memoryInfo.chooseMemoryTypeIndex(
                14, 100_000_000,
                requiredPropertyFlags = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                desiredPropertyFlags = VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
            ))
            assertNull(memoryInfo.chooseMemoryTypeIndex(
                14, 100_000_000,
                requiredPropertyFlags = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
                or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                desiredPropertyFlags = VK_MEMORY_PROPERTY_HOST_CACHED_BIT
            ))
        }
    }

    @Test
    fun testChooseMemoryTypeIndexSelection() {
        stackPush().use { stack ->
            val memoryProperties = VkPhysicalDeviceMemoryProperties.calloc(stack)
            populateMemoryProperties(memoryProperties, listOf(
                MemoryType(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT, 1),
                MemoryType(
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
                    or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, 0
                ),
                MemoryType(
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
                    or VK_MEMORY_PROPERTY_PROTECTED_BIT, 0
                ),
                MemoryType(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_PROTECTED_BIT, 1)
            ), listOf(
                MemoryHeap(300_000_000), MemoryHeap(4_000_000_000)
            ))

            val memoryInfo = MemoryInfo(memoryProperties)

            assertEquals(1, memoryInfo.chooseMemoryTypeIndex(
                15, 100_000_000,
                requiredPropertyFlags = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT,
                desiredPropertyFlags = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                neutralPropertyFlags = VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
            ))

            assertEquals(0, memoryInfo.chooseMemoryTypeIndex(
                15, 1_000_000_000,
                requiredPropertyFlags = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT,
                desiredPropertyFlags = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                neutralPropertyFlags = VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
            ))
        }
    }
}
