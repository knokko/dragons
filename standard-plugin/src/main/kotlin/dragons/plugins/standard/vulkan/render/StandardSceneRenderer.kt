package dragons.plugins.standard.vulkan.render

import dragons.plugins.standard.state.StandardPluginState
import dragons.plugins.standard.vulkan.render.chunk.ChunkRenderManager
import dragons.plugins.standard.vulkan.render.entity.EntityMesh
import dragons.plugins.standard.vulkan.render.entity.EntityRenderManager
import dragons.plugins.standard.vulkan.render.entity.StandardEntityRenderer
import dragons.plugins.standard.vulkan.render.tile.StandardTileRenderer
import dragons.geometry.Position
import dragons.state.StaticGameState
import dragons.vulkan.memory.VulkanBufferRange
import dragons.world.realm.Realm
import dragons.world.render.SceneRenderTarget
import dragons.world.render.SceneRenderer
import org.joml.Matrix4f
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VkDevice
import troll.sync.WaitSemaphore

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
    private val entityRenderer: StandardEntityRenderer
    private val chunkRenderManager: ChunkRenderManager
    private val entityRenderManager: EntityRenderManager

    init {
        this.sceneCommands = SceneCommands(
            vkDevice = gameState.graphics.troll.vkDevice(),
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

        val indirectDrawTileSize = this.indirectDrawIntBuffer.limit() * 3 / 4
        val indirectDrawEntitySize = this.indirectDrawIntBuffer.limit() / 4
        this.tileRenderer = StandardTileRenderer(
            indirectDrawIntBuffer = this.indirectDrawIntBuffer.slice(0, indirectDrawTileSize),
            indirectDrawVulkanBuffer = VulkanBufferRange(
                this.indirectDrawVulkanBuffer.buffer, this.indirectDrawVulkanBuffer.offset, (indirectDrawTileSize * Int.SIZE_BYTES).toLong()
            ),
            transformationMatrixManager = this.transformationMatrixManager
        )
        this.entityRenderer = StandardEntityRenderer(
            graphicsState = gameState.graphics,
            basicDynamicDescriptorSetLayout = pluginState.graphics.basicGraphicsPipeline.dynamicDescriptorSetLayout,
            meshStagingBuffer = pluginState.graphics.buffers.entityMeshStaging,
            meshDeviceBuffer = pluginState.graphics.buffers.entityMeshDevice,
            meshHostBuffer = pluginState.graphics.buffers.entityMeshHost,
            indirectDrawIntBuffer = this.indirectDrawIntBuffer.slice(indirectDrawTileSize, indirectDrawEntitySize),
            indirectDrawVulkanBuffer = VulkanBufferRange(
                this.indirectDrawVulkanBuffer.buffer,
                this.indirectDrawVulkanBuffer.offset + indirectDrawTileSize * Int.SIZE_BYTES,
                (indirectDrawEntitySize * Int.SIZE_BYTES).toLong()
            ),
            transformationMatrixManager = transformationMatrixManager,
            maxNumPixels = 500_000_000L // TODO Find a better way to define this bound
        )
        this.chunkRenderManager = ChunkRenderManager(gameState, pluginState, this.tileRenderer)
        this.entityRenderManager = EntityRenderManager(gameState.pluginManager)
    }

    private fun recordCommands() {
        this.sceneCommands.record(
            this.cameraManager,
            this.transformationMatrixManager,
            this.tileRenderer,
            this.entityRenderer
        )
    }

    override fun render(realm: Realm, averageEyePosition: Position, leftCameraMatrix: Matrix4f, rightCameraMatrix: Matrix4f) {
        stackPush().use { stack ->

            // The chunks must be chosen and loaded before starting the frame
            this.chunkRenderManager.chooseAndLoadChunks(realm, averageEyePosition)

            this.startFrame(stack, leftCameraMatrix, rightCameraMatrix)

            // TODO Render big tiles as well
            this.chunkRenderManager.renderChunks(this, realm, averageEyePosition)
            this.entityRenderManager.renderEntities(this, realm, averageEyePosition)

            this.endFrame()
        }
    }

    private fun startFrame(stack: MemoryStack, leftCameraMatrix: Matrix4f, rightCameraMatrix: Matrix4f) {
        if (this.tileRenderer.shouldRecordCommandsAgain) this.recordCommands()
        this.sceneCommands.awaitLastSubmission(stack)

        this.cameraManager.setCamera(leftCameraMatrix, rightCameraMatrix)
        this.transformationMatrixManager.startFrame()
        this.tileRenderer.startFrame()
        this.entityRenderer.startFrame()
    }

    fun drawTile(
        vertices: VulkanBufferRange,
        indices: VulkanBufferRange,
        transformationMatrices: Array<Matrix4f>
    ) {
        this.tileRenderer.drawTile(vertices, indices, transformationMatrices)
    }

    fun drawEntity(
        mesh: EntityMesh,
        transformationMatrices: Array<Matrix4f>
    ) {
        this.entityRenderer.drawMesh(mesh, transformationMatrices)
    }

    private fun endFrame() {
        this.entityRenderer.endFrame()
        this.tileRenderer.endFrame()

        // The entity renderer only knows after endFrame() whether it needs to record the commands again
        if (this.entityRenderer.shouldRecordCommandsAgain || this.sceneCommands.shouldRecordCommandsAgain) this.recordCommands()

        this.transformationMatrixManager.endFrame()
    }

    override fun submit(realm: Realm, waitSemaphores: Array<WaitSemaphore>, signalSemaphores: LongArray) {
        val realmSemaphores = this.chunkRenderManager.getWaitSemaphores(realm) + this.entityRenderManager.getWaitSemaphores(realm)

        val combinedSemaphores = waitSemaphores + realmSemaphores
        this.sceneCommands.submit(combinedSemaphores, signalSemaphores)
    }

    fun destroy(vkDevice: VkDevice) {
        this.sceneCommands.destroy()
        this.chunkRenderManager.destroy(vkDevice = vkDevice)
        this.entityRenderManager.destroy(vkDevice = vkDevice)
        this.entityRenderer.destroy()
    }
}
