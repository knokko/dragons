package gruviks.component.fill

import graviks2d.target.GraviksTarget
import graviks2d.util.Color
import gruviks.component.Component
import gruviks.component.RectangularDrawnRegion
import gruviks.component.RenderResult
import gruviks.event.Event

class SimpleColorFillComponent(private val color: Color): Component() {
    override fun subscribeToEvents() {}

    override fun processEvent(event: Event) {
        throw IllegalArgumentException("Unexpected event ${event::class.java}")
    }

    override fun render(target: GraviksTarget, force: Boolean): RenderResult {
        target.fillRect(0f, 0f, 1f, 1f, this.color)
        return RenderResult(
            RectangularDrawnRegion(0f, 0f, 1f, 1f), propagateMissedCursorEvents = true
        )
    }
}
