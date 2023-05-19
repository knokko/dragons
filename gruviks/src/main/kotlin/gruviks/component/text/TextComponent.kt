package gruviks.component.text

import graviks2d.resource.text.TextStyle
import graviks2d.target.GraviksTarget
import gruviks.component.Component
import gruviks.component.RectangularDrawnRegion
import gruviks.component.RenderResult
import gruviks.event.Event
import gruviks.feedback.RenderFeedback

class TextComponent(
    private var text: String,
    private val style: TextStyle
): Component() {
    override fun subscribeToEvents() {
        // This component doesn't respond to any events
    }

    override fun processEvent(event: Event) {
        throw UnsupportedOperationException("This component shouldn't receive any events")
    }

    fun setText(newText: String) {
        this.text = newText
        agent.giveFeedback(RenderFeedback())
    }

    override fun render(target: GraviksTarget, force: Boolean): RenderResult {
        val characterPositions = target.drawString(0f, 0f, 1f, 1f, text, style)
        if (characterPositions.isEmpty()) return RenderResult(
            drawnRegion = null, recentDrawnRegion = null, propagateMissedCursorEvents = true
        )
        val drawnRegion = RectangularDrawnRegion(
            characterPositions.minOf { it.minX }, characterPositions.minOf { it.minY },
            characterPositions.maxOf { it.maxX }, characterPositions.maxOf { it.maxY }
        )
        return RenderResult(drawnRegion = drawnRegion, propagateMissedCursorEvents = true)
    }
}
