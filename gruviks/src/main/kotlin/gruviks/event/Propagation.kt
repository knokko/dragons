package gruviks.event

import gruviks.component.Component
import gruviks.component.agent.ComponentAgent

fun propagateEvent(event: Event, component: Component, agent: ComponentAgent) {
    val lastDrawnRegion = agent.lastRenderResult?.drawnRegion
    if (event is CursorMoveEvent) {
        if (!arrayOf(CursorEnterEvent::class, CursorLeaveEvent::class, CursorMoveEvent::class).any { agent.isSubscribed(it) }) return

        // CursorMoveEvent needs special treatment because CursorEnterEvent and CursorLeaveEvent may need to be
        // fired as well
        if (lastDrawnRegion != null) {
            val wasInside = lastDrawnRegion.isInside(event.oldPosition.x, event.oldPosition.y)
            val isInside = lastDrawnRegion.isInside(event.newPosition.x, event.newPosition.y)

            if (!wasInside && isInside && agent.isSubscribed(CursorEnterEvent::class)) {
                component.processEvent(CursorEnterEvent(event.cursor, event.newPosition))
            }
            if (wasInside && !isInside && agent.isSubscribed(CursorLeaveEvent::class)) {
                component.processEvent(CursorLeaveEvent(event.cursor, event.oldPosition))
            }
            if (wasInside && isInside && agent.isSubscribed(CursorMoveEvent::class)) {
                component.processEvent(event)
            }
        }
    } else {
        val shouldProcess = if (event is PositionedEvent) {
            lastDrawnRegion?.isInside(event.position.x, event.position.y) ?: false
        } else { true }

        if (shouldProcess && agent.isSubscribed(event::class)) {
            component.processEvent(event)
        }
    }
}
