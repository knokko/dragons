package gruviks.core

import graviks2d.target.GraviksTarget
import graviks2d.util.Color
import gruviks.component.Component
import gruviks.component.ComponentAgent
import gruviks.event.*

class GruviksWindow(
    private var rootComponent: Component
) {
    private lateinit var rootAgent: ComponentAgent

    init {
        this.setRootComponent(rootComponent)
    }

    fun setRootComponent(newComponent: Component) {
        this.rootComponent = newComponent
        this.rootAgent = ComponentAgent()

        this.rootComponent.initAgent(this.rootAgent)
        this.rootComponent.subscribeToEvents()
    }

    fun fireEvent(event: Event) {
        val lastRenderResult = this.rootAgent.lastRenderResult
        if (event is CursorMoveEvent) {
            // CursorMoveEvent needs special treatment because CursorEnterEvent and CursorLeaveEvent may need to be
            // fired as well
            if (lastRenderResult != null) {
                val wasInside = lastRenderResult.drawnRegion.isInside(event.oldPosition.x, event.oldPosition.y)
                val isInside = lastRenderResult.drawnRegion.isInside(event.newPosition.x, event.newPosition.y)

                if (!wasInside && isInside) {
                    this.rootComponent.processEvent(CursorEnterEvent(event.cursor, event.newPosition))
                }
                if (wasInside && !isInside) {
                    this.rootComponent.processEvent(CursorLeaveEvent(event.cursor, event.oldPosition))
                }
                if (wasInside && isInside) {
                    this.rootComponent.processEvent(event)
                }
            }
        } else {
            val shouldProcess = if (event is PositionedEvent) {
                lastRenderResult?.drawnRegion?.isInside(event.position.x, event.position.y) ?: false
            } else { true }
            if (shouldProcess) {
                this.rootComponent.processEvent(event)
            }
        }
    }

    fun render(target: GraviksTarget, force: Boolean): Boolean {
        return if (force || this.rootAgent.didRequestRender) {

            // When the render is forced, we should invalidate any previously drawn content
            if (force) {
                target.fillRect(0f, 0f, 1f, 1f, Color.rgbInt(0, 0, 0))
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
