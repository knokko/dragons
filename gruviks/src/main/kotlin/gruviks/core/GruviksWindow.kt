package gruviks.core

import graviks2d.target.GraviksTarget
import graviks2d.util.Color
import gruviks.component.Component
import gruviks.component.agent.ComponentAgent
import gruviks.component.agent.RootCursorTracker
import gruviks.event.*
import gruviks.event.raw.RawEventAdapter
import gruviks.event.raw.RawEvent

class GruviksWindow(
    private var rootComponent: Component
) {
    private lateinit var rootAgent: ComponentAgent
    private val eventAdapter = RawEventAdapter()

    init {
        this.setRootComponent(rootComponent)
    }

    fun setRootComponent(newComponent: Component) {
        this.rootComponent = newComponent
        this.rootAgent = ComponentAgent(RootCursorTracker(eventAdapter) { this.rootAgent.lastRenderResult })

        this.rootComponent.initAgent(this.rootAgent)
        this.rootComponent.subscribeToEvents()
        this.rootAgent.forbidFutureSubscriptions()
    }

    fun fireEvent(rawEvent: RawEvent) {
        for (event in this.eventAdapter.convertRawEvent(rawEvent)) {
            propagateEvent(event, rootComponent, rootAgent)
        }
    }

    fun render(target: GraviksTarget, force: Boolean): Boolean {
        return if (force || this.rootAgent.didRequestRender) {

            if (force) {

                // When the render is forced, we should invalidate any previously drawn content
                target.fillRect(0f, 0f, 1f, 1f, Color.BLACK)
            } else {
                // If the render is not forced, we should only redraw the background areas requested by the component
                for (backgroundRegion in this.rootComponent.regionsToRedrawBeforeNextRender()) {
                    target.fillRect(
                        backgroundRegion.minX, backgroundRegion.minY, backgroundRegion.maxX, backgroundRegion.maxY,
                        Color.BLACK
                    )
                }
            }

            this.rootAgent.didRequestRender = false

            this.rootAgent.lastRenderResult = this.rootComponent.render(target, force)

            // TODO Work after events
            true
        } else {
            false
        }
    }
}
