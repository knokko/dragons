package dragons.plugins.standard.vulkan.render

import dragons.vulkan.memory.VulkanBufferRange
import org.joml.Matrix4f
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import java.nio.FloatBuffer

internal class TransformationMatrixManager(
    private val floatBuffer: FloatBuffer,
    private val hostBuffer: VulkanBufferRange,
    private val deviceBuffer: VulkanBufferRange
) {

    private var currentMatrixIndex = 0
    private var isWithinFrame = false

    init {
        if (this.hostBuffer.size != this.floatBuffer.limit().toLong() * Float.SIZE_BYTES || this.hostBuffer.size != this.deviceBuffer.size) {
            throw IllegalArgumentException("All buffer sizes must be equal")
        }
    }

    fun recordCommands(commandBuffer: VkCommandBuffer) {
        // TODO Don't copy (many) more matrices than are used (although this is complicated if we don't record every frame)
        recordBufferCopyAndMemoryBarriers(
            commandBuffer, this.hostBuffer, this.deviceBuffer,
            VK_ACCESS_HOST_WRITE_BIT, VK_PIPELINE_STAGE_HOST_BIT, VK_ACCESS_SHADER_READ_BIT,
            VK_PIPELINE_STAGE_TESSELLATION_EVALUATION_SHADER_BIT or VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
        )
    }

    fun startFrame() {
        if (this.isWithinFrame) throw IllegalStateException("Frame already started")

        this.currentMatrixIndex = 0
        this.isWithinFrame = true
    }

    fun endFrame() {
        if (!this.isWithinFrame) throw IllegalStateException("No frame is in progress")

        this.isWithinFrame = false
    }

    fun prepareMatrices(matrices: Array<Matrix4f>): Int {
        if (!this.isWithinFrame) throw IllegalStateException("No frame is in progress")

        // TODO Do this atomically
        val startMatrixIndex = this.currentMatrixIndex
        this.currentMatrixIndex += matrices.size

        for ((arrayMatrixIndex, matrix) in matrices.withIndex()) {
            val floatIndex = (startMatrixIndex + arrayMatrixIndex) * 16
            matrix.get(floatIndex, this.floatBuffer)
        }

        return startMatrixIndex
    }
}
