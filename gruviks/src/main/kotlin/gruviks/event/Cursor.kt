package gruviks.event

@JvmInline
value class Cursor(private val id: Int) {
    override fun toString() = "Cursor($id)"
}

class CursorPressEvent(
    val cursor: Cursor,
    position: EventPosition,
    val button: Int
): PositionedEvent(position)

class CursorReleaseEvent(
    val cursor: Cursor,
    position: EventPosition,
    val button: Int
): PositionedEvent(position)

class CursorClickEvent(
    val cursor: Cursor,
    position: EventPosition,
    val button: Int
): PositionedEvent(position)

class CursorEnterEvent(val cursor: Cursor, position: EventPosition): PositionedEvent(position)

class CursorLeaveEvent(val cursor: Cursor, position: EventPosition): PositionedEvent(position)

class CursorMoveEvent(
    val cursor: Cursor,
    val oldPosition: EventPosition,
    val newPosition: EventPosition
): Event()
