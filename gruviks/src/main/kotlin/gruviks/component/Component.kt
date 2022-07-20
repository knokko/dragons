package gruviks.component

import graviks2d.target.GraviksTarget
import gruviks.event.Event

abstract class Component {
    protected lateinit var agent: ComponentAgent

    fun initAgent(agent: ComponentAgent) {
        if (this::agent.isInitialized) throw IllegalStateException("Agent is already initialized")
        this.agent = agent
    }

    abstract fun subscribeToEvents()

    abstract fun processEvent(event: Event)

    abstract fun render(target: GraviksTarget, force: Boolean): RenderResult
}
