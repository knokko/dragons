package gruviks.component.test

import graviks2d.target.GraviksTarget
import graviks2d.util.Color
import gruviks.component.Component
import gruviks.component.RectangularDrawnRegion
import gruviks.component.RenderResult
import gruviks.event.*
import java.util.*

private fun generateColor(): Color {
    val rng = Random()
    return Color.rgbInt(rng.nextInt(255), rng.nextInt(255), rng.nextInt(255))
}

class ColorShuffleComponent : Component() {

    private var currentColor = generateColor()

    override fun subscribeToEvents() {
        agent.subscribe(CursorClickEvent::class)
        agent.subscribe(CursorPressEvent::class)
        agent.subscribe(CursorReleaseEvent::class)
        agent.subscribe(CursorEnterEvent::class)
        agent.subscribe(CursorLeaveEvent::class)
    }

    override fun processEvent(event: Event) {
        if (event is CursorClickEvent) {
            if (event.button == 0) {
                currentColor = generateColor()
                agent.didRequestRender = true
            }
        } else if (event is CursorPressEvent) {
            if (event.button != 0) {
                currentColor = Color.rgbInt(200, 0, 0)
                agent.didRequestRender = true
            }
        } else if (event is CursorReleaseEvent) {
            if (event.button != 0) {
                currentColor = Color.rgbInt(0, 200, 0)
                agent.didRequestRender = true
            }
        } else if (event is CursorEnterEvent) {
            currentColor = Color.rgbInt(0, 0, 200)
            agent.didRequestRender = true
        } else if (event is CursorLeaveEvent) {
            currentColor = Color.rgbInt(255, 255, 255)
            agent.didRequestRender = true
        } else {
            throw IllegalArgumentException("Unexpected event ${event::class.java}")
        }
    }

    override fun render(target: GraviksTarget, force: Boolean): RenderResult {
        target.fillRect(0f, 0f, 0.8f, 1f, currentColor)
        return RenderResult(
            RectangularDrawnRegion(0f, 0f, 0.8f, 1f),
            isOpaque = true, propagateMissedCursorEvents = false
        )
    }
}
