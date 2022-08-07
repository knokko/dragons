package dragons.plugins.standard.vulkan.render

import dragons.plugins.standard.vulkan.render.tile.TileRenderer
import dragons.vulkan.memory.VulkanBufferRange
import dragons.vulkan.queue.QueueFamily
import org.joml.Matrix4f
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VkDevice
import java.nio.FloatBuffer
import java.nio.IntBuffer

class SceneRenderer(
    vkDevice: VkDevice,
    queueFamily: QueueFamily,
    scenePipelines: ScenePipelines,
    sceneRenderTarget: SceneRenderTarget,

    private val indirectDrawIntBuffer: IntBuffer,
    private val indirectDrawVulkanBuffer: VulkanBufferRange,

    private val storageFloatBuffer: FloatBuffer,
    private val storageHostBuffer: VulkanBufferRange,
    private val storageDeviceBuffer: VulkanBufferRange,

    private val cameraFloatBuffer: FloatBuffer,
    private val cameraHostBuffer: VulkanBufferRange,
    private val cameraDeviceBuffer: VulkanBufferRange
) {
    private val sceneCommands: SceneCommands
    private val cameraManager: CameraBufferManager
    private val transformationMatrixManager: TransformationMatrixManager
    val tileRenderer: TileRenderer

    init {
        this.sceneCommands = SceneCommands(
            vkDevice = vkDevice,
            queueFamily = queueFamily,
            pipelines = scenePipelines,
            renderTarget = sceneRenderTarget
        )
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
                this.indirectDrawVulkanBuffer.buffer, this.indirectDrawVulkanBuffer.offset, (indirectDrawTileSize * Int.SIZE_BYTES).toLong()
            ),
            transformationMatrixManager = this.transformationMatrixManager
        )
    }

    private fun recordCommands() {
        this.sceneCommands.record(
            this.cameraManager,
            this.transformationMatrixManager,
            this.tileRenderer
        )
    }

    fun startFrame(stack: MemoryStack, leftCameraMatrix: Matrix4f, rightCameraMatrix: Matrix4f) {
        if (this.tileRenderer.shouldRecordCommandsAgain) this.recordCommands()
        this.sceneCommands.awaitLastSubmission(stack)

        this.cameraManager.setCamera(leftCameraMatrix, rightCameraMatrix)
        this.transformationMatrixManager.startFrame()
        this.tileRenderer.startFrame()
    }

    fun endFrame() {
        this.tileRenderer.endFrame()
        this.transformationMatrixManager.endFrame()
    }

    fun submit(waitSemaphores: LongArray, waitStageMasks: IntArray, signalSemaphores: LongArray) {
        this.sceneCommands.submit(waitSemaphores, waitStageMasks, signalSemaphores)
    }

    fun destroy() {
        this.sceneCommands.destroy()
    }
}
