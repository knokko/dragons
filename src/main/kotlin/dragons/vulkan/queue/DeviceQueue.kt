package dragons.vulkan.queue

import dragons.vulkan.util.assertVkSuccess
import org.lwjgl.vulkan.VK12.vkQueueSubmit
import org.lwjgl.vulkan.VK12.vkQueueWaitIdle
import org.lwjgl.vulkan.VkQueue
import org.lwjgl.vulkan.VkSubmitInfo

class DeviceQueue(val handle: VkQueue) {
    fun submit(pSubmitInfo: VkSubmitInfo.Buffer, fence: Long) {
        synchronized(handle) {
            assertVkSuccess(vkQueueSubmit(handle, pSubmitInfo, fence), "QueueSubmit")
        }
    }

    fun waitIdle() {
        synchronized(handle) {
            assertVkSuccess(vkQueueWaitIdle(handle), "QueueWaitIdle")
        }
    }
}
