package gruviks.component.agent

import gruviks.component.RenderResult
import gruviks.event.Cursor
import gruviks.event.EventPosition
import gruviks.event.raw.RawEventAdapter
import java.util.*

interface CursorTracker {
    fun getAllCursors(): Collection<Cursor>

    fun getHoveringCursors(): Collection<Cursor>

    fun getCursorState(cursor: Cursor): TrackedCursor?
}

class TrackedCursor(
    val localPosition: EventPosition,
    val pressedButtons: Set<Int>
) {
    override fun equals(other: Any?) = other is TrackedCursor && this.localPosition == other.localPosition && this.pressedButtons == other.pressedButtons

    override fun hashCode() = localPosition.hashCode() + 31 * pressedButtons.hashCode()

    override fun toString() = "TrackedCursor($localPosition, pressedButtons=$pressedButtons)"
}

class RootCursorTracker(
    private val rawEventAdapter: RawEventAdapter,
    private val getLastRenderResult: () -> RenderResult?
): CursorTracker {
    override fun getAllCursors() = rawEventAdapter.getAllCursors()

    override fun getHoveringCursors(): Collection<Cursor> {
        val drawnRegion = this.getLastRenderResult()?.drawnRegion ?: return Collections.emptyList()

        return getAllCursors().filter { cursor ->
            val cursorState = rawEventAdapter.getCursorState(cursor) ?: return@filter false
            val position = cursorState.localPosition
            drawnRegion.isInside(position.x, position.y)
        }
    }

    override fun getCursorState(cursor: Cursor) = rawEventAdapter.getCursorState(cursor)
}
