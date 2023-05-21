package gruviks.event.raw

import gruviks.event.Cursor
import gruviks.event.EventPosition
import gruviks.event.ScrollDirection

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

class RawCursorScrollEvent(
    cursor: Cursor,
    val amount: Float,
    val direction: ScrollDirection
) : RawCursorEvent(cursor)
