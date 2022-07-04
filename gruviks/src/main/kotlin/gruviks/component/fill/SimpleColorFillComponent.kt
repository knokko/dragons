package gruviks.component.fill

import graviks2d.target.GraviksTarget
import graviks2d.util.Color
import gruviks.component.Component
import gruviks.component.ComponentAgent
import gruviks.component.RectangularDrawnRegion
import gruviks.component.RenderResult
import gruviks.event.cursor.CursorClickEvent
import gruviks.event.cursor.Event

class SimpleColorFillComponent(private val color: Color): Component() {
    override fun subscribeToEvents() {
        agent.subscribe(CursorClickEvent::class)
    }

    override fun processEvent(event: Event) {
        if (event is CursorClickEvent) {
            println("Received CursorClickEvent at (${event.position}")
        } else {
            throw IllegalArgumentException("Unexpected event ${event::class}")
        }
    }

    override fun render(target: GraviksTarget, force: Boolean): RenderResult {
        target.fillRect(0f, 0f, 1f, 1f, this.color)
        return RenderResult(
            RectangularDrawnRegion(0f, 0f, 1f, 1f),
            isOpaque = true, propagateMissedCursorEvents = true
        )
    }
}
