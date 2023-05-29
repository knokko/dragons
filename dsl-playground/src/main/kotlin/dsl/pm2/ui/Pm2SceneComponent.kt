package dsl.pm2.ui

import dsl.pm2.renderer.Pm2Mesh
import graviks2d.target.GraviksTarget
import gruviks.component.Component
import gruviks.component.RectangularDrawnRegion
import gruviks.component.RenderResult
import gruviks.event.*
import gruviks.feedback.RenderFeedback
import gruviks.feedback.RequestKeyboardFocusFeedback
import org.joml.Matrix3x2f

class Pm2SceneComponent(
    private var mesh: Pm2Mesh? = null,
    private val renderScene: (Pm2Mesh, Matrix3x2f) -> Triple<Long, Long, Long>
) : Component() {

    private val cameraMatrix = Matrix3x2f().scale(1f, -1f)

    override fun subscribeToEvents() {
        // No need to handle events at this point
        agent.subscribe(KeyPressEvent::class)
        agent.subscribe(CursorClickEvent::class)
        agent.subscribe(CursorScrollEvent::class)
    }

    override fun processEvent(event: Event) {
        if (event is CursorClickEvent) agent.giveFeedback(RequestKeyboardFocusFeedback())
        if (event is KeyPressEvent) {
            if (event.key.type == KeyType.Left) cameraMatrix.translate(0.1f, 0f)
            if (event.key.type == KeyType.Right) cameraMatrix.translate(-0.1f, 0f)
            if (event.key.type == KeyType.Down) cameraMatrix.translate(0f, 0.1f)
            if (event.key.type == KeyType.Up) cameraMatrix.translate(0f, -0.1f)
            agent.giveFeedback(RenderFeedback())
        }
        if (event is CursorScrollEvent) {
            if (event.direction == ScrollDirection.Horizontal) cameraMatrix.translate(-event.amount, 0f)
            if (event.direction == ScrollDirection.Vertical) cameraMatrix.translate(0f, event.amount)
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
    }

    fun setMesh(newMesh: Pm2Mesh) {
        this.mesh = newMesh
        agent.giveFeedback(RenderFeedback())
    }

    override fun render(target: GraviksTarget, force: Boolean): RenderResult {
        if (mesh != null) {
            val (image, imageView, semaphore) = this.renderScene(this.mesh!!, cameraMatrix)
            target.addWaitSemaphore(semaphore)
            target.drawVulkanImage(0f, 0f, 1f, 1f, image, imageView)
        }

        return RenderResult(
            drawnRegion = RectangularDrawnRegion(0f, 0f, 1f, 1f),
            propagateMissedCursorEvents = false
        )
    }
}
