package gruviks.component.menu

import graviks2d.target.GraviksTarget
import graviks2d.util.Color
import gruviks.component.*
import gruviks.component.agent.ComponentAgent
import gruviks.component.agent.CursorTracker
import gruviks.component.agent.DummyCursorTracker
import gruviks.component.agent.TrackedCursor
import gruviks.event.*
import gruviks.space.RectRegion
import gruviks.space.SpaceLayout
import gruviks.util.DummyGraviksTarget
import gruviks.util.FillRectCall
import gruviks.util.LoggedGraviksTarget
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.abs

class TestSimpleFlatMenu {

    // TODO Test getVisibleRegion

    @Test
    fun testProcessPositionedEvents() {
        val allowedEvents = setOf(
            CursorEnterEvent::class, CursorLeaveEvent::class, CursorPressEvent::class,
            CursorReleaseEvent::class, CursorClickEvent::class
        )
        val usedComponent = EventLogComponent(allowedEvents)
        val unusedComponent = EventLogComponent(allowedEvents)
        val centerPosition = EventPosition(0.6f, 0.7f)
        val cursor = Cursor(1)
        val button = 1

        val menu = SimpleFlatMenu(SpaceLayout.Simple, Color.BLACK)
        val agent = ComponentAgent(DummyCursorTracker())
        menu.initAgent(agent)
        menu.subscribeToEvents()
        for (event in arrayOf(
            CursorEnterEvent::class, CursorPressEvent::class, CursorReleaseEvent::class,
            CursorClickEvent::class, CursorLeaveEvent::class
        )) {
            assertTrue(agent.isSubscribed(event))
        }
        menu.addComponent(usedComponent, RectRegion.percentage(40, 60, 80, 80))
        menu.addComponent(unusedComponent, RectRegion.percentage(20, 0, 50, 30))

        // No events can be received until the first render
        menu.processEvent(CursorClickEvent(cursor, centerPosition, button))
        assertTrue(usedComponent.log.isEmpty())

        menu.render(DummyGraviksTarget(), false)

        menu.processEvent(CursorEnterEvent(cursor, centerPosition))
        menu.processEvent(CursorPressEvent(cursor, centerPosition, button))
        menu.processEvent(CursorReleaseEvent(cursor, centerPosition, button))
        menu.processEvent(CursorClickEvent(cursor, centerPosition, button))
        menu.processEvent(CursorLeaveEvent(cursor, centerPosition))

        // The unused component should *not* have received any events
        assertTrue(unusedComponent.log.isEmpty())

        fun checkPosition(actual: EventPosition) {
            assertEquals(0.5f, actual.x, 0.001f)
            assertEquals(0.5f, actual.y, 0.001f)
        }

        // The used component should have received all events
        assertEquals(5, usedComponent.log.size)
        usedComponent.log[0].let {
            val event = it as CursorEnterEvent
            assertEquals(cursor, event.cursor)
            checkPosition(event.position)
        }
        usedComponent.log[1].let {
            val event = it as CursorPressEvent
            assertEquals(cursor, event.cursor)
            checkPosition(event.position)
            assertEquals(button, event.button)
        }
        usedComponent.log[2].let {
            val event = it as CursorReleaseEvent
            assertEquals(cursor, event.cursor)
            checkPosition(event.position)
            assertEquals(button, event.button)
        }
        usedComponent.log[3].let {
            val event = it as CursorClickEvent
            assertEquals(cursor, event.cursor)
            checkPosition(event.position)
            assertEquals(button, event.button)
        }
        usedComponent.log[4].let {
            val event = it as CursorLeaveEvent
            assertEquals(cursor, event.cursor)
            checkPosition(event.position)
        }
    }

