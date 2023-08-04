package gruviks.component.fill

import graviks2d.target.GraviksTarget
import graviks2d.util.Color
import gruviks.component.Component
import gruviks.component.RectangularDrawnRegion
import gruviks.component.RenderResult
import gruviks.event.CursorClickEvent
import gruviks.event.Event
import gruviks.feedback.Feedback

class SimpleColorClickComponent(
    private val color: Color,
    val clickAction: (CursorClickEvent, (Feedback) -> Unit) -> Unit
): Component() {
    override fun subscribeToEvents() {
        agent.subscribe(CursorClickEvent::class)
    }

    override fun processEvent(event: Event) {
        if (event is CursorClickEvent) {
            clickAction(event, agent.giveFeedback)
        } else throw UnsupportedOperationException("Unexpected event $event")
    }

    override fun render(target: GraviksTarget, force: Boolean): RenderResult {
        target.fillRect(0f, 0f, 1f, 1f, color)
        return RenderResult(
            drawnRegion = RectangularDrawnRegion(0f, 0f, 1f, 1f),
            propagateMissedCursorEvents = true
        )
    }
}
