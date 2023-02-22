package gruviks.component.text

import graviks2d.resource.text.TextStyle
import graviks2d.target.GraviksTarget
import graviks2d.util.Color
import gruviks.component.Component
import gruviks.component.RectangularDrawnRegion
import gruviks.component.RenderResult
import gruviks.event.Event

class TextComponent(
    private val text: String,
    private val style: TextStyle,
    private val background: Color
): Component() {
    override fun subscribeToEvents() {
        // This component doesn't respond to any events
    }

    override fun processEvent(event: Event) {
        throw UnsupportedOperationException("This component shouldn't receive any events")
    }

    override fun render(target: GraviksTarget, force: Boolean): RenderResult {
        target.drawString(0f, 0f, 1f, 1f, text, style, background)
        return RenderResult(
            // TODO Add more accurate drawn region, and test it
            drawnRegion = RectangularDrawnRegion(0f, 0f, 1f, 1f),
            propagateMissedCursorEvents = true
        )
    }
}