    @Test
    fun testProcessCursorMoveEvent() {
        val leftComponent = EventLogComponent(setOf(CursorMoveEvent::class, CursorEnterEvent::class))
        val rightComponent = EventLogComponent(setOf(CursorLeaveEvent::class, CursorMoveEvent::class))
        val cursor = Cursor(3)
        val fullEvent = CursorMoveEvent(cursor, EventPosition(0.7f, 0.25f), EventPosition(0f, 0.25f))

        val agent = ComponentAgent(DummyCursorTracker())
        val menu = SimpleFlatMenu(SpaceLayout.Simple, Color.BLACK)
        menu.initAgent(agent)
        menu.subscribeToEvents()
        assertTrue(agent.isSubscribed(CursorMoveEvent::class))

        menu.addComponent(leftComponent, RectRegion.percentage(10, 0, 40, 30))
        menu.addComponent(rightComponent, RectRegion.percentage(40, 20, 60, 30))

        // The event should be ignored before the menu has been rendered
        menu.processEvent(fullEvent)
        assertTrue(leftComponent.log.isEmpty())
        assertTrue(rightComponent.log.isEmpty())

        menu.render(DummyGraviksTarget(), false)

        fun checkPosition(expected: EventPosition, actual: EventPosition) {
            assertEquals(expected.x, actual.x, 0.05f)
            assertEquals(expected.y, actual.y, 0.05f)
        }

        menu.processEvent(CursorEnterEvent(cursor, fullEvent.oldPosition))
        menu.processEvent(fullEvent)
        assertEquals(2, leftComponent.log.size)
        leftComponent.log[0].let {
            val event = it as CursorEnterEvent
            checkPosition(EventPosition(0.9f, 5f / 6f), event.position)
            assertEquals(cursor, event.cursor)
        }
        leftComponent.log[1].let {
            val event = it as CursorMoveEvent
            checkPosition(EventPosition(0.9f, 5f / 6f), event.oldPosition)
            checkPosition(EventPosition(0.1f, 5f / 6f), event.newPosition)
            assertEquals(cursor, event.cursor)
        }

        assertEquals(2, rightComponent.log.size)
        rightComponent.log[0].let {
            val event = it as CursorMoveEvent
            checkPosition(EventPosition(0.9f, 0.5f), event.oldPosition)
            checkPosition(EventPosition(0.1f, 0.5f), event.newPosition)
            assertEquals(cursor, event.cursor)
        }
        rightComponent.log[1].let {
            val event = it as CursorLeaveEvent
            checkPosition(EventPosition(0.1f, 0.5f), event.position)
            assertEquals(cursor, event.cursor)
        }

        val leftPosition1 = EventPosition(0.25f, 0.25f)
        menu.processEvent(CursorMoveEvent(cursor, fullEvent.newPosition, leftPosition1))
        assertEquals(4, leftComponent.log.size)
        leftComponent.log[2].let {
            val event = it as CursorEnterEvent
            checkPosition(EventPosition(0.1f, 5f / 6f), event.position)
            assertEquals(cursor, event.cursor)
        }
        leftComponent.log[3].let {
            val event = it as CursorMoveEvent
            checkPosition(EventPosition(0.1f, 5f / 6f), event.oldPosition)
            checkPosition(EventPosition(0.5f, 5f / 6f), event.newPosition)
            assertEquals(cursor, event.cursor)
        }

        assertEquals(2, rightComponent.log.size)

        menu.processEvent(CursorMoveEvent(cursor, leftPosition1, EventPosition(0.25f, 0.15f)))
        assertEquals(5, leftComponent.log.size)
        leftComponent.log[4].let {
            val event = it as CursorMoveEvent
            checkPosition(EventPosition(0.5f, 5f / 6f), event.oldPosition)
            checkPosition(EventPosition(0.5f, 0.5f), event.newPosition)
            assertEquals(cursor, event.cursor)
        }
    }

