package gruviks.core

import graviks2d.target.GraviksTarget
import graviks2d.util.Color
import gruviks.component.Component
import gruviks.component.RectangularDrawnRegion
import gruviks.component.agent.ComponentAgent
import gruviks.component.agent.RootCursorTracker
import gruviks.event.*
import gruviks.event.raw.RawEventAdapter
import gruviks.event.raw.RawEvent
import gruviks.feedback.*

class GruviksWindow(
    private var rootComponent: Component
) {
    private lateinit var rootAgent: ComponentAgent
    private var didRequestRender = true
    private var didRequestExit = false
    private var nextComponent: (() -> Component)? = null
    private val eventAdapter = RawEventAdapter()
    private val cursorTracker = RootCursorTracker(eventAdapter) { this.rootAgent.lastRenderResult }

    private var isFirstRenderOfRootComponent = true
    private var keyboardFocusState = KeyboardFocusState.NoFocus

    init {
        this.setRootComponent(rootComponent)
    }

    fun shouldExit() = this.didRequestExit

    private fun processFeedback(feedback: Feedback) {
        if (feedback is RenderFeedback) {
            this.didRequestRender = true
        } else if (feedback is AddressedFeedback) {
            if (feedback.targetID != null) throw IllegalArgumentException("Received missed feedback $feedback")
            processFeedback(feedback.targetFeedback)
        } else if (feedback is RequestKeyboardFocusFeedback) {
            this.keyboardFocusState = KeyboardFocusState.GettingFocus
        } else if (feedback is ReleaseKeyboardFocusFeedback) {
            this.keyboardFocusState = KeyboardFocusState.LosingFocus
        } else if (feedback is ExitFeedback) {
            this.didRequestExit = true
        } else if (feedback is ReplaceMeFeedback) {
            this.nextComponent = feedback.createReplacement
        } else {
            throw UnsupportedOperationException("Unexpected feedback $feedback")
        }
    }

    fun setRootComponent(newComponent: Component) {
        if (this::rootAgent.isInitialized && this.rootAgent.isSubscribed(RemoveEvent::class)) {
            this.rootComponent.processEvent(RemoveEvent())
        }
        this.rootComponent = newComponent
        this.rootAgent = ComponentAgent(cursorTracker, this::processFeedback) { this.keyboardFocusState == KeyboardFocusState.Focus }
        this.didRequestRender = true

        this.rootComponent.initAgent(this.rootAgent)
        this.rootComponent.subscribeToEvents()
        this.rootAgent.forbidFutureSubscriptions()

        this.isFirstRenderOfRootComponent = true
    }

    private fun checkNextComponent() {
        if (this.keyboardFocusState == KeyboardFocusState.GettingFocus) {
            this.keyboardFocusState = KeyboardFocusState.Focus
            rootComponent.processEvent(KeyboardFocusAcquiredEvent())
        }

        if (this.keyboardFocusState == KeyboardFocusState.LosingFocus) {
            this.keyboardFocusState = KeyboardFocusState.NoFocus
            rootComponent.processEvent(KeyboardFocusLostEvent())
        }

        val nextComponent = this.nextComponent
        if (nextComponent != null) {
            this.nextComponent = null
            this.setRootComponent(nextComponent())
        }
    }

    fun fireEvent(rawEvent: RawEvent) {
        this.checkNextComponent()

        for (event in this.eventAdapter.convertRawEvent(rawEvent)) {
            propagateEvent(event, rootComponent, rootAgent)
        }
    }

    fun render(target: GraviksTarget, force: Boolean, outDrawnRegions: MutableCollection<RectangularDrawnRegion>?) {
        this.checkNextComponent()
        if (force || this.didRequestRender) {

            if (force) {

                // When the render is forced, we should invalidate any previously drawn content
                target.fillRect(0f, 0f, 1f, 1f, Color.BLACK)
                outDrawnRegions?.add(RectangularDrawnRegion(0f, 0f, 1f, 1f))
            } else {
                // If the render is not forced, we should only redraw the background areas requested by the component
                for (backgroundRegion in this.rootComponent.regionsToRedrawBeforeNextRender()) {
                    target.fillRect(
                        backgroundRegion.minX, backgroundRegion.minY, backgroundRegion.maxX, backgroundRegion.maxY,
                        Color.BLACK
                    )
                    outDrawnRegions?.add(RectangularDrawnRegion(
                            backgroundRegion.minX, backgroundRegion.minY, backgroundRegion.maxX, backgroundRegion.maxY
                    ))
                }
            }

            this.didRequestRender = false
            this.rootAgent.lastRenderResult = this.rootComponent.render(target, force)
            if (!force && outDrawnRegions != null) {
                this.rootAgent.lastRenderResult?.recentDrawnRegion?.pushRectangles(outDrawnRegions)
            }

            if (this.isFirstRenderOfRootComponent) {
                for (cursor in this.cursorTracker.getHoveringCursors()) {
                    this.rootComponent.processEvent(CursorEnterEvent(cursor, this.cursorTracker.getCursorState(cursor)!!.localPosition))
                }
                this.isFirstRenderOfRootComponent = false
            }

            checkNextComponent()
        }
    }
}

private enum class KeyboardFocusState {
    NoFocus,
    Focus,
    GettingFocus,
    LosingFocus
}
