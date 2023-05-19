package dsl.pm2.ui

import dsl.pm2.renderer.Pm2Mesh
import graviks2d.target.GraviksTarget
import graviks2d.util.Color
import gruviks.component.Component
import gruviks.component.RectangularDrawnRegion
import gruviks.component.RenderResult
import gruviks.event.Event
import gruviks.feedback.RenderFeedback

class Pm2SceneComponent(
    private var mesh: Pm2Mesh? = null,
    private val renderScene: (Pm2Mesh) -> Triple<Long, Long, Long>
) : Component() {

    override fun subscribeToEvents() {
        // No need to handle events at this point
    }

    override fun processEvent(event: Event) {
        throw UnsupportedOperationException("Shouldn't receive any events")
    }

    fun setMesh(newMesh: Pm2Mesh) {
        this.mesh = newMesh
        agent.giveFeedback(RenderFeedback())
    }

    override fun render(target: GraviksTarget, force: Boolean): RenderResult {
        if (mesh != null) {
            val (image, imageView, semaphore) = this.renderScene(this.mesh!!)
            target.addWaitSemaphore(semaphore)
            target.drawVulkanImage(0f, 0f, 1f, 1f, image, imageView)
        }

        return RenderResult(
            drawnRegion = RectangularDrawnRegion(0f, 0f, 1f, 1f),
            propagateMissedCursorEvents = false
        )
    }
}