    @Test
    fun testRegionsToRedrawBeforeNextRenderTransparent() {
        val menu = SimpleFlatMenu(SpaceLayout.Simple, Color.TRANSPARENT)
        menu.initAgent(ComponentAgent(DummyCursorTracker()))
        menu.subscribeToEvents()

        // Should be empty when there are no components to be drawn
        assertTrue(menu.regionsToRedrawBeforeNextRender().isEmpty())

        fun checkResults(expected: Set<BackgroundRegion>, actual: Collection<BackgroundRegion>) {
            assertEquals(expected.size, actual.size)
            for (region in expected) {
                assertTrue(actual.any {
                    abs(it.minX - region.minX) + abs(it.minY - region.minY) + abs(it.maxX - region.maxX)
                    + abs(it.maxY - region.maxY) < 0.01f
                })
            }
        }

        // There is nothing to be redrawn until the component is rendered
        menu.addComponent(ClickDrawComponent(), RectRegion.percentage(50, 0, 100, 50))
        assertTrue(menu.regionsToRedrawBeforeNextRender().isEmpty())

        // There is no need to redraw anything until the component wants to be rendered
        menu.render(DummyGraviksTarget(), false)
        assertTrue(menu.regionsToRedrawBeforeNextRender().isEmpty())

        // When the component requests to be redrawn, the menu should request to clear its background
        menu.processEvent(CursorClickEvent(Cursor(2), EventPosition(0.75f, 0.25f), 5))
        checkResults(setOf(BackgroundRegion(0.55f, 0.05f, 0.95f, 0.45f)), menu.regionsToRedrawBeforeNextRender())

        // The new component doesn't need a background clear yet
        menu.addComponent(ClickDrawComponent(), RectRegion.percentage(50, 50, 100, 100))
        checkResults(setOf(BackgroundRegion(0.55f, 0.05f, 0.95f, 0.45f)), menu.regionsToRedrawBeforeNextRender())

        // After rendering, both components should be satisfied
        menu.render(DummyGraviksTarget(), false)
        assertTrue(menu.regionsToRedrawBeforeNextRender().isEmpty())
    }

    @Test
    fun testRegionsToRedrawBeforeNextRenderSolid() {
        val menu = SimpleFlatMenu(SpaceLayout.Simple, Color.WHITE)
        menu.initAgent(ComponentAgent(DummyCursorTracker()))
        menu.subscribeToEvents()

        // When the menu has a solid background, it should never need to redraw the background
        assertTrue(menu.regionsToRedrawBeforeNextRender().isEmpty())

        menu.addComponent(ClickDrawComponent(), RectRegion.percentage(10, 20, 30, 40))
        assertTrue(menu.regionsToRedrawBeforeNextRender().isEmpty())

        menu.render(DummyGraviksTarget(), false)
        assertTrue(menu.regionsToRedrawBeforeNextRender().isEmpty())

        menu.processEvent(CursorClickEvent(Cursor(1), EventPosition(0.2f, 0.3f), 1))
        assertTrue(menu.regionsToRedrawBeforeNextRender().isEmpty())

        menu.render(DummyGraviksTarget(), false)
        assertTrue(menu.regionsToRedrawBeforeNextRender().isEmpty())
    }

    @Test
    fun testRenderSolid() {
        val target = LoggedGraviksTarget()
        val margin = 0.001f

        fun checkRenderResult(result: RenderResult) {
            val renderedRegion = result.drawnRegion as RectangularDrawnRegion
            assertEquals(0f, renderedRegion.minX, margin)
            assertEquals(0f, renderedRegion.minY, margin)
            assertEquals(1f, renderedRegion.maxX, margin)
            assertEquals(1f, renderedRegion.maxY, margin)
        }

        fun checkFillRectCalls(vararg expected: FillRectCall) {
            assertEquals(expected.size, target.fillRectCalls.size)
            for (actual in target.fillRectCalls) {
                assertTrue(expected.contains(actual))
            }
            target.fillRectCalls.clear()
        }

        val backgroundColor = Color.rgbInt(1, 2, 3)
        val menu = SimpleFlatMenu(SpaceLayout.Simple, backgroundColor)
        menu.initAgent(ComponentAgent(DummyCursorTracker()))
        menu.subscribeToEvents()

        menu.addComponent(ClickDrawComponent(), RectRegion.percentage(0, 50, 50, 100))
        checkRenderResult(menu.render(target, false))

        // The menu should have used 1 fillRect call to draw its background and the component should
        // also have called fillRect once
        checkFillRectCalls(
            FillRectCall(0f, 0f, 1f, 1f, backgroundColor),
            FillRectCall(0.05f, 0.55f, 0.45f, 0.95f, Color.RED)
        )

        // If we draw again without forcing, nothing should happen
        checkRenderResult(menu.render(target, false))
        checkFillRectCalls()

        // If we add another component, only that component should be drawn, as well as the requested region behind it
        menu.addComponent(ClickDrawComponent(), RectRegion.percentage(0, 0, 50, 50))
        checkRenderResult(menu.render(target, false))
        checkFillRectCalls(
            FillRectCall(0.05f, 0.05f, 0.45f, 0.45f, backgroundColor),
            FillRectCall(0.05f, 0.05f, 0.45f, 0.45f, Color.RED)
        )

        // If we force a draw, both components and the background should be rendered
        checkRenderResult(menu.render(target, true))
        checkFillRectCalls(
            FillRectCall(0f, 0f, 1f, 1f, backgroundColor),
            FillRectCall(0.05f, 0.05f, 0.45f, 0.45f, Color.RED),
            FillRectCall(0.05f, 0.55f, 0.45f, 0.95f, Color.RED)
        )

        // If we redraw after doing nothing, nothing should be redrawn
        checkRenderResult(menu.render(target, false))
        checkFillRectCalls()

        // If we click one of the components, only that component should be redrawn,
        // but the menu should also clear the region behind it
        menu.processEvent(CursorClickEvent(Cursor(5), EventPosition(0.25f, 0.75f), 3))
        checkRenderResult(menu.render(target, false))
        checkFillRectCalls(
            FillRectCall(0.05f, 0.55f, 0.45f, 0.95f, backgroundColor),
            FillRectCall(0.05f, 0.55f, 0.45f, 0.95f, Color.RED)
        )

        // If we redraw again after doing nothing, nothing should be redrawn
        checkRenderResult(menu.render(target, false))
        checkFillRectCalls()
    }

