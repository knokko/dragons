package dragons.plugins.standard.vulkan.render

import dragons.plugins.standard.vulkan.pipeline.BasicGraphicsPipeline
import dragons.plugins.standard.vulkan.render.tile.TileRenderer
import dragons.vulkan.memory.VulkanBufferRange
import org.joml.Matrix4f
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkClearValue
import org.lwjgl.vulkan.VkCommandBuffer
import org.lwjgl.vulkan.VkRenderPassBeginInfo
import java.nio.FloatBuffer
import java.nio.IntBuffer

class SceneRenderer(
    private val indirectDrawIntBuffer: IntBuffer,
    private val indirectDrawVulkanBuffer: VulkanBufferRange,

    private val storageFloatBuffer: FloatBuffer,
    private val storageHostBuffer: VulkanBufferRange,
    private val storageDeviceBuffer: VulkanBufferRange,

    private val cameraFloatBuffer: FloatBuffer,
    private val cameraHostBuffer: VulkanBufferRange,
    private val cameraDeviceBuffer: VulkanBufferRange
) {
    private val cameraManager: CameraBufferManager
    private val transformationMatrixManager: TransformationMatrixManager
    private val tileRenderer: TileRenderer

    init {
        this.cameraManager = CameraBufferManager(
            floatBuffer = this.cameraFloatBuffer,
            hostBuffer = this.cameraHostBuffer,
            deviceBuffer = this.cameraDeviceBuffer
        )
        this.transformationMatrixManager = TransformationMatrixManager(
            floatBuffer = this.storageFloatBuffer,
            hostBuffer = this.storageHostBuffer,
            deviceBuffer = this.storageDeviceBuffer
        )

        // TODO Once we have entities, the indirect draw buffer should be divided between tiles and entities
        val indirectDrawTileSize = this.indirectDrawIntBuffer.limit()
        this.tileRenderer = TileRenderer(
            indirectDrawIntBuffer = this.indirectDrawIntBuffer.slice(0, indirectDrawTileSize),
            indirectDrawVulkanBuffer = VulkanBufferRange(
                this.indirectDrawVulkanBuffer.buffer, 0, (indirectDrawTileSize * Int.SIZE_BYTES).toLong()
            ),
            transformationMatrixManager = this.transformationMatrixManager
        )
    }

    fun recordCommands(
        commandBuffer: VkCommandBuffer,
        basicRenderPass: Long, basicPipeline: BasicGraphicsPipeline, staticDescriptorSet: Long,
        leftFramebuffer: Long, rightFramebuffer: Long, framebufferWidth: Int, framebufferHeight: Int
    ) {
        stackPush().use { stack ->
            this.cameraManager.recordCommands(commandBuffer)
            this.transformationMatrixManager.recordCommands(commandBuffer)

            val clearValues = VkClearValue.calloc(2, stack)
            val colorClearValue = clearValues[0]
            colorClearValue.color { color ->
                color.float32(0, 68f / 255f)
                color.float32(1, 107f / 255f)
                color.float32(2, 176f / 255f)
                color.float32(3, 1f)
            }
            val depthClearValue = clearValues[1]
            depthClearValue.depthStencil { depthStencil ->
                depthStencil.depth(1f) // 1 is at the far plane
                depthStencil.stencil(0) // I don't really know what to do with this...
            }

            val biRenderPass = VkRenderPassBeginInfo.calloc(stack)
            biRenderPass.`sType$Default`()
            biRenderPass.renderPass(basicRenderPass)
            // framebuffer will be filled in later
            biRenderPass.renderArea { renderArea ->
                renderArea.offset { offset -> offset.set(0, 0) }
                renderArea.extent { extent -> extent.set(framebufferWidth, framebufferHeight) }
            }
            biRenderPass.clearValueCount(2)
            biRenderPass.pClearValues(clearValues)

            for ((eyeIndex, framebuffer) in arrayOf(leftFramebuffer, rightFramebuffer).withIndex()) {
                biRenderPass.framebuffer(framebuffer)

                vkCmdBeginRenderPass(commandBuffer, biRenderPass, VK_SUBPASS_CONTENTS_INLINE)
                vkCmdBindPipeline(
                    commandBuffer,
                    VK_PIPELINE_BIND_POINT_GRAPHICS,
                    basicPipeline.handle
                )
                vkCmdPushConstants(
                    commandBuffer,
                    basicPipeline.pipelineLayout,
                    VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT,
                    0,
                    stack.ints(eyeIndex)
                )

                this.tileRenderer.recordCommands(commandBuffer, basicPipeline, staticDescriptorSet)

                vkCmdEndRenderPass(commandBuffer)
            }
        }
    }

    fun startFrame(leftCameraMatrix: Matrix4f, rightCameraMatrix: Matrix4f) {
        this.cameraManager.setCamera(leftCameraMatrix, rightCameraMatrix)
        this.transformationMatrixManager.startFrame()
        this.tileRenderer.startFrame()
    }

    fun endFrame() {
        this.tileRenderer.endFrame()
        this.transformationMatrixManager.endFrame()
    }
}
