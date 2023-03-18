package gruviks.component.test

import graviks2d.target.GraviksTarget
import graviks2d.util.Color
import gruviks.component.Component
import gruviks.component.RectangularDrawnRegion
import gruviks.component.RenderResult
import gruviks.event.Event
import gruviks.feedback.RenderFeedback
import org.joml.Math.*
import java.lang.System.currentTimeMillis

class DiscoComponent(
        private val timeOffset: Int = (360_000 * Math.random()).toInt()
): Component() {
    override fun subscribeToEvents() {
        // No subscriptions are needed
    }

    override fun processEvent(event: Event) {
        throw UnsupportedOperationException("This component doesn't subscribe to any events")
    }

    override fun render(target: GraviksTarget, force: Boolean): RenderResult {
        val time = (currentTimeMillis() + timeOffset) % 360_000
        val red = 0.5f + 0.5f * sin(time * 0.00123f)
        val green = 0.5f + 0.5f * cos(time * 0.00567f)
        val blue = 0.5f + 0.5f * sin(red * 2f * PI.toFloat())
        target.fillRect(0f, 0f, 1f, 1f, Color.rgbFloat(red, green, blue))
        agent.giveFeedback(RenderFeedback())
        return RenderResult(
                drawnRegion = RectangularDrawnRegion(0f, 0f, 1f, 1f),
                propagateMissedCursorEvents = true
        )
    }
}
