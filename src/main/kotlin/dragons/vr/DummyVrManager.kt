package dragons.vr

import dragons.state.StaticGraphicsState
import org.joml.Math.toRadians
import org.joml.Matrix4f
import org.lwjgl.vulkan.VkPhysicalDevice
import java.lang.System.currentTimeMillis
import java.lang.Thread.sleep

class DummyVrManager(
    val instanceExtensions: Set<String> = setOf(),
    val deviceExtensions: Set<String> = setOf()
): VrManager {
    override fun getVulkanInstanceExtensions(availableExtensions: Set<String>): Set<String> {
        return instanceExtensions
    }

    override fun getVulkanDeviceExtensions(
        device: VkPhysicalDevice, deviceName: String, availableExtensions: Set<String>
    ): Set<String> {
        return deviceExtensions
    }

    // These values are pretty arbitrary, but some decision had to be made
    override fun getWidth() = 1600
    override fun getHeight() = 900

    override fun setGraphicsState(graphicsState: StaticGraphicsState) {
        // The dummy VR manager doesn't need the graphics state
    }

    private var lastRenderTime: Long? = null

    override fun prepareRender(): Pair<Matrix4f, Matrix4f> {
        if (lastRenderTime != null) {
            // This should cause a framerate of ~90 fps
            val nextRenderTime = lastRenderTime!! + 1000 / 90
            val currentTime = currentTimeMillis()
            if (currentTime < nextRenderTime) {
                sleep(nextRenderTime - currentTime)
            }
        }

        val projectionMatrix = Matrix4f().scale(1f, -1f, 1f).perspective(
            toRadians(70f),
            getWidth().toFloat() / getHeight().toFloat(),
            0.01f, 1000f, true
        )

        // Let the camera rotate slowly
        val viewMatrix = Matrix4f()
            .rotateXYZ(toRadians(20f), toRadians(((currentTimeMillis() / 10) % 360).toFloat()), 0f)
            .translate(0f, 10f, 0f)
        val combinedMatrix = projectionMatrix.mul(viewMatrix)

        lastRenderTime = currentTimeMillis()
        return Pair(Matrix4f(combinedMatrix), Matrix4f(combinedMatrix))
    }

    override fun submitFrames() {
        // We don't really need to do anything here
    }

    override fun destroy() {}
}
