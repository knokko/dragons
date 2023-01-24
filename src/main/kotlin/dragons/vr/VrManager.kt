package dragons.vr

import dragons.plugin.interfaces.vulkan.VulkanStaticMemoryUser
import dragons.state.StaticGraphicsState
import dragons.geometry.Angle
import dragons.geometry.Distance
import dragons.vr.controls.DragonControls
import dragons.vulkan.RenderImageInfo
import dragons.vulkan.queue.QueueManager
import dragons.vulkan.util.assertVkSuccess
import org.joml.Vector2f
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import org.slf4j.Logger

interface VrManager {
    fun getVulkanInstanceExtensions(availableExtensions: Set<String>): Set<String>

    fun getVulkanDeviceExtensions(
        device: VkPhysicalDevice, deviceName: String, availableExtensions: Set<String>
    ): Set<String>

    /**
     * The game core should use this method to create the Vulkan instance rather than calling `vkCreateInstance`
     * directly because the `OpenXrManager` needs to use `xrCreateVulkanInstanceKHR` instead. The other implementations
     * will simply call `vkCreateInstance`.
     */
    fun createVulkanInstance(
        ciInstance: VkInstanceCreateInfo,
        pInstance: PointerBuffer
    ): Int {
        return vkCreateInstance(ciInstance, null, pInstance)
    }

    /**
     * The game core should use this method to enumerate the Vulkan physical devices rather than calling
     * `vkEnumeratePhysicalDevices` directly because the `OpenXrManager` requires a specific physical device (it will
     * return only that physical device rather than all available physical devices).
     */
    fun enumerateVulkanPhysicalDevices(
        logger: Logger,
        vkInstance: VkInstance,
        stack: MemoryStack
    ): PointerBuffer {
        val pNumDevices = stack.callocInt(1)
        assertVkSuccess(
            vkEnumeratePhysicalDevices(vkInstance, pNumDevices, null),
            "EnumeratePhysicalDevices", "count"
        )
        val numDevices = pNumDevices[0]
        logger.info("There are $numDevices physical devices with Vulkan support")

        val pPhysicalDevices = stack.callocPointer(numDevices)
        assertVkSuccess(
            vkEnumeratePhysicalDevices(vkInstance, pNumDevices, pPhysicalDevices),
            "EnumeratePhysicalDevices", "device pointers"
        )

        return pPhysicalDevices
    }

    /**
     * The game core should use this method to create the Vulkan logical device rather than calling `vkCreateDevice`
     * directly because the `OpenXrManager` needs to use `xrCreateVulkanDeviceKHR` instead. The other implementations
     * will simply call `vkCreateDevice`.
     */
    fun createVulkanLogicalDevice(
        physicalDevice: VkPhysicalDevice, ciDevice: VkDeviceCreateInfo, pDevice: PointerBuffer
    ): Int {
        return vkCreateDevice(physicalDevice, ciDevice, null, pDevice)
    }

    /**
     * Gives the VrManager the opportunity to claim some static GPU resources.
     */
    fun claimStaticMemory(agent: VulkanStaticMemoryUser.Agent, queueManager: QueueManager, renderImageInfo: RenderImageInfo) {
        // Not all VR managers need this
    }

    fun getWidth(): Int

    fun getHeight(): Int

    /**
     * Should only be used by the game core
     */
    fun setGraphicsState(graphicsState: StaticGraphicsState)

    /**
     * Blocks the current thread until the right moment to start rendering the next frame.
     *
     * When the orientation and position of the player can't be tracked for some reason, this method will return null.
     */
    fun prepareRender(nearPlane: Distance, farPlane: Distance, extraRotationY: Angle): CameraMatrices?

    /**
     * This should be called right before the first `vkQueueSubmit` of each frame. This helps the VR manager with
     * getting better timing information and thus more accurate view matrices.
     */
    fun markFirstFrameQueueSubmit()

    fun resolveAndSubmitFrames(waitSemaphore: Long?, takeScreenshot: Boolean)

    fun getDragonControls(): DragonControls {
        return DragonControls(
            walkDirection = Vector2f(), cameraTurnDirection = 0f, isSpitting = false, isUsingPower = false,
            shouldToggleMenu = false, shouldToggleLeftWing = false, shouldToggleRightWing = false,
            isGrabbingLeft = false, isGrabbingRight = false,
            leftHandPosition = null, rightHandPosition = null, leftHandOrientation = null, rightHandOrientation = null,
            leftHandAimPosition = null, rightHandAimPosition = null, leftHandAimOrientation = null, rightHandAimOrientation = null
        )
    }

    fun destroy()

    /**
     * This should cause `shouldStop()` to return `true` soon
     */
    fun requestStop()

    fun shouldStop(): Boolean
}
