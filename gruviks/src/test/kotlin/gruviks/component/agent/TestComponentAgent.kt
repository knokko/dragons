package gruviks.component.agent

import gruviks.event.CursorClickEvent
import gruviks.event.CursorMoveEvent
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.IllegalStateException

class TestComponentAgent {

    @Test
    fun testSubscribe() {
        val agent = ComponentAgent(DummyCursorTracker(), DUMMY_FEEDBACK, NO_KEYBOARD_FOCUS)
        assertFalse(agent.isSubscribed(CursorClickEvent::class))

        agent.subscribe(CursorClickEvent::class)
        assertTrue(agent.isSubscribed(CursorClickEvent::class))
        assertFalse(agent.isSubscribed(CursorMoveEvent::class))

        agent.forbidFutureSubscriptions()
        assertTrue(agent.isSubscribed(CursorClickEvent::class))
        assertFalse(agent.isSubscribed(CursorMoveEvent::class))
        assertThrows<IllegalStateException> { agent.subscribe(CursorMoveEvent::class) }
        assertFalse(agent.isSubscribed(CursorMoveEvent::class))
    }

    @Test
    fun testSubscribeAll() {
        val agent = ComponentAgent(DummyCursorTracker(), DUMMY_FEEDBACK, NO_KEYBOARD_FOCUS)
        assertFalse(agent.isSubscribed(CursorClickEvent::class))

        agent.subscribeToAllEvents()
        assertTrue(agent.isSubscribed(CursorClickEvent::class))

        agent.forbidFutureSubscriptions()
        assertTrue(agent.isSubscribed(CursorMoveEvent::class))
        assertThrows<IllegalStateException> { agent.subscribeToAllEvents() }
    }
}
