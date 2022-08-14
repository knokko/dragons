package dragons.plugins.standard.vulkan.render

import dragons.plugins.standard.state.StandardPluginState
import dragons.plugins.standard.vulkan.render.chunk.ChunkRenderManager
import dragons.plugins.standard.vulkan.render.tile.StandardTileRenderer
import dragons.state.StaticGameState
import dragons.vulkan.memory.VulkanBuffer
import dragons.vulkan.memory.VulkanBufferRange
import dragons.world.realm.Realm
import dragons.world.render.SceneRenderTarget
import dragons.world.render.SceneRenderer
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VkDevice

class StandardSceneRenderer internal constructor(
    gameState: StaticGameState,
    pluginState: StandardPluginState,
): SceneRenderer {
    private val indirectDrawIntBuffer = pluginState.graphics.buffers.indirectDrawHost.asIntBuffer()
    private val indirectDrawVulkanBuffer = pluginState.graphics.buffers.indirectDrawDevice

    private val storageFloatBuffer = pluginState.graphics.buffers.transformationMatrixHost.asFloatBuffer()
    private val storageHostBuffer = pluginState.graphics.buffers.transformationMatrixStaging
    private val storageDeviceBuffer = pluginState.graphics.buffers.transformationMatrixDevice

    private val cameraFloatBuffer = pluginState.graphics.buffers.cameraHost.asFloatBuffer()
    private val cameraHostBuffer = pluginState.graphics.buffers.cameraStaging
    private val cameraDeviceBuffer = pluginState.graphics.buffers.cameraDevice

    private val sceneCommands: SceneCommands
    private val cameraManager: CameraBufferManager
    private val transformationMatrixManager: TransformationMatrixManager
    private val tileRenderer: StandardTileRenderer
    private val chunkRenderManager: ChunkRenderManager

    init {
        this.sceneCommands = SceneCommands(
            vkDevice = gameState.graphics.vkDevice,
            queueFamily = gameState.graphics.queueManager.generalQueueFamily,
            basicRenderPass = pluginState.graphics.basicRenderPass,
            basicPipeline = pluginState.graphics.basicGraphicsPipeline,
            staticDescriptorSet = pluginState.graphics.basicStaticDescriptorSet,
            renderTarget = SceneRenderTarget(
                leftFramebuffer = pluginState.graphics.basicLeftFramebuffer,
                rightFramebuffer = pluginState.graphics.basicRightFramebuffer,
                width = gameState.vrManager.getWidth(),
                height = gameState.vrManager.getHeight()
            )
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
        this.tileRenderer = StandardTileRenderer(
            indirectDrawIntBuffer = this.indirectDrawIntBuffer.slice(0, indirectDrawTileSize),
            indirectDrawVulkanBuffer = VulkanBufferRange(
                this.indirectDrawVulkanBuffer.buffer, this.indirectDrawVulkanBuffer.offset, (indirectDrawTileSize * Int.SIZE_BYTES).toLong()
            ),
            transformationMatrixManager = this.transformationMatrixManager
        )
        this.chunkRenderManager = ChunkRenderManager(gameState, pluginState, this.tileRenderer)
    }

    private fun recordCommands() {
        this.sceneCommands.record(
            this.cameraManager,
            this.transformationMatrixManager,
            this.tileRenderer
        )
    }

    override fun render(realm: Realm, averageEyePosition: Vector3f, leftCameraMatrix: Matrix4f, rightCameraMatrix: Matrix4f) {
        stackPush().use { stack ->
            this.startFrame(stack, leftCameraMatrix, rightCameraMatrix)

            // TODO Render big tiles and entities as well
            this.chunkRenderManager.renderChunks(this, realm, averageEyePosition)

            this.endFrame()
        }
    }

    fun startFrame(stack: MemoryStack, leftCameraMatrix: Matrix4f, rightCameraMatrix: Matrix4f) {
        if (this.tileRenderer.shouldRecordCommandsAgain) this.recordCommands()
        this.sceneCommands.awaitLastSubmission(stack)

        this.cameraManager.setCamera(leftCameraMatrix, rightCameraMatrix)
        this.transformationMatrixManager.startFrame()
        this.tileRenderer.startFrame()
    }

    fun addChunk(
        vertexBuffer: VulkanBuffer,
        indexBuffer: VulkanBuffer,
        dynamicDescriptorSet: Long,
        maxNumIndirectDrawCalls: Int
    ) {
        this.tileRenderer.addChunk(vertexBuffer, indexBuffer, dynamicDescriptorSet, maxNumIndirectDrawCalls)
    }

    fun drawTile(
        vertices: VulkanBufferRange,
        indices: VulkanBufferRange,
        transformationMatrices: Array<Matrix4f>
    ) {
        this.tileRenderer.drawTile(vertices, indices, transformationMatrices)
    }

    fun endFrame() {
        this.tileRenderer.endFrame()
        this.transformationMatrixManager.endFrame()
    }

    override fun submit(waitSemaphores: LongArray, waitStageMasks: IntArray, signalSemaphores: LongArray) {
        this.sceneCommands.submit(waitSemaphores, waitStageMasks, signalSemaphores)
    }

    fun destroy(vkDevice: VkDevice) {
        this.sceneCommands.destroy()
        this.chunkRenderManager.destroy(vkDevice = vkDevice)
    }
}
