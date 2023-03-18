package gruviks.component.menu

import graviks2d.target.ChildTarget
import graviks2d.target.GraviksTarget
import graviks2d.util.Color
import gruviks.component.*
import gruviks.component.agent.ComponentAgent
import gruviks.component.agent.CursorTracker
import gruviks.component.agent.TrackedCursor
import gruviks.event.*
import gruviks.feedback.*
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

    private var aspectRatio = 1f

    private val componentTree = RectTree<ComponentNode>()
    private val componentsToAdd = mutableListOf<Pair<Component, RectRegion>>()

    private var cameraPosition = Point.percentage(0, 0)
    private var lastVisibleRegion: RectRegion? = null

    // The first draw should always draw all components
    private var shouldDoFullDraw = true

    private fun updateComponentTree(giveRenderFeedback: Boolean) {
        while (componentsToAdd.isNotEmpty()) {
            val (component, region) = componentsToAdd.removeLast()
            val childCursorTracker = NodeCursorTracker(agent.cursorTracker, region, this::getVisibleRegion)
            val childFeedback = mutableListOf<Feedback>()
            val childAgent = ComponentAgent(childCursorTracker, childFeedback::add)
            childCursorTracker.getLastRenderResult = { childAgent.lastRenderResult }
            val node = ComponentNode(component, childAgent, childFeedback)
            component.initAgent(childAgent)
            component.subscribeToEvents()
            childAgent.forbidFutureSubscriptions()

            componentTree.insert(node, region)
            if (giveRenderFeedback) agent.giveFeedback(RenderFeedback())
        }
    }

    fun shiftCamera(deltaX: Coordinate, deltaY: Coordinate) {
        cameraPosition = Point(cameraPosition.x + deltaX, cameraPosition.y + deltaY)
        agent.giveFeedback(RenderFeedback())
        shouldDoFullDraw = true
    }

    fun moveCamera(destination: Point) {
        cameraPosition = destination
        agent.giveFeedback(RenderFeedback())
        shouldDoFullDraw = true
    }

    fun getVisibleRegion(): RectRegion {
        val baseRegion = if (layout == SpaceLayout.Simple) RectRegion.percentage(0, 0, 100, 100)
        else if (layout == SpaceLayout.GrowDown) {
            RectRegion(
                Coordinate.percentage(0),
                Coordinate.fromFloat(1f - 1f / aspectRatio),
                Coordinate.percentage(100),
                Coordinate.percentage(100)
            )
        } else if (layout == SpaceLayout.GrowUp) {
            RectRegion(
                Coordinate.percentage(0),
                Coordinate.percentage(0),
                Coordinate.percentage(100),
                Coordinate.fromFloat(1f / aspectRatio)
            )
        } else if (layout == SpaceLayout.GrowRight) {
            RectRegion(
                Coordinate.percentage(0),
                Coordinate.percentage(0),
                Coordinate.fromFloat(aspectRatio),
                Coordinate.percentage(100)
            )
        } else {
            throw IllegalStateException("Unsupported layout: $layout")
        }

        return RectRegion(
            baseRegion.minX + cameraPosition.x,
            baseRegion.minY + cameraPosition.y,
            baseRegion.boundX + cameraPosition.x,
            baseRegion.boundY + cameraPosition.y
        )
    }

    override fun subscribeToEvents() {
        agent.subscribeToAllEvents()

        updateComponentTree(false)
    }

    fun addComponent(component: Component, region: RectRegion) {
        componentsToAdd.add(Pair(component, region))
        if (didInitAgent()) agent.giveFeedback(RenderFeedback())
    }

    override fun processEvent(event: Event) {
        updateComponentTree(true)

        val visitedNodes = mutableSetOf<ComponentNode>()

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
                        visitedNodes.add(targetNode)
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

                visitedNodes.addAll(targetComponents.map { it.second })
            }
        } else {
            throw UnsupportedOperationException("Unknown event $event")
        }

        for (node in visitedNodes) {
            updateNode(node)
        }

        updateComponentTree(true)
    }

    override fun regionsToRedrawBeforeNextRender(): Collection<BackgroundRegion> {
        updateComponentTree(true)

        // There is no need to redraw anything behind the menu when the background color is solid
        if (backgroundColor.alpha == 255) return emptyList()

        // When lastVisibleRegion is null, no render has occurred yet, so there is no need to redraw anything
        if (lastVisibleRegion == null) return emptyList()

        val result = mutableListOf<BackgroundRegion>()

        val currentVisibleRegion = getVisibleRegion()
        val oldVisibleRegion = lastVisibleRegion!!
        val changedVisibleRegion = currentVisibleRegion != oldVisibleRegion

        val potentialComponents = componentTree.findBetween(oldVisibleRegion)
        for ((region, node) in potentialComponents) {
            if ((node.didRequestRender || changedVisibleRegion) && node.agent.lastRenderResult != null) {
                for (childRegion in node.component.regionsToRedrawBeforeNextRender()) {
                    val absoluteMin = region.transform(childRegion.minX, childRegion.minY)
                    val absoluteBounds = region.transform(childRegion.maxX, childRegion.maxY)
                    val visibleMin = oldVisibleRegion.transformBack(absoluteMin.first, absoluteMin.second)
                    val visibleMax = oldVisibleRegion.transformBack(absoluteBounds.first, absoluteBounds.second)
                    result.add(BackgroundRegion(
                        visibleMin.first, visibleMin.second, visibleMax.first, visibleMax.second
                    ))
                }
            }
        }

        return result.filter { it.minX < 1f && it.minY < 1f && it.maxX > 0f && it.maxY > 0f }.map {
            BackgroundRegion(max(0f, it.minX), max(0f, it.minY), min(1f, it.maxX), min(1f, it.maxY))
        }
    }

    override fun render(target: GraviksTarget, force: Boolean): RenderResult {
        this.aspectRatio = target.getAspectRatio()
        updateComponentTree(false)

        if (force) shouldDoFullDraw = true

        val drawnRegions = mutableListOf<DrawnRegion>()
        val recentDrawnRegions = mutableListOf<DrawnRegion>()
        if (backgroundColor.alpha > 0) drawnRegions.add(RectangularDrawnRegion(0f, 0f, 1f, 1f))

        val shouldDrawBackground = shouldDoFullDraw && backgroundColor.alpha > 0
        if (shouldDrawBackground) {
            target.fillRect(0f, 0f, 1f, 1f, backgroundColor)
            recentDrawnRegions.add(RectangularDrawnRegion(0f, 0f, 1f, 1f))
        }

        val visibleRegion = getVisibleRegion()
        val visibleComponents = componentTree.findBetween(visibleRegion)

        for ((region, node) in visibleComponents) {
            val transformedRegion = visibleRegion.transformBack(region)
            if (shouldDoFullDraw || node.didRequestRender) {
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
                        recentDrawnRegions.add(TransformedDrawnRegion(
                                RectangularDrawnRegion(
                                        backgroundRegion.minX, backgroundRegion.minY,
                                        backgroundRegion.maxX, backgroundRegion.maxY
                                ),
                                transformedRegion.minX,
                                transformedRegion.minY,
                                transformedRegion.maxX,
                                transformedRegion.maxY
                        ))
                    }
                }

                node.didRequestRender = false
                node.agent.lastRenderResult = node.component.render(childTarget, shouldDoFullDraw)

                val recentDrawnRegion = node.agent.lastRenderResult?.recentDrawnRegion
                if (recentDrawnRegion != null && !shouldDrawBackground) {
                    recentDrawnRegions.add(TransformedDrawnRegion(
                            recentDrawnRegion,
                            transformedRegion.minX,
                            transformedRegion.minY,
                            transformedRegion.maxX,
                            transformedRegion.maxY
                    ))
                }
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

        for ((_, node) in visibleComponents) {
            updateNode(node)
        }

        shouldDoFullDraw = false
        lastVisibleRegion = visibleRegion

        return RenderResult(
            drawnRegion = if (drawnRegions.isEmpty()) null else if (drawnRegions.size == 1) drawnRegions[0] else CompositeDrawnRegion(drawnRegions),
                recentDrawnRegion = if (recentDrawnRegions.isEmpty()) null else if (recentDrawnRegions.size == 1) recentDrawnRegions[0] else CompositeDrawnRegion(recentDrawnRegions),
            propagateMissedCursorEvents = true
        )
    }

    private fun updateNode(node: ComponentNode) {
        for (feedback in node.feedback) {
            if (feedback is RenderFeedback) {
                node.didRequestRender = true
                agent.giveFeedback(RenderFeedback())
            } else if (feedback is AddressedFeedback) {
                // TODO Check whether it's addressed to me
                agent.giveFeedback(feedback)
            } else if (feedback is ShiftCameraFeedback) {
                shiftCamera(feedback.deltaX, feedback.deltaY)
            } else if (feedback is MoveCameraFeedback) {
                moveCamera(feedback.newPosition)
            } else if (feedback is ReplaceYouFeedback) {
                // TODO Add support for ReplaceMeFeedback
                agent.giveFeedback(ReplaceMeFeedback(feedback.createReplacement))
            } else {
                throw UnsupportedOperationException("Unexpected feedback $feedback")
            }
        }
        node.feedback.clear()
    }
}

private class ComponentNode(
    val component: Component,
    val agent: ComponentAgent,
    val feedback: MutableList<Feedback>
) {
    var didRequestRender = true
}

private class NodeCursorTracker(
    private val parentTracker: CursorTracker,
    private val region: RectRegion,
    private val getVisibleRegion: () -> RectRegion
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
        val (visibleX, visibleY) = getVisibleRegion().transform(parentState.localPosition.x, parentState.localPosition.y)
        val (localX, localY) = region.transformBack(visibleX, visibleY)
        return TrackedCursor(EventPosition(localX, localY), parentState.pressedButtons)
    }
}
