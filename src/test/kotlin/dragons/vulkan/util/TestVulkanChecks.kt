package dragons.vulkan.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.lwjgl.vulkan.VK12.VK_ERROR_DEVICE_LOST
import org.lwjgl.vulkan.VK12.VK_SUCCESS

class TestVulkanChecks {

    @Test
    fun testAssertVkSuccess() {
        assertVkSuccess(VK_SUCCESS, "CreateInstance")
        assertVkSuccess(VK_SUCCESS, "CreateInstance", "test")
    }

    @Test
    fun testAssertVkFailure() {
        try {
            assertVkSuccess(VK_ERROR_DEVICE_LOST, "CreateDevice")
            fail()
        } catch (failure: VulkanFailureException) {
            assertEquals("vkCreateDevice returned -4 (VK_ERROR_DEVICE_LOST)", failure.message)
        }

        try {
            assertVkSuccess(VK_ERROR_DEVICE_LOST, "CreateDevice", "test")
            fail()
        } catch (failure: VulkanFailureException) {
            assertEquals("vkCreateDevice (test) returned -4 (VK_ERROR_DEVICE_LOST)", failure.message)
        }
    }
}
