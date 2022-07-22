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
)

class RootCursorTracker(
    private val rawEventAdapter: RawEventAdapter,
    private val getLastRenderResult: () -> RenderResult?
): CursorTracker {
    override fun getAllCursors() = rawEventAdapter.getAllCursors()

    override fun getHoveringCursors(): Collection<Cursor> {
        val lastRenderResult = this.getLastRenderResult() ?: return Collections.emptyList()

        return getAllCursors().filter { cursor ->
            val cursorState = rawEventAdapter.getCursorState(cursor) ?: return@filter false
            val position = cursorState.localPosition
            lastRenderResult.drawnRegion.isInside(position.x, position.y)
        }
    }

    override fun getCursorState(cursor: Cursor) = rawEventAdapter.getCursorState(cursor)
}
