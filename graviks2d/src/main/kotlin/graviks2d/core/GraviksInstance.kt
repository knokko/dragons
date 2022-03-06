package graviks2d.core

import graviks2d.pipeline.GraviksPipeline
import org.lwjgl.vulkan.*

class GraviksInstance(
    val instance: VkInstance,
    val physicalDevice: VkPhysicalDevice,
    val device: VkDevice,
    val vmaAllocator: Long,
    val queueFamilyIndex: Int,
    /**
     * This instance will use `queueSubmit` instead of `vkQueueSubmit`. This method is expected to call `vkQueueSubmit`,
     * but possibly in a synchronized manner. This method has 2 purposes:
     * - The user can choose which `VkQueue` will be used by this `Graviks2dInstance`.
     * - If the user also needs this `VkQueue` for other purposes, the user can put synchronization logic inside this
     * method (since the `queue` used in `vkQueueSubmit` **must** be externally synchronized). Note that this
     * `Graviks2dInstance` will synchronize all calls to `queueSubmit`, so user synchronization is only needed if the
     * used `VkQueue` is also used for other purposes.
     */
    private val queueSubmit: (VkSubmitInfo.Buffer, Long) -> Int
) {

    internal val pipeline = GraviksPipeline(this)

    fun synchronizedQueueSubmit(pSubmitInfo: VkSubmitInfo.Buffer, fence: Long): Int {
        return synchronized(queueSubmit) {
            queueSubmit(pSubmitInfo, fence)
        }
    }

    /**
     * Note: you must destroy all contexts **before** destroying this instance.
     */
    fun destroy() {
        pipeline.destroy()
    }
}
