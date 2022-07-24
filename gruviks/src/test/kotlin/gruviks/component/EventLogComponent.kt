package gruviks.component

import graviks2d.target.GraviksTarget
import gruviks.event.Event
import kotlin.reflect.KClass

class EventLogComponent(private val allowedEvents: Set<KClass<out Event>>): Component() {

    val log: MutableList<Event> = mutableListOf()

    override fun subscribeToEvents() {
        allowedEvents.forEach(agent::subscribe)
    }

    override fun processEvent(event: Event) {
        if (allowedEvents.contains(event::class)) {
            log.add(event)
        } else {
            throw IllegalArgumentException("Unsupported event class ${event::class.java}")
        }
    }

    override fun render(target: GraviksTarget, force: Boolean) = RenderResult(
        RectangularDrawnRegion(0.1f, 0.1f, 0.9f, 0.9f), propagateMissedCursorEvents = false
    )
}
