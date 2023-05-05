package dsl.pm2.interpreter

import graviks2d.target.GraviksTarget
import graviks2d.util.Color
import gruviks.component.Component
import gruviks.component.RectangularDrawnRegion
import gruviks.component.RenderResult
import gruviks.event.Event
import gruviks.feedback.RenderFeedback

class Pm2SceneComponent(
    private var scene: Pm2Scene
) : Component() {
    override fun subscribeToEvents() {
        // No need to handle events at this point
    }

    override fun processEvent(event: Event) {
        throw UnsupportedOperationException("Shouldn't receive any events")
    }

    fun setScene(newScene: Pm2Scene) {
        this.scene = newScene
        agent.giveFeedback(RenderFeedback())
    }

    override fun render(target: GraviksTarget, force: Boolean): RenderResult {
        target.fillRect(0f, 0f, 1f, 1f, Color.BLACK)

        val radius = 0.002f
        for (vertex in scene.vertices) {
            target.fillRect(vertex.x - radius, vertex.y - radius, vertex.x + radius, vertex.y + radius, Color.RED)
        }

        return RenderResult(
            drawnRegion = RectangularDrawnRegion(0f, 0f, 1f, 1f),
            propagateMissedCursorEvents = false
        )
    }
}
