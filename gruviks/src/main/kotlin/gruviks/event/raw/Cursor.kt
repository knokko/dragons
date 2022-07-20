package gruviks.event.raw

import gruviks.event.Cursor
import gruviks.event.EventPosition

abstract class RawCursorEvent(val cursor: Cursor): RawEvent()

class RawCursorPressEvent(
    cursor: Cursor,
    val button: Int
): RawCursorEvent(cursor)

class RawCursorReleaseEvent(
    cursor: Cursor,
    val button: Int
): RawCursorEvent(cursor)

class RawCursorMoveEvent(
    cursor: Cursor,
    val newPosition: EventPosition
): RawCursorEvent(cursor)

class RawCursorLeaveEvent(cursor: Cursor): RawCursorEvent(cursor)
