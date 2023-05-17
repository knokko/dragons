package gruviks.event

import kotlin.math.absoluteValue

abstract class Event

class UpdateEvent : Event()

abstract class PositionedEvent(val position: EventPosition): Event() {
    abstract fun copyWitChangedPosition(newPosition: EventPosition): PositionedEvent
}

/**
 * Represents a point in component domain coordinates (so (0, 0) is the bottom-left corner of the component domain and
 * (1, 1) is the top-right corner of the component domain).
 */
class EventPosition(val x: Float, val y: Float) {
    override fun equals(other: Any?) = other is EventPosition && (this.x - other.x).absoluteValue < 0.001f && (this.y - other.y).absoluteValue < 0.001f

    override fun toString() = String.format("(%.3f, %.3f)", x, y)

    override fun hashCode() = x.hashCode() + 31 * y.hashCode()
}
