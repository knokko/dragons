package dragons.vulkan.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkApplicationInfo
import org.lwjgl.vulkan.VkBaseOutStructure
import org.lwjgl.vulkan.VkDeviceCreateInfo
import org.lwjgl.vulkan.VkInstanceCreateInfo

class TestNextChain {

    @Test
    fun testCombineNextChainsBothNull() {
        assertNull(combineNextChains(null, null))
    }

    @Test
    fun testCombineNextChainsOnlyA() {
        val a = VkBaseOutStructure.create()
        val combined = combineNextChains(a, null)!!
        assertEquals(a.address(), combined.address())
        assertNull(a.pNext())
    }

    @Test
    fun testCombineNextChainsOnlyB() {
        val b = VkBaseOutStructure.create()
        val combined = combineNextChains(null, b)!!
        assertEquals(b.address(), combined.address())
        assertNull(b.pNext())
    }

    @Test
    fun testCombineSingleStructNextChains() {
        val a = VkBaseOutStructure.create()
        val b = VkBaseOutStructure.create()
        val combined = combineNextChains(a, b)!!
        assertTrue(a.address() == combined.address() || b.address() == combined.address())
        assertTrue(a.address() == combined.pNext()!!.address() || b.address() == combined.pNext()!!.address())
        assertNull(combined.pNext()!!.pNext())
    }

    @Test
    fun testCombineMultiStructNextChains() {
        val a1 = VkBaseOutStructure.create()
        val a2 = VkBaseOutStructure.create()
        a1.pNext(a2)

        val b1 = VkBaseOutStructure.create()
        val b2 = VkBaseOutStructure.create()
        val b3 = VkBaseOutStructure.create()
        b1.pNext(b2)
        b2.pNext(b3)

        val combined = combineNextChains(a1, b1)!!
        if (combined.address() == a1.address()) {
            assertEquals(combined.pNext()!!.address(), a2.address())
            assertEquals(combined.pNext()!!.pNext()!!.address(), b1.address())
            assertEquals(combined.pNext()!!.pNext()!!.pNext()!!.address(), b2.address())
            assertEquals(combined.pNext()!!.pNext()!!.pNext()!!.pNext()!!.address(), b3.address())
            assertNull(combined.pNext()!!.pNext()!!.pNext()!!.pNext()!!.pNext())
        } else {
            assertEquals(b1.address(), combined.address())
            assertEquals(combined.pNext()!!.address(), b2.address())
            assertEquals(combined.pNext()!!.pNext()!!.address(), b3.address())
            assertEquals(combined.pNext()!!.pNext()!!.pNext()!!.address(), a1.address())
            assertEquals(combined.pNext()!!.pNext()!!.pNext()!!.pNext()!!.address(), a2.address())
            assertNull(combined.pNext()!!.pNext()!!.pNext()!!.pNext()!!.pNext())
        }
    }

    @Test
    fun testFindInNullNextChain() {
        assertNull(findInNextChain(null, VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO))
    }

    @Test
    fun testDontFindInNextChain() {
        val a = VkInstanceCreateInfo.create()
        a.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)

        val b = VkDeviceCreateInfo.create()
        b.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)

        a.pNext(b.address())

        assertNull(findInNextChain(a, VK_STRUCTURE_TYPE_APPLICATION_INFO))
    }

    @Test
    fun testFindInNextChain() {
        val a = VkInstanceCreateInfo.create()
        a.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)

        val b = VkDeviceCreateInfo.create()
        b.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)

        val c = VkApplicationInfo.create()
        c.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)

        a.pNext(b.address())
        b.pNext(c.address())

        assertEquals(a.address(), findInNextChain(a, VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)!!.address())
        assertEquals(b.address(), findInNextChain(a, VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)!!.address())
        assertEquals(c.address(), findInNextChain(a, VK_STRUCTURE_TYPE_APPLICATION_INFO)!!.address())
    }
}