    @Test
    fun testRenderTransparent() {
        val target = LoggedGraviksTarget()
        val margin = 0.001f

        fun checkRenderResult(result: RenderResult, hasComponent1: Boolean, hasComponent2: Boolean) {
            fun checkRegion1(region: DrawnRegion) {
                assertEquals(0.05f, region.minX, margin)
                assertEquals(0.55f, region.minY, margin)
                assertEquals(0.45f, region.maxX, margin)
                assertEquals(0.95f, region.maxY, margin)
            }
            
            fun checkRegion2(region: DrawnRegion) {
                assertEquals(0.05f, region.minX, margin)
                assertEquals(0.05f, region.minY, margin)
                assertEquals(0.45f, region.maxX, margin)
                assertEquals(0.45f, region.maxY, margin)
            }
            
            if (hasComponent2) {
                val renderedRegion = result.drawnRegion as CompositeDrawnRegion
                assertEquals(2, renderedRegion.regions.size)
                checkRegion1(renderedRegion.regions.sortedBy { it.minY }[1])
                checkRegion2(renderedRegion.regions.sortedBy { it.minY }[0])
            } else if (hasComponent1) {
                checkRegion1(result.drawnRegion!!)
            } else {
                assertNull(result.drawnRegion)
            }
        }

        fun checkFillRectCalls(vararg expected: FillRectCall) {
            assertEquals(expected.size, target.fillRectCalls.size)
            for (actual in target.fillRectCalls) {
                assertTrue(expected.contains(actual))
            }
            target.fillRectCalls.clear()
        }

        val menu = SimpleFlatMenu(SpaceLayout.Simple, Color.TRANSPARENT)
        menu.initAgent(ComponentAgent(DummyCursorTracker()))
        menu.subscribeToEvents()

        checkRenderResult(menu.render(target, false), hasComponent1 = false, false)
        checkFillRectCalls()

        menu.addComponent(ClickDrawComponent(), RectRegion.percentage(0, 50, 50, 100))
        checkRenderResult(menu.render(target, false), hasComponent1 = true, false)

        // The menu should have drawn its only component
        checkFillRectCalls(
            FillRectCall(0.05f, 0.55f, 0.45f, 0.95f, Color.RED)
        )

        // If we draw again without forcing, nothing should happen
        checkRenderResult(menu.render(target, false), hasComponent1 = true, false)
        checkFillRectCalls()

        // If we add another component, only that new component should be drawn
        menu.addComponent(ClickDrawComponent(), RectRegion.percentage(0, 0, 50, 50))
        checkRenderResult(menu.render(target, false), hasComponent1 = true, true)
        checkFillRectCalls(
            FillRectCall(0.05f, 0.05f, 0.45f, 0.45f, Color.RED)
        )

        // If we force a draw, both components should be rendered
        checkRenderResult(menu.render(target, true), hasComponent1 = true, true)
        checkFillRectCalls(
            FillRectCall(0.05f, 0.05f, 0.45f, 0.45f, Color.RED),
            FillRectCall(0.05f, 0.55f, 0.45f, 0.95f, Color.RED)
        )

        // If we redraw after doing nothing, nothing should be redrawn
        checkRenderResult(menu.render(target, false), hasComponent1 = true, true)
        checkFillRectCalls()

        // If we click one of the components, only that component should be redrawn
        menu.processEvent(CursorClickEvent(Cursor(5), EventPosition(0.25f, 0.75f), 3))
        checkRenderResult(menu.render(target, false), hasComponent1 = true, true)
        checkFillRectCalls(
            FillRectCall(0.05f, 0.55f, 0.45f, 0.95f, Color.RED)
        )

        // If we redraw again after doing nothing, nothing should be redrawn
        checkRenderResult(menu.render(target, false), hasComponent1 = true, true)
        checkFillRectCalls()       
    }

