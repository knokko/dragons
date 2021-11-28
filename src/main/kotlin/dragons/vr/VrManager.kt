package dragons.vr

import dragons.state.StaticGraphicsState
import org.joml.Matrix4f
import org.joml.Vector3f
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
     * Blocks the current thread until the right moment to start rendering the next frame. It will return a triple
     * `(averageEyePosition, leftEyeMatrix, rightEyeMatrix)`.
     *
     * ## Eye matrices
     * `leftEyeMatrix` and `rightEyeMatrix` are basically the VR equivalent of the traditional
     * `projectionMatrix * viewMatrix` in non-VR 3d games.
     *
     * ## Average eye position
     * The `averageEyePosition` is the position of the point that is exactly between the left eye and the right eye
     * (so `0.5 * leftEyePosition + 0.5 * rightEyePosition`). This position is relative to some *origin on the ground*
     * in the *real* world and uses the *meter* as distance unit.
     *
     * ### Y-axis
     * The y-coordinate of this position is the vertical coordinate and a positive y-value indicates that the
     * position is *above* the *origin/ground* (which should usually be the case).
     *
     * ### X-axis and Z-axis
     * The x-axis and the z-axis are consistent with the eye matrices (so if the eye matrices indicate that the player
     * is looking in the positive X-direction and the (x, z) coordinates of the `averageEyePosition` are (1, 0), the
     * *origin* is 1 meter behind the player). Other than that, the real-world directions of these axes are not
     * specified (so the positive X-direction could be north, but could just as well be east, or some direction
     * between south and west).
     *
     * ### Purposes
     * This position has 2 primary purposes:
     *  - Determine the direction and distance between the camera and rendered objects, which is required for lighting.
     *  - Check in which direction the player is walking in real life and try to move the players avatar in-game
     *  whenever the player walks in real life.
     *
     *  ## Null
     *  When the orientation and position of the player can't be tracked for some reason, this method will return null.
     */
    fun prepareRender(): Triple<Vector3f, Matrix4f, Matrix4f>?

    /**
     * This should be called right before the first `vkQueueSubmit` of each frame. This helps the VR manager with
     * getting better timing information and thus more accurate view matrices.
     */
    fun markFirstFrameQueueSubmit()

    fun submitFrames()

    fun destroy()
}
