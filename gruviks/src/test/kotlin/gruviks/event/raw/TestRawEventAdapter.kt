package gruviks.event.raw

import gruviks.component.agent.TrackedCursor
import gruviks.event.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TestRawEventAdapter {

    @Test
    fun testConvertCursorEvents() {
        val cursor1 = Cursor(1)
        val cursor2 = Cursor(2)
        val adapter = RawEventAdapter()

        // Initially, it should be empty
        assertEquals(0, adapter.getAllCursors().size)
        assertNull(adapter.getCursorState(cursor1))

        // The following events should be ignored because no CursorMoveEvent has been fired yet
        // ... and thus the adapter doesn't know the cursor position, which is required to convert the events
        val ignoredEvents = arrayOf(
            RawCursorPressEvent(cursor1, 1),
            RawCursorReleaseEvent(cursor1, 1),
            RawCursorLeaveEvent(cursor1)
        )
        for (event in ignoredEvents) {
            assertEquals(0, adapter.convertRawEvent(event).size)
            assertEquals(0, adapter.getAllCursors().size)
        }

        // Let cursor1 enter
        assertEquals(
            listOf(CursorEnterEvent(cursor1, EventPosition(0.1f, 0.2f))),
            adapter.convertRawEvent(RawCursorMoveEvent(cursor1, EventPosition(0.1f, 0.2f)))
        )
        assertEquals(1, adapter.getAllCursors().size)
        assertTrue(adapter.getAllCursors().contains(cursor1))
        assertEquals(TrackedCursor(EventPosition(0.1f, 0.2f), setOf()), adapter.getCursorState(cursor1))
        assertNull(adapter.getCursorState(cursor2))

        // Let cursor2 enter
        assertEquals(
            listOf(CursorEnterEvent(cursor2, EventPosition(0.3f, 0.4f))),
            adapter.convertRawEvent(RawCursorMoveEvent(cursor2, EventPosition(0.3f, 0.4f)))
        )
        assertEquals(2, adapter.getAllCursors().size)
        assertEquals(TrackedCursor(EventPosition(0.3f, 0.4f), setOf()), adapter.getCursorState(cursor2))

        // And check that cursor1 didn't move when adding cursor2
        assertEquals(TrackedCursor(EventPosition(0.1f, 0.2f), setOf()), adapter.getCursorState(cursor1))

        // Move cursor1
        assertEquals(
            listOf(CursorMoveEvent(cursor1, EventPosition(0.1f, 0.2f), EventPosition(0.2f, 0.1f))),
            adapter.convertRawEvent(RawCursorMoveEvent(cursor1, EventPosition(0.2f, 0.1f)))
        )
        assertEquals(2, adapter.getAllCursors().size)
        assertEquals(TrackedCursor(EventPosition(0.2f, 0.1f), setOf()), adapter.getCursorState(cursor1))
        assertEquals(TrackedCursor(EventPosition(0.3f, 0.4f), setOf()), adapter.getCursorState(cursor2))

        // Press button 3 of cursor2
        assertEquals(
            listOf(CursorPressEvent(cursor2, EventPosition(0.3f, 0.4f), 3)),
            adapter.convertRawEvent(RawCursorPressEvent(cursor2, 3))
        )
        assertEquals(TrackedCursor(EventPosition(0.2f, 0.1f), setOf()), adapter.getCursorState(cursor1))
        assertEquals(TrackedCursor(EventPosition(0.3f, 0.4f), setOf(3)), adapter.getCursorState(cursor2))

        // Press button 0 of cursor1
        assertEquals(
            listOf(CursorPressEvent(cursor1, EventPosition(0.2f, 0.1f), 0)),
            adapter.convertRawEvent(RawCursorPressEvent(cursor1, 0))
        )
        assertEquals(TrackedCursor(EventPosition(0.2f, 0.1f), setOf(0)), adapter.getCursorState(cursor1))
        assertEquals(TrackedCursor(EventPosition(0.3f, 0.4f), setOf(3)), adapter.getCursorState(cursor2))

        // Press button 1 of cursor1
        assertEquals(
            listOf(CursorPressEvent(cursor1, EventPosition(0.2f, 0.1f), 1)),
            adapter.convertRawEvent(RawCursorPressEvent(cursor1, 1))
        )
        assertEquals(TrackedCursor(EventPosition(0.2f, 0.1f), setOf(0, 1)), adapter.getCursorState(cursor1))
        assertEquals(TrackedCursor(EventPosition(0.3f, 0.4f), setOf(3)), adapter.getCursorState(cursor2))

        // Release button 0 of cursor1
        assertEquals(
            listOf(
                CursorReleaseEvent(cursor1, EventPosition(0.2f, 0.1f), 0),
                CursorClickEvent(cursor1, EventPosition(0.2f, 0.1f), 0)
            ), adapter.convertRawEvent(RawCursorReleaseEvent(cursor1, 0))
        )
        assertEquals(TrackedCursor(EventPosition(0.2f, 0.1f), setOf(1)), adapter.getCursorState(cursor1))
        assertEquals(TrackedCursor(EventPosition(0.3f, 0.4f), setOf(3)), adapter.getCursorState(cursor2))

        // Release button 3 of cursor2
        assertEquals(
            listOf(
                CursorReleaseEvent(cursor2, EventPosition(0.3f, 0.4f), 3),
                CursorClickEvent(cursor2, EventPosition(0.3f, 0.4f), 3)
            ), adapter.convertRawEvent(RawCursorReleaseEvent(cursor2, 3))
        )
        assertEquals(TrackedCursor(EventPosition(0.2f, 0.1f), setOf(1)), adapter.getCursorState(cursor1))
        assertEquals(TrackedCursor(EventPosition(0.3f, 0.4f), setOf()), adapter.getCursorState(cursor2))

        // Let cursor1 leave
        assertEquals(
            listOf(CursorLeaveEvent(cursor1, EventPosition(0.2f, 0.1f))),
            adapter.convertRawEvent(RawCursorLeaveEvent(cursor1))
        )
        assertEquals(1, adapter.getAllCursors().size)
        assertNull(adapter.getCursorState(cursor1))
        assertEquals(TrackedCursor(EventPosition(0.3f, 0.4f), setOf()), adapter.getCursorState(cursor2))

        // Let cursor2 leave
        assertEquals(
            listOf(CursorLeaveEvent(cursor2, EventPosition(0.3f, 0.4f))),
            adapter.convertRawEvent(RawCursorLeaveEvent(cursor2))
        )
        assertEquals(0, adapter.getAllCursors().size)
        assertNull(adapter.getCursorState(cursor2))
    }
}
