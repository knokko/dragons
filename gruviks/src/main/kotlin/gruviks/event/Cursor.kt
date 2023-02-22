package gruviks.event

@JvmInline
value class Cursor(private val id: Int) {
    override fun toString() = "Cursor($id)"
}

class CursorPressEvent(
    val cursor: Cursor,
    position: EventPosition,
    val button: Int
): PositionedEvent(position) {
    override fun copyWitChangedPosition(newPosition: EventPosition) = CursorPressEvent(cursor, newPosition, button)

    override fun equals(other: Any?): Boolean {
        return if (other is CursorPressEvent) {
            this.cursor == other.cursor && this.position == other.position && this.button == other.button
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        var result = cursor.hashCode()
        result = 31 * result + position.hashCode()
        result = 31 * result + button
        return result
    }
}

class CursorReleaseEvent(
    val cursor: Cursor,
    position: EventPosition,
    val button: Int
): PositionedEvent(position) {
    override fun copyWitChangedPosition(newPosition: EventPosition) = CursorReleaseEvent(cursor, newPosition, button)

    override fun equals(other: Any?): Boolean {
        return if (other is CursorReleaseEvent) {
            this.cursor == other.cursor && this.position == other.position && this.button == other.button
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        var result = cursor.hashCode()
        result = 31 * result + position.hashCode()
        result = 31 * result + button
        return result
    }
}

class CursorClickEvent(
    val cursor: Cursor,
    position: EventPosition,
    val button: Int
): PositionedEvent(position) {
    override fun copyWitChangedPosition(newPosition: EventPosition) = CursorClickEvent(cursor, newPosition, button)

    override fun equals(other: Any?): Boolean {
        return if (other is CursorClickEvent) {
            this.cursor == other.cursor && this.position == other.position && this.button == other.button
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        var result = cursor.hashCode()
        result = 31 * result + position.hashCode()
        result = 31 * result + button
        return result
    }
}

class CursorEnterEvent(val cursor: Cursor, position: EventPosition): PositionedEvent(position) {
    override fun copyWitChangedPosition(newPosition: EventPosition) = CursorEnterEvent(cursor, newPosition)

    override fun equals(other: Any?) = other is CursorEnterEvent && this.cursor == other.cursor && this.position == other.position

    override fun hashCode() = cursor.hashCode() + 31 * position.hashCode()
}

class CursorLeaveEvent(val cursor: Cursor, position: EventPosition): PositionedEvent(position) {
    override fun copyWitChangedPosition(newPosition: EventPosition) = CursorLeaveEvent(cursor, newPosition)

    override fun equals(other: Any?) = other is CursorLeaveEvent && this.cursor == other.cursor && this.position == other.position

    override fun hashCode() = cursor.hashCode() + 31 * position.hashCode()
}

class CursorMoveEvent(
    val cursor: Cursor,
    val oldPosition: EventPosition,
    val newPosition: EventPosition
): Event() {
    override fun equals(other: Any?): Boolean {
        return if (other is CursorMoveEvent) {
            this.cursor == other.cursor && this.oldPosition == other.oldPosition && this.newPosition == other.newPosition
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        var result = cursor.hashCode()
        result = 31 * result + oldPosition.hashCode()
        result = 31 * result + newPosition.hashCode()
        return result
    }
}
