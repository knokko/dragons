package gruviks.event

import gruviks.component.EventLogComponent
import gruviks.component.agent.ComponentAgent
import gruviks.component.agent.DummyCursorTracker
import gruviks.util.DummyGraviksTarget
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class TestPropagation {

    private fun createLogComponent(vararg events: KClass<out Event>): Pair<EventLogComponent, ComponentAgent> {
        val component = EventLogComponent(events.toSet())
        val agent = ComponentAgent(DummyCursorTracker())
        component.initAgent(agent)
        component.subscribeToEvents()
        return Pair(component, agent)
    }

    @Test
    fun testPropagateClick() {
        val (interestedComponent, interestedAgent) = createLogComponent(CursorClickEvent::class)
        val (boringComponent, boringAgent) = createLogComponent(CursorEnterEvent::class, CursorPressEvent::class)

        val middleClick = CursorClickEvent(Cursor(1), EventPosition(0.5f, 0.5f), 1)

        // Before rendering, the drawn region should be null, so no events should be received
        propagateEvent(middleClick, interestedComponent, interestedAgent)
        assertEquals(0, interestedComponent.log.size)

        // So let's render the components
        interestedAgent.lastRenderResult = interestedComponent.render(DummyGraviksTarget(), false)
        boringAgent.lastRenderResult = boringComponent.render(DummyGraviksTarget(), false)

        // This time, the event should be propagated normally
        propagateEvent(middleClick, interestedComponent, interestedAgent)
        assertEquals(1, interestedComponent.log.size)
        assertEquals(middleClick, interestedComponent.log[0])

        // Since the boring component is not subscribed to CursorClickEvent, it should not be propagated
        propagateEvent(middleClick, boringComponent, boringAgent)
        assertEquals(0, boringComponent.log.size)

        // Miss-clicks should not be propagated to either of the components
        val missClick = CursorClickEvent(Cursor(1), EventPosition(0.05f, 0.05f), 0)
        propagateEvent(missClick, interestedComponent, interestedAgent)
        propagateEvent(missClick, boringComponent, boringAgent)
        assertEquals(1, interestedComponent.log.size)
        assertEquals(0, boringComponent.log.size)
    }

    @Test
    fun testPropagateCursorMove() {
        val (fullComponent, fullAgent) = createLogComponent(CursorEnterEvent::class, CursorMoveEvent::class, CursorLeaveEvent::class)
        val (enterOnlyComponent, enterOnlyAgent) = createLogComponent(CursorEnterEvent::class)
        val (moveOnlyComponent, moveOnlyAgent) = createLogComponent(CursorMoveEvent::class)
        val (boringComponent, boringAgent) = createLogComponent(CursorClickEvent::class)

        fun propagateAll(event: Event) {
            propagateEvent(event, fullComponent, fullAgent)
            propagateEvent(event, enterOnlyComponent, enterOnlyAgent)
            propagateEvent(event, moveOnlyComponent, moveOnlyAgent)
            propagateEvent(event, boringComponent, boringAgent)
        }

        val allComponents = arrayOf(fullComponent, enterOnlyComponent, moveOnlyComponent, boringComponent)

        val ignoredEvent = CursorMoveEvent(Cursor(0), EventPosition(0.01f, 0.01f), EventPosition(0.02f, 0.02f))
        val enteringEvent = CursorMoveEvent(Cursor(1), EventPosition(0.01f, 0.01f), EventPosition(0.2f, 0.3f))
        val moveEvent = CursorMoveEvent(Cursor(1), EventPosition(0.2f, 0.3f), EventPosition(0.5f, 0.3f))
        val leavingEvent = CursorMoveEvent(Cursor(1), EventPosition(0.5f, 0.3f), EventPosition(0.99f, 0.3f))

        val receivedEnterEvent = CursorEnterEvent(Cursor(1), EventPosition(0.2f, 0.3f))
        val receivedLeaveEvent = CursorLeaveEvent(Cursor(1), EventPosition(0.5f, 0.3f))

        // Before rendering, all events should be ignored
        propagateAll(enteringEvent)
        for (component in allComponents) {
            assertEquals(0, component.log.size)
        }

        // Render all components
        fullAgent.lastRenderResult = fullComponent.render(DummyGraviksTarget(), false)
        enterOnlyAgent.lastRenderResult = enterOnlyComponent.render(DummyGraviksTarget(), false)
        moveOnlyAgent.lastRenderResult = moveOnlyComponent.render(DummyGraviksTarget(), false)
        boringAgent.lastRenderResult = boringComponent.render(DummyGraviksTarget(), false)

        // The ignored event should not have any effect
        propagateAll(ignoredEvent)
        for (component in allComponents) {
            assertEquals(0, component.log.size)
        }

        // The enter event should be received by the full component and the enter component
        propagateAll(enteringEvent)
        assertEquals(1, fullComponent.log.size)
        assertEquals(receivedEnterEvent, fullComponent.log[0])
        assertEquals(1, enterOnlyComponent.log.size)
        assertEquals(receivedEnterEvent, enterOnlyComponent.log[0])
        assertEquals(0, moveOnlyComponent.log.size)
        assertEquals(0, boringComponent.log.size)

        // The move event should be received by the full component and the move component
        propagateAll(moveEvent)
        assertEquals(2, fullComponent.log.size)
        assertEquals(moveEvent, fullComponent.log[1])
        assertEquals(1, enterOnlyComponent.log.size)
        assertEquals(1, moveOnlyComponent.log.size)
        assertEquals(moveEvent, moveOnlyComponent.log[0])
        assertEquals(0, boringComponent.log.size)

        // The leave event should only be received by the full component
        propagateAll(leavingEvent)
        assertEquals(3, fullComponent.log.size)
        assertEquals(receivedLeaveEvent, fullComponent.log[2])
        assertEquals(1, enterOnlyComponent.log.size)
        assertEquals(1, moveOnlyComponent.log.size)
        assertEquals(0, boringComponent.log.size)
    }
}
