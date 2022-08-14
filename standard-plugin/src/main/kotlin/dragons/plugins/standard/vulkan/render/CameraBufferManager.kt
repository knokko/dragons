package dragons.plugins.standard.vulkan.render

import dragons.vulkan.memory.VulkanBufferRange
import org.joml.Matrix4f
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import java.nio.FloatBuffer

internal class CameraBufferManager(
    private val floatBuffer: FloatBuffer,
    private val hostBuffer: VulkanBufferRange,
    private val deviceBuffer: VulkanBufferRange
) {

    init {
        if (this.floatBuffer.limit().toLong() * Float.SIZE_BYTES != this.hostBuffer.size || this.hostBuffer.size != this.deviceBuffer.size) {
            throw IllegalArgumentException("The size of all 3 camera buffers must be equal")
        }
        if (this.floatBuffer.limit() != 32) throw IllegalArgumentException("The size of the camera buffers should be 32 floats")
    }

    fun recordCommands(commandBuffer: VkCommandBuffer) {
        recordBufferCopyAndMemoryBarriers(
            commandBuffer, this.hostBuffer, this.deviceBuffer,
            VK_ACCESS_HOST_WRITE_BIT, VK_PIPELINE_STAGE_HOST_BIT, VK_ACCESS_SHADER_READ_BIT,
            VK_PIPELINE_STAGE_TESSELLATION_EVALUATION_SHADER_BIT or VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
        )
    }

    fun setCamera(leftEyeMatrix: Matrix4f, rightEyeMatrix: Matrix4f) {
        leftEyeMatrix.get(0, this.floatBuffer)
        rightEyeMatrix.get(16, this.floatBuffer)
    }
}
