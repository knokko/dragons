package gruviks.core

import graviks2d.target.GraviksTarget
import graviks2d.util.Color
import gruviks.component.Component
import gruviks.component.EventLogComponent
import gruviks.component.RectangularDrawnRegion
import gruviks.component.RenderResult
import gruviks.component.agent.ComponentAgent
import gruviks.component.agent.DUMMY_FEEDBACK
import gruviks.component.agent.DummyCursorTracker
import gruviks.component.fill.SimpleColorFillComponent
import gruviks.component.text.TextButton
import gruviks.component.text.TextButtonStyle
import gruviks.event.*
import gruviks.event.raw.CLICK_DURATION_THRESHOLD
import gruviks.event.raw.RawCursorMoveEvent
import gruviks.event.raw.RawCursorPressEvent
import gruviks.event.raw.RawCursorReleaseEvent
import gruviks.feedback.ExitFeedback
import gruviks.feedback.ReplaceMeFeedback
import gruviks.util.DummyGraviksTarget
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.Thread.sleep

class TestGruviksWindow {

    private val textButtonStyle = TextButtonStyle.textAndBorder(
        Color.rgbInt(4, 5, 6), Color.rgbInt(7, 8, 9)
    )

    @Test
    fun testSetRootComponent() {
        val window = GruviksWindow(SimpleColorFillComponent(Color.rgbInt(1, 2, 3)))

        var pressedButton: Int? = null

        val nextComponent = TextButton("test", null, textButtonStyle) { event, _ ->
            pressedButton = event.button
        }
        window.setRootComponent(nextComponent)

        // window.setRootComponent should initialize the component agent, so attempting to initialize it again
        // should throw an IllegalStateException
        assertThrows<IllegalStateException> { nextComponent.initAgent(ComponentAgent(DummyCursorTracker(), DUMMY_FEEDBACK)) }

        assertTrue(window.render(DummyGraviksTarget(), false))

        val cursor = Cursor(5)
        window.fireEvent(RawCursorMoveEvent(cursor, EventPosition(0.5f, 0.5f)))
        window.fireEvent(RawCursorPressEvent(cursor, 6))
        window.fireEvent(RawCursorReleaseEvent(cursor, 6))
        assertEquals(6, pressedButton)
    }

    @Test
    fun testFireEvent() {
        val component = EventLogComponent(setOf(
            CursorMoveEvent::class, CursorLeaveEvent::class, CursorClickEvent::class, CursorPressEvent::class
        ))
        val window = GruviksWindow(component)

        assertTrue(window.render(DummyGraviksTarget(), false))

        val cursor1 = Cursor(1)
        window.fireEvent(RawCursorMoveEvent(cursor1, EventPosition(0.05f, 0.2f)))

        // Since the cursor 'landed' outside the drawn region of the component, the event won't be propagated
        assertEquals(0, component.log.size)

        window.fireEvent(RawCursorMoveEvent(cursor1, EventPosition(0.15f, 0.2f)))

        // If the component were subscribed for CursorEnterEvent's, it would have received it
        // ... but it is not subscribed
        assertEquals(0, component.log.size)

        window.fireEvent(RawCursorMoveEvent(cursor1, EventPosition(0.2f, 0.21f)))

        assertEquals(1, component.log.size)
        assertEquals(CursorMoveEvent(
            cursor1, oldPosition = EventPosition(0.15f, 0.2f), newPosition = EventPosition(0.2f, 0.21f)
        ), component.log[0])

        // Firing a raw press + release event should cause a 'fine' press + release + click event
        // ... but the component is only subscribed to press and click events
        window.fireEvent(RawCursorPressEvent(cursor1, 0))
        assertEquals(2, component.log.size)
        assertEquals(CursorPressEvent(cursor1, EventPosition(0.2f, 0.21f), 0), component.log[1])
        sleep(CLICK_DURATION_THRESHOLD / 2L)
        window.fireEvent(RawCursorReleaseEvent(cursor1, 0))
        assertEquals(3, component.log.size)
        assertEquals(CursorClickEvent(cursor1, EventPosition(0.2f, 0.21f), 0), component.log[2])

        window.fireEvent(RawCursorMoveEvent(cursor1, EventPosition(0.05f, 0.23f)))

        // The cursor left the drawn region, so the component should receive a CursorLeaveEvent
        assertEquals(4, component.log.size)
        assertEquals(CursorLeaveEvent(cursor1, EventPosition(0.2f, 0.21f)), component.log[3])
    }

