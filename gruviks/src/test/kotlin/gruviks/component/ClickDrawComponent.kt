package gruviks.component

import graviks2d.target.GraviksTarget
import graviks2d.util.Color
import gruviks.event.CursorClickEvent
import gruviks.event.Event

class ClickDrawComponent: Component() {
    override fun subscribeToEvents() {
        agent.subscribe(CursorClickEvent::class)
    }

    override fun processEvent(event: Event) {
        agent.didRequestRender = true
    }

    override fun render(target: GraviksTarget, force: Boolean): RenderResult {
        target.fillRect(0.1f, 0.1f, 0.9f, 0.9f, Color.RED)
        return RenderResult(
            drawnRegion = RectangularDrawnRegion(0.1f, 0.1f, 0.9f, 0.9f),
            propagateMissedCursorEvents = true
        )
    }

    override fun regionsToRedrawBeforeNextRender() = listOf(BackgroundRegion(0.1f, 0.1f, 0.9f, 0.9f))
}
