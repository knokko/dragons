package dsl.pm2.ui

import dsl.pm2.interpreter.Pm2Model
import dsl.pm2.interpreter.Pm2RuntimeError
import dsl.pm2.interpreter.Pm2Vertex
import dsl.pm2.interpreter.value.Pm2Value
import dsl.pm2.renderer.Pm2Instance
import dsl.pm2.renderer.Pm2Scene
import graviks2d.target.GraviksTarget
import graviks2d.util.Color
import gruviks.component.Component
import gruviks.component.RectangularDrawnRegion
import gruviks.component.RenderResult
import gruviks.event.*
import gruviks.feedback.RenderFeedback
import gruviks.feedback.RequestKeyboardFocusFeedback
import org.joml.Matrix3x2f
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.util.vma.Vma.vmaDestroyImage
import org.lwjgl.vulkan.VK10.*
import troll.images.VmaImage
import troll.sync.WaitSemaphore
import kotlin.math.absoluteValue

private fun createDummyModel() = Pm2Model(listOf(
        Pm2Vertex(-0.8f, -0.7f, Color.RED, 0),
        Pm2Vertex(0.8f, -0.2f, Color.GREEN, 0),
        Pm2Vertex(-0.9f, 0.8f, Color.BLUE, 0)
    ), listOf(null), emptyMap())

class Pm2PreviewComponent(
    private val pm2Instance: Pm2Instance,
    initialModel: Pm2Model?, width: Int, height: Int,
    private val reportError: (String) -> Unit
): Component() {

    private var lastDynamicParameterValues: MutableMap<String, Pm2Value>? = null
    val dynamicParameterValues = mutableMapOf<String, Pm2Value>()

    private val cameraMatrix = Matrix3x2f().scale(1f, -1f)

    private var currentMesh = pm2Instance.allocations.allocateMesh(initialModel ?: createDummyModel())
    private var oldSceneLayout = VK_IMAGE_LAYOUT_UNDEFINED
    private var sceneAccessMask = 0
    private var sceneStageMask = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT

    private val scene: Pm2Scene
    private val sceneImage: VmaImage
    private val sceneSemaphore: Long

    init {
        scene = Pm2Scene(
            pm2Instance.troll,
            pm2Instance.descriptorSetLayout,
            20, 200, 250,
            width, height
        )
        stackPush().use { stack ->
            sceneImage = pm2Instance.troll.images.createSimple(
                stack, width, height, VK_FORMAT_R8G8B8A8_SRGB,
                VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT,
                VK_IMAGE_ASPECT_COLOR_BIT, "Pm2PreviewImage"
            )
            sceneSemaphore = pm2Instance.troll.sync.createSemaphores("Pm2PreviewSemaphore", 1)[0]
        }
    }

    fun getDynamicParameterTypes() = currentMesh.dynamicParameterTypes

    fun updateModel(newModel: Pm2Model) {
        val newMesh = pm2Instance.allocations.allocateMesh(newModel)
        scene.awaitLastDraw()
        pm2Instance.allocations.destroyMesh(currentMesh)
        currentMesh = newMesh
        agent.giveFeedback(RenderFeedback())
    }

    override fun subscribeToEvents() {
        agent.subscribe(RemoveEvent::class)
        agent.subscribe(KeyPressEvent::class)
        agent.subscribe(CursorClickEvent::class)
        agent.subscribe(CursorScrollEvent::class)
        agent.subscribe(UpdateEvent::class)
    }

    override fun processEvent(event: Event) {
        if (event is CursorClickEvent) agent.giveFeedback(RequestKeyboardFocusFeedback())
        if (event is UpdateEvent) {
            if (dynamicParameterValues != lastDynamicParameterValues) {
                lastDynamicParameterValues = HashMap(dynamicParameterValues)
                agent.giveFeedback(RenderFeedback())
            }
        }
        if (event is KeyPressEvent) {
            if (event.key.type == KeyType.Left) cameraMatrix.translate(0.1f, 0f)
            if (event.key.type == KeyType.Right) cameraMatrix.translate(-0.1f, 0f)
            if (event.key.type == KeyType.Down) cameraMatrix.translate(0f, 0.1f)
            if (event.key.type == KeyType.Up) cameraMatrix.translate(0f, -0.1f)
            agent.giveFeedback(RenderFeedback())
        }
        if (event is CursorScrollEvent) {
            if (event.direction == ScrollDirection.Horizontal) cameraMatrix.translateLocal(-event.amount, 0f)
            if (event.direction == ScrollDirection.Vertical) cameraMatrix.translateLocal(0f, -event.amount)
            if (event.direction == ScrollDirection.Zoom) {
                val scale = if (event.amount >= 0f) 1f / (1f + event.amount) else 1f - event.amount
                val cursorState = agent.cursorTracker.getCursorState(event.cursor)

                if (cursorState != null) {
                    cameraMatrix.scaleAroundLocal(
                        scale,
                        2f * cursorState.localPosition.x - 1f,
                        -2f * cursorState.localPosition.y + 1f
                    )
                }
            }
            agent.giveFeedback(RenderFeedback())
        }
        if (event is RemoveEvent) {
            scene.destroy()
            pm2Instance.allocations.destroyMesh(currentMesh)
            vkDestroyImageView(pm2Instance.troll.vkDevice(), sceneImage.vkImageView, null)
            vmaDestroyImage(pm2Instance.troll.vmaAllocator(), sceneImage.vkImage, sceneImage.vmaAllocation)
            vkDestroySemaphore(pm2Instance.troll.vkDevice(), sceneSemaphore, null)
        }
    }

    override fun render(target: GraviksTarget, force: Boolean): RenderResult {
        val aspectRatio = target.getAspectRatio()
        val cameraRatio = cameraMatrix.m11 / -cameraMatrix.m00

        if ((aspectRatio - cameraRatio).absoluteValue > 0.001f) {
            cameraMatrix.scale(cameraRatio / aspectRatio, 1f)
        }

        try {
            scene.drawAndCopy(
                pm2Instance, listOf(Pair(currentMesh, dynamicParameterValues)), cameraMatrix, sceneSemaphore,
                destImage = sceneImage.vkImage, oldLayout = oldSceneLayout,
                srcAccessMask = sceneAccessMask, srcStageMask = sceneStageMask,
                newLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, dstAccessMask = VK_ACCESS_SHADER_READ_BIT,
                dstStageMask = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                offsetX = 0, offsetY = 0, blitSizeX = sceneImage.width, blitSizeY = sceneImage.height
            )
            oldSceneLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL
            sceneAccessMask = VK_ACCESS_SHADER_READ_BIT
            sceneStageMask = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT

            target.addWaitSemaphore(WaitSemaphore(sceneSemaphore, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT))
            target.drawVulkanImage(0f, 0f, 1f, 1f, sceneImage.vkImage, sceneImage.vkImageView)
        } catch (runtimeError: Pm2RuntimeError) {
            reportError(runtimeError.message!!)
        }

        return RenderResult(
            drawnRegion = RectangularDrawnRegion(0f, 0f, 1f, 1f),
            propagateMissedCursorEvents = false
        )
    }
}
