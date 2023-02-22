package gruviks.component.menu

import graviks2d.target.ChildTarget
import graviks2d.target.GraviksTarget
import graviks2d.util.Color
import gruviks.component.*
import gruviks.component.agent.ComponentAgent
import gruviks.component.agent.CursorTracker
import gruviks.component.agent.TrackedCursor
import gruviks.event.*
import gruviks.space.*
import java.lang.Float.max
import java.lang.Float.min

/**
 * A flat menu where with a finite number of components that are always stored in main memory.
 */
class SimpleFlatMenu(
    val layout: SpaceLayout,
    var backgroundColor: Color
): Component() {

    private val componentTree = RectTree<ComponentNode>()
    private val componentsToAdd = mutableListOf<Pair<Component, RectRegion>>()

    // We consider the 'initial background' to be transparent
    private var didChangeBackground = backgroundColor.alpha != 0

    private fun updateComponentTree() {
        while (componentsToAdd.isNotEmpty()) {
            val (component, region) = componentsToAdd.removeLast()
            val childCursorTracker = NodeCursorTracker(agent.cursorTracker, region)
            val childAgent = ComponentAgent(childCursorTracker)
            childCursorTracker.getLastRenderResult = { childAgent.lastRenderResult }
            val node = ComponentNode(component, childAgent)
            component.initAgent(childAgent)
            component.subscribeToEvents()
            childAgent.forbidFutureSubscriptions()

            componentTree.insert(node, region)
            agent.didRequestRender = true
        }
    }

    private fun getVisibleRegion() = RectRegion.percentage(0, 0, 100, 100) // TODO Respect layout

    override fun subscribeToEvents() {
        agent.subscribeToAllEvents()

        updateComponentTree()
    }

    fun addComponent(component: Component, region: RectRegion) {
        componentsToAdd.add(Pair(component, region))
    }

    // TODO Test this
    override fun processEvent(event: Event) {
        updateComponentTree()

        val visibleRegion = getVisibleRegion()
        if (event is PositionedEvent) {

            val (transformedX, transformedY) = visibleRegion.transform(event.position.x, event.position.y)
            val transformedPoint = Point.fromFloat(transformedX, transformedY)

            val targetComponents = componentTree.findBetween(transformedPoint.toRectRegion())
            if (targetComponents.isNotEmpty()) {
                assert(targetComponents.size == 1)
                val (targetRegion, targetNode) = targetComponents[0]

                if (targetNode.agent.isSubscribed(event::class)) {
                    val (componentX, componentY) = targetRegion.transformBack(transformedX, transformedY)
                    val renderResult = targetNode.agent.lastRenderResult

                    if (renderResult?.drawnRegion != null && renderResult.drawnRegion.isInside(componentX, componentY)) {
                        val transformedEvent = event.copyWitChangedPosition(EventPosition(componentX, componentY))
                        targetNode.component.processEvent(transformedEvent)
                        if (targetNode.agent.didRequestRender) this.agent.didRequestRender = true
                    }
                }
            }
        } else if (event is CursorMoveEvent) {
            // TODO Maybe remember old mouse position to avoid potential precision issues
            val (oldX, oldY) = visibleRegion.transform(event.oldPosition.x, event.oldPosition.y)
            val (newX, newY) = visibleRegion.transform(event.newPosition.x, event.newPosition.y)

            val minX = min(oldX, newX)
            val minY = min(oldY, newY)
            val maxX = max(oldX, newX)
            val maxY = max(oldY, newY)

            val margin = 0.01f
            val relevantRegion = RectRegion.fromFloat(
                minX - margin, minY - margin, maxX + margin, maxY + margin
            )
            val targetComponents = componentTree.findBetween(relevantRegion).filter { (_, node) ->
                node.agent.lastRenderResult?.drawnRegion != null &&
                        (node.agent.isSubscribed(CursorEnterEvent::class)
                        || node.agent.isSubscribed(CursorLeaveEvent::class)
                        || node.agent.isSubscribed(CursorMoveEvent::class))
            }
            if (targetComponents.isNotEmpty()) {
                val numSteps = 100
                val deltaX = newX - oldX
                val deltaY = newY - oldY
                for ((targetRegion, targetNode) in targetComponents) {
                    var fromX = oldX
                    var fromY = oldY

                    var lastInsideStep = 0
                    var (lastInsideX, lastInsideY) = targetRegion.transformBack(fromX, fromY)

                    for (step in 1 .. numSteps) {
                        val toX = oldX + step * deltaX / numSteps
                        val toY = oldY + step * deltaY / numSteps

                        val renderResult = targetNode.agent.lastRenderResult!!

                        val (componentFromX, componentFromY) = targetRegion.transformBack(fromX, fromY)
                        val (componentToX, componentToY) = targetRegion.transformBack(toX, toY)
                        val fromInside = renderResult.drawnRegion!!.isInside(componentFromX, componentFromY)
                        val toInside = renderResult.drawnRegion.isInside(componentToX, componentToY)

                        if (!fromInside && toInside) {
                            lastInsideX = componentToX
                            lastInsideY = componentToY
                            lastInsideStep = step
                        }

                        if (fromInside && !toInside) {
                            if (step - 1 != lastInsideStep && targetNode.agent.isSubscribed(CursorMoveEvent::class)) {
                                targetNode.component.processEvent(CursorMoveEvent(
                                    event.cursor, EventPosition(lastInsideX, lastInsideY),
                                    EventPosition(componentFromX, componentFromY)
                                ))
                            }
                            if (targetNode.agent.isSubscribed(CursorLeaveEvent::class)) {
                                targetNode.component.processEvent(CursorLeaveEvent(
                                        event.cursor, EventPosition(componentFromX, componentFromY)
                                ))
                            }
                        }

                        if (!fromInside && toInside && targetNode.agent.isSubscribed(CursorEnterEvent::class)) {
                            targetNode.component.processEvent(CursorEnterEvent(
                                event.cursor, EventPosition(componentToX, componentToY)
                            ))
                        }

                        if (step == numSteps && toInside && lastInsideStep != step && targetNode.agent.isSubscribed(CursorMoveEvent::class)) {
                            targetNode.component.processEvent(CursorMoveEvent(
                                event.cursor, EventPosition(lastInsideX, lastInsideY), EventPosition(componentToX, componentToY)
                            ))
                        }

                        fromX = toX
                        fromY = toY
                    }
                }

                for ((_, node) in targetComponents) {
                    if (node.agent.didRequestRender) this.agent.didRequestRender = true
                }
            }
        } else {
            throw UnsupportedOperationException("Unknown event $event")
        }

        updateComponentTree()
    }

    override fun regionsToRedrawBeforeNextRender(): Collection<BackgroundRegion> {
        updateComponentTree()

        // There is no need to redraw anything behind the menu when the background color is solid
        if (backgroundColor.alpha == 255) return emptyList()

        val result = mutableListOf<BackgroundRegion>()
        val visibleRegion = getVisibleRegion()
        val potentialComponents = componentTree.findBetween(visibleRegion)
        for ((region, node) in potentialComponents) {
            if (node.agent.didRequestRender && node.agent.lastRenderResult != null) {
                for (childRegion in node.component.regionsToRedrawBeforeNextRender()) {
                    val absoluteMin = region.transform(childRegion.minX, childRegion.minY)
                    val absoluteBounds = region.transform(childRegion.maxX, childRegion.maxY)
                    val visibleMin = visibleRegion.transformBack(absoluteMin.first, absoluteMin.second)
                    val visibleMax = visibleRegion.transformBack(absoluteBounds.first, absoluteBounds.second)
                    result.add(BackgroundRegion(
                        visibleMin.first, visibleMin.second, visibleMax.first, visibleMax.second
                    ))
                }
            }
        }
        return result
    }

    override fun render(target: GraviksTarget, force: Boolean): RenderResult {
        updateComponentTree()

        val drawnRegions = mutableListOf<DrawnRegion>()
        if (backgroundColor.alpha > 0) drawnRegions.add(RectangularDrawnRegion(0f, 0f, 1f, 1f))

        val shouldDrawBackground = (force || didChangeBackground) && backgroundColor.alpha > 0
        if (shouldDrawBackground) target.fillRect(0f, 0f, 1f, 1f, backgroundColor)
        didChangeBackground = false

        val visibleRegion = getVisibleRegion()
        val visibleComponents = componentTree.findBetween(visibleRegion)

        for ((region, node) in visibleComponents) {
            val transformedRegion = visibleRegion.transformBack(region)
            if (force || node.agent.didRequestRender) {
                val childTarget = ChildTarget(
                    target, transformedRegion.minX, transformedRegion.minY, transformedRegion.maxX, transformedRegion.maxY
                )

                if (!shouldDrawBackground && backgroundColor.alpha > 0) {
                    for (backgroundRegion in node.component.regionsToRedrawBeforeNextRender()) {
                        childTarget.fillRect(
                            backgroundRegion.minX, backgroundRegion.minY,
                            backgroundRegion.maxX, backgroundRegion.maxY,
                            backgroundColor
                        )
                    }
                }

                node.agent.didRequestRender = false
                val childRenderResult = node.component.render(childTarget, force)
                if (node.agent.didRequestRender) this.agent.didRequestRender = true
                node.agent.lastRenderResult = childRenderResult
            }

            val childRenderResult = node.agent.lastRenderResult
            if (childRenderResult != null && backgroundColor.alpha == 0 && childRenderResult.drawnRegion != null) {
                drawnRegions.add(TransformedDrawnRegion(
                    childRenderResult.drawnRegion,
                    transformedRegion.minX,
                    transformedRegion.minY,
                    transformedRegion.maxX,
                    transformedRegion.maxY
                ))
            }
        }

        return RenderResult(
            drawnRegion = if (drawnRegions.isEmpty()) null else if (drawnRegions.size == 1) drawnRegions[0] else CompositeDrawnRegion(drawnRegions),
            propagateMissedCursorEvents = true
        )
    }
}

private class ComponentNode(
    val component: Component,
    val agent: ComponentAgent
)

private class NodeCursorTracker(
    private val parentTracker: CursorTracker,
    private val region: RectRegion
): CursorTracker {

    lateinit var getLastRenderResult: () -> RenderResult?

    override fun getAllCursors() = parentTracker.getAllCursors()

    override fun getHoveringCursors() = parentTracker.getHoveringCursors().filter {
        val renderResult = getLastRenderResult()
        val state = getCursorState(it)

        state != null && renderResult?.drawnRegion != null && renderResult.drawnRegion.isInside(
            state.localPosition.x,
            state.localPosition.y
        )
    }

    override fun getCursorState(cursor: Cursor): TrackedCursor? {
        val parentState = parentTracker.getCursorState(cursor) ?: return null
        val (localX, localY) = region.transformBack(parentState.localPosition.x, parentState.localPosition.y)
        return TrackedCursor(EventPosition(localX, localY), parentState.pressedButtons)
    }
}
