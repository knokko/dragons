package gruviks.event

import gruviks.space.Point

abstract class Event

abstract class PositionedEvent(val position: EventPosition): Event()

/**
 * Represents a point in component domain coordinates (so (0, 0) is the bottom-left corner of the component domain and
 * (1, 1) is the top-right corner of the component domain).
 */
class EventPosition(val x: Float, val y: Float)
