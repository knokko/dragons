package dragons.vr

import dragons.state.StaticGraphicsState
import org.joml.Matrix4f
import org.lwjgl.vulkan.VkPhysicalDevice

interface VrManager {
    fun getVulkanInstanceExtensions(availableExtensions: Set<String>): Set<String>

    fun getVulkanDeviceExtensions(
        device: VkPhysicalDevice, deviceName: String, availableExtensions: Set<String>
    ): Set<String>

    fun getWidth(): Int

    fun getHeight(): Int

    /**
     * Should only be used by the game core
     */
    fun setGraphicsState(graphicsState: StaticGraphicsState)

    /**
     * Blocks the current thread until the right moment to start rendering the next frame. It will return a pair
     * `(leftEyeMatrix, rightEyeMatrix)`. These 'eye matrices' are basically the VR equivalent of the traditional
     * `projectileMatrix * viewMatrix` in non-VR 3d games.
     */
    fun prepareRender(): Pair<Matrix4f, Matrix4f>?

    fun submitFrames()

    fun destroy()
}
