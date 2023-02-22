package gruviks.component.agent

import gruviks.component.RenderResult
import gruviks.event.Event
import kotlin.reflect.KClass

class ComponentAgent(
    val cursorTracker: CursorTracker
) {

    /**
     * This is true by default since every component should be rendered as soon as it is placed
     */
    var didRequestRender = true

    internal var lastRenderResult: RenderResult? = null

    private var allowSubscriptions = true

    private val subscribedEvents = HashSet<KClass<out Event>>()
    private var subscribedToAllEvents = false

    fun subscribe(eventClass: KClass<out Event>) {
        if (!this.allowSubscriptions) throw IllegalStateException("New subscriptions are no longer allowed")

        this.subscribedEvents.add(eventClass)
    }

    fun subscribeToAllEvents() {
        if (!this.allowSubscriptions) throw IllegalStateException("New subscriptions are no longer allowed")

        this.subscribedToAllEvents = true
    }

    fun isSubscribed(eventClass: KClass<out Event>) = this.subscribedToAllEvents || this.subscribedEvents.contains(eventClass)

    fun forbidFutureSubscriptions() {
        this.allowSubscriptions = false
    }
}
