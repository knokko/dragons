package gruviks.component.agent

import gruviks.component.RectangularDrawnRegion
import gruviks.component.RenderResult
import gruviks.event.Cursor
import gruviks.event.EventPosition
import gruviks.event.raw.RawCursorMoveEvent
import gruviks.event.raw.RawCursorPressEvent
import gruviks.event.raw.RawEventAdapter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TestRootCursorTracker {

    private val tracker: RootCursorTracker
    private val cursor0 = Cursor(0)
    private val cursor1: Cursor
    private val cursor2: Cursor

    init {
        val events = RawEventAdapter()
        tracker = RootCursorTracker(events) {
            RenderResult(
                RectangularDrawnRegion(0.1f, 0f, 0.9f, 1f), propagateMissedCursorEvents = true
            )
        }

        cursor1 = Cursor(1)
        cursor2 = Cursor(2)
        events.convertRawEvent(RawCursorMoveEvent(cursor1, EventPosition(0.05f, 0.2f)))
        events.convertRawEvent(RawCursorMoveEvent(cursor2, EventPosition(0.5f, 0.7f)))
        events.convertRawEvent(RawCursorPressEvent(cursor1, 3))
    }

    @Test
    fun testGetAllCursors() {
        val allCursors = tracker.getAllCursors()
        assertEquals(2, allCursors.size)
        assertFalse(allCursors.contains(cursor0))
        assertTrue(allCursors.contains(cursor1))
        assertTrue(allCursors.contains(cursor2))
    }

    @Test
    fun testGetHoveringCursors() {
        val hoveringCursors = tracker.getHoveringCursors()
        assertEquals(1, hoveringCursors.size)
        assertFalse(hoveringCursors.contains(cursor0))
        assertFalse(hoveringCursors.contains(cursor1))
        assertTrue(hoveringCursors.contains(cursor2))
    }

    @Test
    fun testGetCursorState() {
        assertNull(tracker.getCursorState(cursor0))
        assertEquals(TrackedCursor(
            EventPosition(0.05f, 0.2f),
            setOf(3)
        ), tracker.getCursorState(cursor1))
        assertEquals(TrackedCursor(
            EventPosition(0.5f, 0.7f),
            setOf()
        ), tracker.getCursorState(cursor2))
    }
}
