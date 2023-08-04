package gruviks.component.util

import graviks2d.target.GraviksTarget
import gruviks.component.BackgroundRegion
import gruviks.component.Component
import gruviks.component.RenderResult
import gruviks.component.agent.ComponentAgent
import gruviks.component.agent.CursorTracker
import gruviks.event.Cursor
import gruviks.event.Event
import gruviks.event.RemoveEvent
import gruviks.feedback.Feedback
import gruviks.feedback.RenderFeedback

class SwitchComponent(initialComponent: Component): Component() {

    private var lastRenderedComponent: Component? = null
    private var currentComponent = initialComponent

    private val agents = mutableMapOf<Component, ComponentAgent>()
    private val feedbackMap = mutableMapOf<Component, MutableList<Feedback>>()

    init {
        insertAgent(currentComponent)
    }

    fun setComponent(newComponent: Component) {
        currentComponent = newComponent
        if (!agents.containsKey(newComponent)) insertAgent(newComponent)
        agent.giveFeedback(RenderFeedback())
    }

    fun removeComponent(component: Component) {
        if (component === currentComponent) throw IllegalStateException("Can't remove current component")
        val agentToRemove = agents[component]!!
        if (agentToRemove.isSubscribed(RemoveEvent::class)) component.processEvent(RemoveEvent())
        agents.remove(component)
        feedbackMap.remove(component)
    }

    private fun insertAgent(component: Component) {
        val cursorTracker = SwitchCursorTracker(this, component)
        val feedbackList = mutableListOf<Feedback>()
        val hasKeyboardFocus = { component === currentComponent && this.agent.hasKeyboardFocus() }
        agents[component] = ComponentAgent(cursorTracker, feedbackList::add, hasKeyboardFocus)
        component.initAgent(agents[component]!!)
        component.subscribeToEvents()
        feedbackMap[component] = feedbackList
    }

    override fun subscribeToEvents() {
        agent.subscribeToAllEvents()
    }

    private fun processFeedback() {
        val feedbackList = feedbackMap[currentComponent]!!
        for (feedback in feedbackList) agent.giveFeedback(feedback)
        feedbackList.clear()
    }

    override fun processEvent(event: Event) {
        processFeedback()

        if (event is RemoveEvent) {
            for ((component, agent) in agents) {
                if (agent.isSubscribed(RemoveEvent::class)) component.processEvent(event)
            }
        } else {
            val agent = agents[currentComponent]!!
            if (agent.isSubscribed(event::class)) currentComponent.processEvent(event)
        }

        processFeedback()
    }

    override fun regionsToRedrawBeforeNextRender(): Collection<BackgroundRegion> {
        return if (lastRenderedComponent !== currentComponent) super.regionsToRedrawBeforeNextRender()
        else currentComponent.regionsToRedrawBeforeNextRender()
    }

    override fun render(target: GraviksTarget, force: Boolean): RenderResult {
        val result = currentComponent.render(target, force || lastRenderedComponent !== currentComponent)
        lastRenderedComponent = currentComponent
        return result
    }

    private class SwitchCursorTracker(
        private val switchComponent: SwitchComponent,
        private val myComponent: Component
    ): CursorTracker {
        override fun getAllCursors() = switchComponent.agent.cursorTracker.getAllCursors()

        override fun getHoveringCursors() = if (myComponent === switchComponent.currentComponent) {
            switchComponent.agent.cursorTracker.getHoveringCursors()
        } else emptyList()

        override fun getCursorState(cursor: Cursor) = if (myComponent === switchComponent.currentComponent) {
            switchComponent.agent.cursorTracker.getCursorState(cursor)
        } else null
    }
}