    @Test
    fun testRender() {
        val window = GruviksWindow(TextButton("test1234", null, textButtonStyle) { _, _ -> })

        val target = DummyGraviksTarget()

        /*
         * During each render:
         * 1 fillRect call should be made by the window
         * 1 drawRoundedRect call should be made by the component
         * 1 drawString call should be made by the component
         */
        fun assertRenderCalls(numCalls: Int) {
            assertEquals(numCalls, target.fillRectCounter)
            assertEquals(numCalls, target.drawRoundedRectCounter)
            assertEquals(0, target.drawImageCounter)
            assertEquals(numCalls, target.drawStringCounter)
        }

        // The first render call should always cause a render
        assertTrue(window.render(target, false))
        assertRenderCalls(1)

        // The second render call should only cause a render if it is forced, or if the component requested it
        assertFalse(window.render(target, false))
        assertRenderCalls(1)

        // Forcing the render should work
        assertTrue(window.render(target, true))
        assertRenderCalls(2)

        // But not during the next render call
        assertFalse(window.render(target, false))
        assertRenderCalls(2)

        // Rendering should happen if the component requests it
        window.fireEvent(RawCursorMoveEvent(Cursor(1), EventPosition(0.5f, 0.5f)))
        assertTrue(window.render(target, false))
        assertRenderCalls(3)

        // But only once after the component requested it
        assertFalse(window.render(target, false))
        assertRenderCalls(3)
    }

    @Test
    fun testExitFeedback() {
        class TestComponent : Component() {
            override fun subscribeToEvents() {
                agent.subscribe(CursorPressEvent::class)
            }

            override fun processEvent(event: Event) {
                agent.giveFeedback(ExitFeedback())
            }

            override fun render(target: GraviksTarget, force: Boolean) = RenderResult(
                drawnRegion = RectangularDrawnRegion(0f, 0f, 1f, 1f),
                propagateMissedCursorEvents = false
            )
        }

        val window = GruviksWindow(TestComponent())
        window.render(DummyGraviksTarget(), false)
        window.fireEvent(RawCursorMoveEvent(Cursor(1), EventPosition(0.4f, 0.6f)))

        assertFalse(window.shouldExit())
        window.fireEvent(RawCursorPressEvent(Cursor(1), 4))
        assertTrue(window.shouldExit())
    }

    @Test
    fun testReplaceMeFeedback() {
        var renderedSecondComponent = false

        class SecondComponent : Component() {
            override fun subscribeToEvents() {}

            override fun processEvent(event: Event) {}

            override fun render(target: GraviksTarget, force: Boolean): RenderResult {
                renderedSecondComponent = true
                return RenderResult(
                    drawnRegion = RectangularDrawnRegion(0f, 0f, 1f, 1f),
                    propagateMissedCursorEvents = true
                )
            }
        }

        class FirstComponent : Component() {
            override fun subscribeToEvents() {
                agent.subscribe(CursorPressEvent::class)
            }

            override fun processEvent(event: Event) {
                agent.giveFeedback(ReplaceMeFeedback { SecondComponent() })
            }

            override fun render(target: GraviksTarget, force: Boolean) = RenderResult(
                drawnRegion = RectangularDrawnRegion(0f, 0f, 1f, 1f),
                propagateMissedCursorEvents = false
            )
        }

        val window = GruviksWindow(FirstComponent())
        window.render(DummyGraviksTarget(), false)
        window.fireEvent(RawCursorMoveEvent(Cursor(1), EventPosition(0.4f, 0.6f)))
        window.fireEvent(RawCursorPressEvent(Cursor(1), 4))

        assertFalse(renderedSecondComponent)
        window.render(DummyGraviksTarget(), true)
        assertTrue(renderedSecondComponent)
    }
}
