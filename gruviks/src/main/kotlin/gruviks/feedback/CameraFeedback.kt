package gruviks.feedback

import gruviks.space.Coordinate
import gruviks.space.Point

class ShiftCameraFeedback(val deltaX: Coordinate, val deltaY: Coordinate) : Feedback()

class MoveCameraFeedback(val newPosition: Point) : Feedback()
