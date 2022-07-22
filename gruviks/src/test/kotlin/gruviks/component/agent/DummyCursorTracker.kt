package gruviks.component.agent

import gruviks.event.Cursor
import java.util.*

class DummyCursorTracker: CursorTracker {
    override fun getAllCursors(): Collection<Cursor> = Collections.emptyList()

    override fun getHoveringCursors(): Collection<Cursor> = Collections.emptyList()

    override fun getCursorState(cursor: Cursor): TrackedCursor? = null
}