    // TODO add more tests for everything once the visibleRegion can be changed

    @Test
    fun testCursorTracker() {
        val cursor1 = Cursor(1)
        val cursor2 = Cursor(2)
        val cursor3 = Cursor(3)

        class FakeCursorTracker: CursorTracker {
            override fun getAllCursors() = listOf(cursor1, cursor2)

            override fun getHoveringCursors() = getAllCursors()

            override fun getCursorState(cursor: Cursor): TrackedCursor? {
                return if (cursor == cursor1) TrackedCursor(EventPosition(0.6f, 0.8f), setOf(5))
                else if (cursor == cursor2) TrackedCursor(EventPosition(0.2f, 0.1f), emptySet())
                else null
            }
        }

        var state = 0

        class CursorCheckComponent: Component() {
            override fun subscribeToEvents() {
                // Don't care about events
            }

            override fun processEvent(event: Event) {
                throw UnsupportedOperationException("Didn't subscribe to any events")
            }

            override fun render(target: GraviksTarget, force: Boolean): RenderResult {
                agent.didRequestRender = true

                assertFalse(agent.cursorTracker.getAllCursors().contains(cursor3))
                assertFalse(agent.cursorTracker.getHoveringCursors().contains(cursor3))
                assertNull(agent.cursorTracker.getCursorState(cursor3))

                assertTrue(agent.cursorTracker.getAllCursors().contains(cursor1))
                assertTrue(agent.cursorTracker.getAllCursors().contains(cursor2))
                assertEquals(TrackedCursor(
                    EventPosition(0.2f, 6f / 7f), setOf(5)
                ), agent.cursorTracker.getCursorState(cursor1))
                assertEquals(TrackedCursor(
                    EventPosition(-0.6f, -1f / 7f), emptySet()
                ), agent.cursorTracker.getCursorState(cursor2))

                if (state == 0) {
                    assertTrue(agent.cursorTracker.getHoveringCursors().isEmpty())
                    state = 1
                } else if (state == 1) {
                    assertEquals(1, agent.cursorTracker.getHoveringCursors().size)
                    assertEquals(cursor1, agent.cursorTracker.getHoveringCursors().first())
                    state = 2
                    return RenderResult(
                        drawnRegion = RectangularDrawnRegion(0f, 0f, 1f, 0.5f),
                        propagateMissedCursorEvents = false
                    )
                } else if (state == 2) {
                    assertTrue(agent.cursorTracker.getHoveringCursors().isEmpty())
                    state = 3
                } else {
                    throw IllegalStateException("Unexpected state $state")
                }

                return RenderResult(
                    drawnRegion = RectangularDrawnRegion(0f, 0f, 1f, 1f),
                    propagateMissedCursorEvents = false
                )
            }
        }

        val menu = SimpleFlatMenu(SpaceLayout.Simple, Color.BLUE)
        menu.initAgent(ComponentAgent(FakeCursorTracker()))
        menu.subscribeToEvents()

        menu.addComponent(CursorCheckComponent(), RectRegion.percentage(50, 20, 100, 90))
        menu.render(DummyGraviksTarget(), false)
        assertEquals(1, state)
        menu.render(DummyGraviksTarget(), false)
        assertEquals(2, state)
        menu.render(DummyGraviksTarget(), false)
        assertEquals(3, state)
    }
}
