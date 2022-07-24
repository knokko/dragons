package gruviks.component

import graviks2d.target.GraviksTarget
import gruviks.component.agent.ComponentAgent
import gruviks.event.Event

abstract class Component {
    protected lateinit var agent: ComponentAgent

    fun initAgent(agent: ComponentAgent) {
        if (this::agent.isInitialized) throw IllegalStateException("Agent is already initialized")
        this.agent = agent
    }

    abstract fun subscribeToEvents()

    abstract fun processEvent(event: Event)

    /**
     * The **background** regions that should be redrawn **before** the next `render` method of this component is
     * called. The **background** is either the component behind this component, or the background color of the
     * window. The owner of this component should call this method before calling the `render` method of this
     * component (unless it clears the entire background).
     *
     * ## Why it is needed
     * Redrawing the background is needed for some components, for instance moving components and translucent
     * components:
     * - When redrawing a translucent component without clearing the background, the translucent colors would
     * intensify because the component would essentially be drawn over itself.
     * - When redrawing a moving component, you would expect to see the background on all regions except the
     * region where the component is drawn. But, when the component **was** somewhere else, you would see the
     * component on both the old and new position, unless the background is redrawn on the old position.
     *
     * ## Default implementation
     * The default implementation of this method simply returns the entire component domain. This is always
     * correct, but may be suboptimal. (Because the entire background will need to be redrawn every time the
     * component is redrawn, even in cases where there is no need to redraw any of the background.) Components
     * can override this to improve the performance. This is most useful for big components like menus because
     * a lot of redrawing can be avoided.
     */
    fun regionsToRedrawBeforeNextRender(): Collection<BackgroundRegion> = listOf(
        BackgroundRegion(0f, 0f, 1f, 1f)
    )

    abstract fun render(target: GraviksTarget, force: Boolean): RenderResult
}
