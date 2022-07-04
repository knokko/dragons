package gruviks.component

import gruviks.event.cursor.Event
import kotlin.reflect.KClass

class ComponentAgent {

    /**
     * This is true by default since every component should be rendered as soon as it is placed
     */
    var didRequestRender = true

    internal var lastRenderResult: RenderResult? = null

    private var allowSubscriptions = true

    private val subscribedEvents = HashSet<KClass<out Event>>()

    fun subscribe(eventClass: KClass<out Event>) {
        if (!this.allowSubscriptions) throw IllegalStateException("New subscriptions are no longer allowed")

        this.subscribedEvents.add(eventClass)
    }

    fun isSubscribed(eventClass: KClass<out Event>) = this.subscribedEvents.contains(eventClass)

    fun forbidFutureSubscriptions() {
        this.allowSubscriptions = false
    }
}
