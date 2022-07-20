package gruviks.event.raw

import gruviks.event.*
import java.lang.System.currentTimeMillis

const val CLICK_DURATION_THRESHOLD = 500

class EventAdapter {

    private val lastCursorStates = HashMap<Cursor, CursorState>()

    fun convertRawEvent(event: RawEvent): List<Event> {
        val result = ArrayList<Event>()

        if (event is RawCursorEvent) {

            val cursorState = lastCursorStates[event.cursor]

            if (event is RawCursorPressEvent && cursorState != null) {
                result.add(CursorPressEvent(event.cursor, cursorState.position, event.button))

                val buttonState = cursorState.pressedButtons[event.button]
                if (buttonState != null) buttonState.pressTime = currentTimeMillis()
                else cursorState.pressedButtons[event.button] = CursorButtonState(currentTimeMillis())
            }
            // If cursorState is null, no press event can be fired because the cursor position is unknown.
            // This should normally not happen.

            if (event is RawCursorReleaseEvent && cursorState != null) {

                val buttonState = cursorState.pressedButtons[event.button]

                if (buttonState != null) {
                    result.add(CursorReleaseEvent(event.cursor, cursorState.position, event.button))
                    val pressDuration = currentTimeMillis() - buttonState.pressTime
                    if (pressDuration < CLICK_DURATION_THRESHOLD) {
                        result.add(CursorClickEvent(event.cursor, cursorState.position, event.button))
                    }
                }
                // If buttonState is null, no press event was fired, so no release event should be fired.
                // This could happen if the cursor was pressed while not hovering over the window.

                cursorState.pressedButtons.remove(event.button)
            }
            // If cursorState is null, no release event can be fired because the cursor position is unknown.
            // This should normally not happen.

            if (event is RawCursorMoveEvent) {
                if (cursorState != null) {
                    result.add(CursorMoveEvent(event.cursor, oldPosition = cursorState.position, newPosition = event.newPosition))
                    cursorState.position = event.newPosition
                } else {
                    // If the old cursor state is unknown, we should fire a CursorEnterEvent instead
                    // This can happen if the cursor is already hovering over the window when the window is created
                    result.add(CursorEnterEvent(event.cursor, event.newPosition))
                    lastCursorStates[event.cursor] = CursorState(event.newPosition)
                }
            }

            if (event is RawCursorLeaveEvent) {
                if (cursorState != null) {
                    result.add(CursorLeaveEvent(event.cursor, cursorState.position))
                }
                // Since CursorLeaveEvent requires a position, it can only be fired if that position is known
                lastCursorStates.remove(event.cursor)
            }
        }

        return result
    }
}

private class CursorState(
    var position: EventPosition
) {
    val pressedButtons = HashMap<Int, CursorButtonState>()
}

private class CursorButtonState(
    var pressTime: Long
)
