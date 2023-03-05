package gruviks.component.agent

import gruviks.component.RenderResult
import gruviks.event.Event
import gruviks.feedback.Feedback
import kotlin.reflect.KClass

class ComponentAgent(
    val cursorTracker: CursorTracker,
    val giveFeedback: (Feedback) -> Unit
) {
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
