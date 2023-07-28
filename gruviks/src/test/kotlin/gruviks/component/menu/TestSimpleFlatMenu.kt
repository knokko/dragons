package gruviks.component.menu

import graviks2d.target.GraviksScissor
import graviks2d.target.GraviksTarget
import graviks2d.util.Color
import gruviks.component.*
import gruviks.component.agent.*
import gruviks.component.fill.SimpleColorFillComponent
import gruviks.component.test.ColorShuffleComponent
import gruviks.event.*
import gruviks.feedback.*
import gruviks.space.Coordinate
import gruviks.space.Point
import gruviks.space.RectRegion
import gruviks.space.SpaceLayout
import gruviks.util.DummyGraviksTarget
import gruviks.util.FillRectCall
import gruviks.util.LoggedGraviksTarget
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.util.*
import kotlin.math.abs

class TestSimpleFlatMenu {

    @Test
    fun testGetVisibleRegionSimple() {
        val menu = SimpleFlatMenu(SpaceLayout.Simple, Color.BLUE)
        menu.initAgent(ComponentAgent(DummyCursorTracker(), DUMMY_FEEDBACK, NO_KEYBOARD_FOCUS))
        menu.subscribeToEvents()

        assertEquals(RectRegion.percentage(0, 0, 100, 100), menu.getVisibleRegion())
        menu.moveCamera(Point.percentage(30, -20))
        assertEquals(RectRegion.percentage(30, -20, 130, 80), menu.getVisibleRegion())
    }

    @Test
    fun testGetVisibleRegionGrowUp() {
        val menu = SimpleFlatMenu(SpaceLayout.GrowUp, Color.BLUE)
        menu.initAgent(ComponentAgent(DummyCursorTracker(), DUMMY_FEEDBACK, NO_KEYBOARD_FOCUS))
        menu.subscribeToEvents()

        // Before rendering, the aspect ratio is assumed to be 1
        assertEquals(RectRegion.percentage(0, 0, 100, 100), menu.getVisibleRegion())
        menu.moveCamera(Point.percentage(30, -20))
        assertEquals(RectRegion.percentage(30, -20, 130, 80), menu.getVisibleRegion())

        menu.render(DummyGraviksTarget(dummyAspectRatio = 2f), false)
        assertEquals(RectRegion.percentage(30, -20, 130, 30), menu.getVisibleRegion())
    }

    @Test
    fun testGetVisibleRegionGrowDown() {
        val menu = SimpleFlatMenu(SpaceLayout.GrowDown, Color.BLUE)
        menu.initAgent(ComponentAgent(DummyCursorTracker(), DUMMY_FEEDBACK, NO_KEYBOARD_FOCUS))
        menu.subscribeToEvents()

        // Before rendering, the aspect ratio is assumed to be 1
        assertEquals(RectRegion.percentage(0, 0, 100, 100), menu.getVisibleRegion())
        menu.moveCamera(Point.percentage(30, -20))
        assertEquals(RectRegion.percentage(30, -20, 130, 80), menu.getVisibleRegion())

        menu.render(DummyGraviksTarget(dummyAspectRatio = 2f), false)
        assertEquals(RectRegion.percentage(30, 30, 130, 80), menu.getVisibleRegion())
    }

    @Test
    fun testGetVisibleRegionGrowRight() {
        val menu = SimpleFlatMenu(SpaceLayout.GrowRight, Color.BLUE)
        menu.initAgent(ComponentAgent(DummyCursorTracker(), DUMMY_FEEDBACK, NO_KEYBOARD_FOCUS))
        menu.subscribeToEvents()

        // Before rendering, the aspect ratio is assumed to be 1
        assertEquals(RectRegion.percentage(0, 0, 100, 100), menu.getVisibleRegion())
        menu.moveCamera(Point.percentage(30, -20))
        assertEquals(RectRegion.percentage(30, -20, 130, 80), menu.getVisibleRegion())

        menu.render(DummyGraviksTarget(dummyAspectRatio = 2f), false)
        assertEquals(RectRegion.percentage(30, -20, 230, 80), menu.getVisibleRegion())
    }

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
        val agent = ComponentAgent(DummyCursorTracker(), DUMMY_FEEDBACK, NO_KEYBOARD_FOCUS)
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

        // Shift the camera such that the same event should touch the 'unused' component instead
        menu.shiftCamera(Coordinate.percentage(-20), Coordinate.percentage(-50))
        menu.processEvent(CursorEnterEvent(cursor, centerPosition))
        menu.processEvent(CursorPressEvent(cursor, centerPosition, button))
        menu.processEvent(CursorReleaseEvent(cursor, centerPosition, button))
        menu.processEvent(CursorClickEvent(cursor, centerPosition, button))
        menu.processEvent(CursorLeaveEvent(cursor, centerPosition))

        // After which both components should have received 5 events
        assertEquals(5, usedComponent.log.size)
        assertEquals(5, unusedComponent.log.size)
    }

    @Test
    fun testProcessCursorMoveEvent() {
        val leftComponent = EventLogComponent(setOf(CursorMoveEvent::class, CursorEnterEvent::class))
        val rightComponent = EventLogComponent(setOf(CursorLeaveEvent::class, CursorMoveEvent::class))
        val cursor = Cursor(3)
        val fullEvent = CursorMoveEvent(cursor, EventPosition(0.7f, 0.25f), EventPosition(0f, 0.25f))

        val agent = ComponentAgent(DummyCursorTracker(), DUMMY_FEEDBACK, NO_KEYBOARD_FOCUS)
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
        assertEquals(0, leftComponent.log.size)

        // Fire a CursorMoveEvent that goes straight through both the right component and the left component
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

        val leftPosition2 = EventPosition(0.25f, 0.15f)
        menu.processEvent(CursorMoveEvent(cursor, leftPosition1, leftPosition2))
        assertEquals(5, leftComponent.log.size)
        leftComponent.log[4].let {
            val event = it as CursorMoveEvent
            checkPosition(EventPosition(0.5f, 5f / 6f), event.oldPosition)
            checkPosition(EventPosition(0.5f, 0.5f), event.newPosition)
            assertEquals(cursor, event.cursor)
        }

        // Shift the camera such that the leftPosition2 (0.25, 0.15) will be inside the right component
        menu.shiftCamera(Coordinate.percentage(20), Coordinate.percentage(10))
        menu.processEvent(CursorMoveEvent(cursor, leftPosition2, EventPosition(0.5f, leftPosition2.y)))

        assertEquals(4, rightComponent.log.size)
        rightComponent.log[2].let {
            val event = it as CursorMoveEvent
            checkPosition(EventPosition(0.25f, 0.5f), event.oldPosition)
            checkPosition(EventPosition(0.9f, 0.5f), event.newPosition)
        }
        rightComponent.log[3].let {
            val event = it as CursorLeaveEvent
            checkPosition(EventPosition(0.9f, 0.5f), event.position)
        }
    }

    @Test
    fun testRegionsToRedrawBeforeNextRenderTransparent() {
        val menu = SimpleFlatMenu(SpaceLayout.Simple, Color.TRANSPARENT)
        menu.initAgent(ComponentAgent(DummyCursorTracker(), DUMMY_FEEDBACK, NO_KEYBOARD_FOCUS))
        menu.subscribeToEvents()

        // Should be empty when there are no components to be drawn
        assertTrue(menu.regionsToRedrawBeforeNextRender().isEmpty())

        fun checkResults(expected: Set<BackgroundRegion>, actual: Collection<BackgroundRegion>) {
            assertEquals(expected.size, actual.size)
            for (region in expected) {
                val equalActual = actual.filter {
                    val absDifference = abs(it.minX - region.minX) + abs(it.minY - region.minY) + abs(it.maxX - region.maxX) +
                            abs(it.maxY - region.maxY)
                    absDifference < 0.0001f
                }
                assertTrue(equalActual.isNotEmpty())
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

        // When we shift the camera, the old regions should be redrawn
        menu.shiftCamera(Coordinate.percentage(40), Coordinate.percentage(30))
        checkResults(setOf(
            BackgroundRegion(0.55f, 0.05f, 0.95f, 0.45f),
            BackgroundRegion(0.55f, 0.55f, 0.95f, 0.95f)
        ), menu.regionsToRedrawBeforeNextRender())

        menu.render(DummyGraviksTarget(), false)
        assertTrue(menu.regionsToRedrawBeforeNextRender().isEmpty())

        // When we shift back, the new regions should be redrawn
        menu.shiftCamera(Coordinate.percentage(-40), Coordinate.percentage(-30))
        checkResults(setOf(
            // Note that the minY of the first region is clamped to 0
            BackgroundRegion(0.15f, 0f, 0.55f, 0.15f),
            BackgroundRegion(0.15f, 0.25f, 0.55f, 0.65f)
        ), menu.regionsToRedrawBeforeNextRender())
    }

    @Test
    fun testRegionsToRedrawBeforeNextRenderSolid() {
        val menu = SimpleFlatMenu(SpaceLayout.Simple, Color.WHITE)
        menu.initAgent(ComponentAgent(DummyCursorTracker(), DUMMY_FEEDBACK, NO_KEYBOARD_FOCUS))
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

        menu.shiftCamera(Coordinate.percentage(12), Coordinate.percentage(34))
        assertTrue(menu.regionsToRedrawBeforeNextRender().isEmpty())
    }

    @Test
    fun testRenderSolid() {
        val target = LoggedGraviksTarget()
        val margin = 0.001f

        fun checkRenderResult(result: RenderResult, checkDrawnRegion: (DrawnRegion?) -> Unit) {
            val renderedRegion = result.drawnRegion as RectangularDrawnRegion
            assertEquals(0f, renderedRegion.minX, margin)
            assertEquals(0f, renderedRegion.minY, margin)
            assertEquals(1f, renderedRegion.maxX, margin)
            assertEquals(1f, renderedRegion.maxY, margin)
            checkDrawnRegion(result.recentDrawnRegion)
        }

        fun checkEntireDrawnRegion(renderedRegion: DrawnRegion?) {
            val rectRegion = renderedRegion as RectangularDrawnRegion
            assertEquals(0f, rectRegion.minX, margin)
            assertEquals(0f, rectRegion.minY, margin)
            assertEquals(1f, rectRegion.maxX, margin)
            assertEquals(1f, rectRegion.maxY, margin)
        }

        fun checkComponent1DrawnRegion(renderedRegion: DrawnRegion?) {
            for (rectRegion in (renderedRegion as CompositeDrawnRegion).regions) {
                assertEquals(0.05f, rectRegion.minX, margin)
                assertEquals(0.55f, rectRegion.minY, margin)
                assertEquals(0.45f, rectRegion.maxX, margin)
                assertEquals(0.95f, rectRegion.maxY, margin)
            }
        }

        fun checkComponent2DrawnRegion(renderedRegion: DrawnRegion?) {
            for (rectRegion in (renderedRegion as CompositeDrawnRegion).regions) {
                assertEquals(0.05f, rectRegion.minX, margin)
                assertEquals(0.05f, rectRegion.minY, margin)
                assertEquals(0.45f, rectRegion.maxX, margin)
                assertEquals(0.45f, rectRegion.maxY, margin)
            }
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
        menu.initAgent(ComponentAgent(DummyCursorTracker(), DUMMY_FEEDBACK, NO_KEYBOARD_FOCUS))
        menu.subscribeToEvents()

        val defaultScissor = GraviksScissor(0f, 0f, 1f, 1f)
        val scissor1 = GraviksScissor(0f, 0.5f, 0.5f, 1f)
        menu.addComponent(ClickDrawComponent(), RectRegion.percentage(0, 50, 50, 100))
        checkRenderResult(menu.render(target, true), ::checkEntireDrawnRegion)

        // The menu should have used 1 fillRect call to draw its background and the component should
        // also have called fillRect once
        checkFillRectCalls(
            FillRectCall(0f, 0f, 1f, 1f, backgroundColor, defaultScissor),
            FillRectCall(0.05f, 0.55f, 0.45f, 0.95f, Color.RED, scissor1)
        )

        // If we draw again without forcing, nothing should happen
        checkRenderResult(menu.render(target, false), ::assertNull)
        checkFillRectCalls()

        // If we add another component, only that component should be drawn, as well as the requested region behind it
        val scissor2 = GraviksScissor(0f, 0f, 0.5f, 0.5f)
        menu.addComponent(ClickDrawComponent(), RectRegion.percentage(0, 0, 50, 50))
        checkRenderResult(menu.render(target, false), ::checkComponent2DrawnRegion)
        checkFillRectCalls(
            FillRectCall(0.05f, 0.05f, 0.45f, 0.45f, backgroundColor, scissor2),
            FillRectCall(0.05f, 0.05f, 0.45f, 0.45f, Color.RED, scissor2)
        )

        // If we force a draw, both components and the background should be rendered
        checkRenderResult(menu.render(target, true), ::checkEntireDrawnRegion)
        checkFillRectCalls(
            FillRectCall(0f, 0f, 1f, 1f, backgroundColor, defaultScissor),
            FillRectCall(0.05f, 0.05f, 0.45f, 0.45f, Color.RED, scissor2),
            FillRectCall(0.05f, 0.55f, 0.45f, 0.95f, Color.RED, scissor1)
        )

        // If we redraw after doing nothing, nothing should be redrawn
        checkRenderResult(menu.render(target, false), ::assertNull)
        checkFillRectCalls()

        // If we click one of the components, only that component should be redrawn,
        // but the menu should also clear the region behind it
        menu.processEvent(CursorClickEvent(Cursor(5), EventPosition(0.25f, 0.75f), 3))
        checkRenderResult(menu.render(target, false), ::checkComponent1DrawnRegion)
        checkFillRectCalls(
            FillRectCall(0.05f, 0.55f, 0.45f, 0.95f, backgroundColor, scissor1),
            FillRectCall(0.05f, 0.55f, 0.45f, 0.95f, Color.RED, scissor1)
        )

        // If we redraw again after doing nothing, nothing should be redrawn
        checkRenderResult(menu.render(target, false), ::assertNull)
        checkFillRectCalls()

        // When we shift the camera, the background should be redrawn, and the components should be drawn
        // at their new relative position
        val shiftedScissor1 = GraviksScissor(
            scissor1.minX - 0.4f, scissor1.minY - 0.2f, scissor1.maxX - 0.4f, scissor1.maxY - 0.2f
        )
        val shiftedScissor2 = GraviksScissor(
            scissor2.minX - 0.4f, scissor2.minY - 0.2f, scissor2.maxX - 0.4f, scissor2.maxY - 0.2f
        )
        menu.shiftCamera(Coordinate.percentage(40), Coordinate.percentage(20))
        checkRenderResult(menu.render(target, false), ::checkEntireDrawnRegion)
        checkFillRectCalls(
            FillRectCall(0f, 0f, 1f, 1f, backgroundColor, defaultScissor),
            FillRectCall(-0.35f, -0.15f, 0.05f, 0.25f, Color.RED, shiftedScissor2),
            FillRectCall(-0.35f, 0.35f, 0.05f, 0.75f, Color.RED, shiftedScissor1)
        )

        // If we render again, nothing should happen
        checkRenderResult(menu.render(target, false), ::assertNull)
        checkFillRectCalls()
    }

    @Test
    fun testRenderTransparent() {
        val target = LoggedGraviksTarget()
        val margin = 0.001f

        fun checkRenderResult(
                result: RenderResult, hasComponent1: Boolean, hasComponent2: Boolean,
                didDrawComponent1: Boolean, didDrawComponent2: Boolean
        ) {
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

            if (didDrawComponent1 && didDrawComponent2) {
                val recentDrawnRegions = (result.recentDrawnRegion as CompositeDrawnRegion).regions.sortedBy { it.minY }
                checkRegion1(recentDrawnRegions[1])
                checkRegion2(recentDrawnRegions[0])
            } else if (didDrawComponent1) {
                checkRegion1(result.recentDrawnRegion as TransformedDrawnRegion)
            } else if (didDrawComponent2) {
                checkRegion2(result.recentDrawnRegion as TransformedDrawnRegion)
            } else {
                assertNull(result.recentDrawnRegion)
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
        menu.initAgent(ComponentAgent(DummyCursorTracker(), DUMMY_FEEDBACK, NO_KEYBOARD_FOCUS))
        menu.subscribeToEvents()

        checkRenderResult(
                menu.render(target, false), hasComponent1 = false, hasComponent2 = false,
                didDrawComponent1 = false, didDrawComponent2 = false
        )
        checkFillRectCalls()

        val scissor1 = GraviksScissor(0f, 0.5f, 0.5f, 1f)
        menu.addComponent(ClickDrawComponent(), RectRegion.percentage(0, 50, 50, 100))
        checkRenderResult(
                menu.render(target, false), hasComponent1 = true, hasComponent2 = false,
                didDrawComponent1 = true, didDrawComponent2 = false
        )

        // The menu should have drawn its only component
        checkFillRectCalls(
            FillRectCall(0.05f, 0.55f, 0.45f, 0.95f, Color.RED, scissor1)
        )

        // If we draw again without forcing, nothing should happen
        checkRenderResult(
                menu.render(target, false), hasComponent1 = true, hasComponent2 = false,
                didDrawComponent1 = false, didDrawComponent2 = false
        )
        checkFillRectCalls()

        // If we add another component, only that new component should be drawn
        val scissor2 = GraviksScissor(0f, 0f, 0.5f, 0.5f)
        menu.addComponent(ClickDrawComponent(), RectRegion.percentage(0, 0, 50, 50))
        checkRenderResult(
                menu.render(target, false), hasComponent1 = true, hasComponent2 = true,
                didDrawComponent1 = false, didDrawComponent2 = true
        )
        checkFillRectCalls(
            FillRectCall(0.05f, 0.05f, 0.45f, 0.45f, Color.RED, scissor2)
        )

        // If we force a draw, both components should be rendered
        checkRenderResult(
                menu.render(target, true), hasComponent1 = true, hasComponent2 = true,
                didDrawComponent1 = true, didDrawComponent2 = true
        )
        checkFillRectCalls(
            FillRectCall(0.05f, 0.05f, 0.45f, 0.45f, Color.RED, scissor2),
            FillRectCall(0.05f, 0.55f, 0.45f, 0.95f, Color.RED, scissor1)
        )

        // If we redraw after doing nothing, nothing should be redrawn
        checkRenderResult(
                menu.render(target, false), hasComponent1 = true, hasComponent2 = true,
                didDrawComponent1 = false, didDrawComponent2 = false
        )
        checkFillRectCalls()

        // If we click one of the components, only that component should be redrawn
        menu.processEvent(CursorClickEvent(Cursor(5), EventPosition(0.25f, 0.75f), 3))
        checkRenderResult(
                menu.render(target, false), hasComponent1 = true, hasComponent2 = true,
                didDrawComponent1 = true, didDrawComponent2 = false
        )
        checkFillRectCalls(
            FillRectCall(0.05f, 0.55f, 0.45f, 0.95f, Color.RED, scissor1)
        )

        // If we redraw again after doing nothing, nothing should be redrawn
        checkRenderResult(
                menu.render(target, false), hasComponent1 = true, hasComponent2 = true,
                didDrawComponent1 = false, didDrawComponent2 = false
        )
        checkFillRectCalls()

        fun checkNewRenderResult(result: RenderResult, didDrawComponents: Boolean) {

            fun checkShiftedRegion1(region: DrawnRegion) {
                assertEquals(-0.35f, region.minX, margin)
                assertEquals(0.35f, region.minY, margin)
                assertEquals(0.05f, region.maxX, margin)
                assertEquals(0.75f, region.maxY, margin)
            }

            fun checkShiftedRegion2(region: DrawnRegion) {
                assertEquals(-0.35f, region.minX, margin)
                assertEquals(-0.15f, region.minY, margin)
                assertEquals(0.05f, region.maxX, margin)
                assertEquals(0.25f, region.maxY, margin)
            }

            (result.drawnRegion as CompositeDrawnRegion).regions.sortedBy { it.minY }[1].let(::checkShiftedRegion1)

            (result.drawnRegion as CompositeDrawnRegion).regions.sortedBy { it.minY }[0].let(::checkShiftedRegion2)

            if (didDrawComponents) {
                val drawnRegions = (result.recentDrawnRegion as CompositeDrawnRegion).regions.sortedBy { it.minY }
                drawnRegions[1].let(::checkShiftedRegion1)
                drawnRegions[0].let(::checkShiftedRegion2)
            } else {
                assertNull(result.recentDrawnRegion)
            }
        }

        // When we shift the camera, the components should be drawn at their new position
        val shiftedScissor1 = GraviksScissor(
            scissor1.minX - 0.4f, scissor1.minY - 0.2f, scissor1.maxX - 0.4f, scissor1.maxY - 0.2f
        )
        val shiftedScissor2 = GraviksScissor(
            scissor2.minX - 0.4f, scissor2.minY - 0.2f, scissor2.maxX - 0.4f, scissor2.maxY - 0.2f
        )
        menu.shiftCamera(Coordinate.percentage(40), Coordinate.percentage(20))
        checkNewRenderResult(menu.render(target, false), true)
        checkFillRectCalls(
            FillRectCall(-0.35f, -0.15f, 0.05f, 0.25f, Color.RED, shiftedScissor2),
            FillRectCall(-0.35f, 0.35f, 0.05f, 0.75f, Color.RED, shiftedScissor1)
        )

        // If we render again, nothing should happen
        checkNewRenderResult(menu.render(target, false), false)
        checkFillRectCalls()
    }

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
                agent.giveFeedback(RenderFeedback())

                assertFalse(agent.cursorTracker.getAllCursors().contains(cursor3))
                assertFalse(agent.cursorTracker.getHoveringCursors().contains(cursor3))
                assertNull(agent.cursorTracker.getCursorState(cursor3))

                assertTrue(agent.cursorTracker.getAllCursors().contains(cursor1))
                assertTrue(agent.cursorTracker.getAllCursors().contains(cursor2))
                if (state != 3) {
                    assertEquals(
                        TrackedCursor(
                            EventPosition(0.2f, 6f / 7f), setOf(5)
                        ), agent.cursorTracker.getCursorState(cursor1)
                    )
                    assertEquals(
                        TrackedCursor(
                            EventPosition(-0.6f, -1f / 7f), emptySet()
                        ), agent.cursorTracker.getCursorState(cursor2)
                    )
                }

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
                } else if (state == 3) {
                    assertEquals(TrackedCursor(
                        EventPosition(0.4f, 0.4f / 0.7f - 1f / 7f), emptySet()
                    ), agent.cursorTracker.getCursorState(cursor2))
                    state = 4
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
        menu.initAgent(ComponentAgent(FakeCursorTracker(), DUMMY_FEEDBACK, NO_KEYBOARD_FOCUS))
        menu.subscribeToEvents()

        menu.addComponent(CursorCheckComponent(), RectRegion.percentage(50, 20, 100, 90))
        menu.render(DummyGraviksTarget(), false)
        assertEquals(1, state)
        menu.render(DummyGraviksTarget(), false)
        assertEquals(2, state)
        menu.render(DummyGraviksTarget(), false)
        assertEquals(3, state)
        menu.shiftCamera(Coordinate.percentage(50), Coordinate.percentage(40))
        menu.render(DummyGraviksTarget(), false)
        assertEquals(4, state)
    }

    @Test
    fun testRenderFeedback() {
        val feedbackList = mutableListOf<Feedback>()

        val menu = SimpleFlatMenu(SpaceLayout.Simple, Color.RED)
        menu.addComponent(ClickDrawComponent(), RectRegion.percentage(0, 0, 10, 10))
        menu.initAgent(ComponentAgent(DummyCursorTracker(), feedbackList::add, NO_KEYBOARD_FOCUS))
        menu.subscribeToEvents()

        assertTrue(feedbackList.isEmpty())
        menu.addComponent(ClickDrawComponent(), RectRegion.percentage(20, 20, 80, 80))
        assertEquals(1, feedbackList.size)
        assertTrue(feedbackList[0] is RenderFeedback)
        feedbackList.clear()

        menu.render(DummyGraviksTarget(), false)
        assertTrue(feedbackList.isEmpty())

        menu.processEvent(CursorClickEvent(Cursor(1), EventPosition(0.5f, 0.5f), 1))
        assertEquals(1, feedbackList.size)
        assertTrue(feedbackList[0] is RenderFeedback)
    }

    @Test
    fun testAddressedFeedback() {
        val feedbackList = mutableListOf<Feedback>()

        val menu = SimpleFlatMenu(SpaceLayout.Simple, Color.BLUE)
        menu.initAgent(ComponentAgent(DummyCursorTracker(), feedbackList::add, NO_KEYBOARD_FOCUS))
        menu.subscribeToEvents()

        class TestComponent : Component() {
            override fun subscribeToEvents() {
                agent.subscribe(CursorClickEvent::class)
            }

            override fun processEvent(event: Event) {
                agent.giveFeedback(AddressedFeedback(null, RenderFeedback()))
                agent.giveFeedback(AddressedFeedback(UUID.randomUUID(), RenderFeedback()))
                agent.giveFeedback(AddressedFeedback(menu.id, AddressedFeedback(null, RenderFeedback())))
            }

            override fun render(target: GraviksTarget, force: Boolean) = RenderResult(
                drawnRegion = RectangularDrawnRegion(0f, 0f, 1f, 1f),
                propagateMissedCursorEvents = false
            )
        }

        menu.addComponent(TestComponent(), RectRegion.percentage(0, 0, 100, 100))
        assertEquals(1, feedbackList.size)
        assertTrue(feedbackList[0] is RenderFeedback)
        feedbackList.clear()
        menu.render(DummyGraviksTarget(), false)
        assertTrue(feedbackList.isEmpty())

        menu.processEvent(CursorClickEvent(Cursor(0), EventPosition(0.1f, 0.2f), 3))
        assertEquals(3, feedbackList.size)

        // This feedback was directly addressed to the window
        assertNull((feedbackList[0] as AddressedFeedback).targetID)
        assertTrue((feedbackList[0] as AddressedFeedback).targetFeedback is RenderFeedback)

        // This feedback was addressed to a non-existing component
        assertNotNull((feedbackList[1] as AddressedFeedback).targetID)
        assertTrue((feedbackList[1] as AddressedFeedback).targetFeedback is RenderFeedback)

        // This feedback was indirectly addressed to the window
        assertNull((feedbackList[2] as AddressedFeedback).targetID)
        assertTrue((feedbackList[2] as AddressedFeedback).targetFeedback is RenderFeedback)
    }

    @Test
    fun testCameraFeedback() {
        val menu = SimpleFlatMenu(SpaceLayout.Simple, Color.RED)
        menu.initAgent(ComponentAgent(DummyCursorTracker(), DUMMY_FEEDBACK, NO_KEYBOARD_FOCUS))
        menu.subscribeToEvents()

        class TestComponent : Component() {
            override fun subscribeToEvents() {
                agent.subscribe(CursorClickEvent::class)
            }

            override fun processEvent(event: Event) {
                menu.moveCamera(Point.percentage(20, 10))
                menu.shiftCamera(Coordinate.percentage(50), Coordinate.percentage(60))
            }

            override fun render(target: GraviksTarget, force: Boolean) = RenderResult(
                drawnRegion = RectangularDrawnRegion(0f, 0f, 1f, 1f),
                propagateMissedCursorEvents = false
            )
        }

        menu.addComponent(TestComponent(), RectRegion.percentage(0, 0, 100, 100))
        menu.render(DummyGraviksTarget(), false)
        assertEquals(RectRegion.percentage(0, 0, 100, 100), menu.getVisibleRegion())
        menu.processEvent(CursorClickEvent(Cursor(1), EventPosition(0.2f, 0.3f), 4))
        assertEquals(RectRegion.percentage(70, 70, 170, 170), menu.getVisibleRegion())
    }

    @Test
    fun testReplaceYouFeedback() {
        val feedbackList = mutableListOf<Feedback>()

        val menu = SimpleFlatMenu(SpaceLayout.Simple, Color.GREEN)
        menu.initAgent(ComponentAgent(DummyCursorTracker(), feedbackList::add, NO_KEYBOARD_FOCUS))
        menu.subscribeToEvents()

        class TestComponent : Component() {
            override fun subscribeToEvents() {
                agent.subscribe(CursorClickEvent::class)
            }

            override fun processEvent(event: Event) {
                agent.giveFeedback(ReplaceYouFeedback { ColorShuffleComponent() })
            }

            override fun render(target: GraviksTarget, force: Boolean) = RenderResult(
                drawnRegion = RectangularDrawnRegion(0f, 0f, 1f, 1f),
                propagateMissedCursorEvents = false
            )
        }

        menu.addComponent(TestComponent(), RectRegion.percentage(50, 50, 100, 100))
        menu.render(DummyGraviksTarget(), false)
        feedbackList.clear()
        menu.processEvent(CursorClickEvent(Cursor(0), EventPosition(0.7f, 0.9f), 5))

        assertEquals(1, feedbackList.size)
        assertTrue(feedbackList[0] is ReplaceMeFeedback)
    }

    @Test
    fun testKeyboardFeedback() {
        class KeyComponent : Component() {

            var currentText = ""
            var acquireCounter = 0
            var lostCounter = 0
            var rejectedCounter = 0

            override fun subscribeToEvents() {
                agent.subscribe(CursorClickEvent::class)
                agent.subscribe(KeyTypeEvent::class)
                agent.subscribe(KeyPressEvent::class)
                agent.subscribe(KeyReleaseEvent::class)
                agent.subscribe(KeyboardFocusAcquiredEvent::class)
                agent.subscribe(KeyboardFocusLostEvent::class)
                agent.subscribe(KeyboardFocusRejectedEvent::class)
            }

            override fun processEvent(event: Event) {
                if (event is CursorClickEvent) agent.giveFeedback(RequestKeyboardFocusFeedback())
                else if (event is KeyTypeEvent) currentText += event.codePoint.toChar()
                else if (event is KeyPressEvent && event.key.type == KeyType.Escape) agent.giveFeedback(ReleaseKeyboardFocusFeedback())
                else if (event is KeyboardFocusAcquiredEvent) acquireCounter += 1
                else if (event is KeyboardFocusLostEvent) lostCounter += 1
                else if (event is KeyboardFocusRejectedEvent) rejectedCounter += 1
                else throw UnsupportedOperationException("Unexpected event $event")
            }

            override fun render(target: GraviksTarget, force: Boolean) = RenderResult(drawnRegion = RectangularDrawnRegion(
                0f, 0f, 1f, 1f
            ), propagateMissedCursorEvents = true)
        }

        var menuHasFocus = false
        val feedback = mutableListOf<Feedback>()
        val menu = SimpleFlatMenu(SpaceLayout.Simple, Color.BLACK)
        menu.initAgent(ComponentAgent(DummyCursorTracker(), feedback::add) { menuHasFocus })

        val clickEvent = CursorClickEvent(Cursor(1), EventPosition(0.5f, 0.5f), 2)

        val component = KeyComponent()
        menu.addComponent(component, RectRegion.percentage(10, 20, 80, 90))
        menu.render(DummyGraviksTarget(), true)
        feedback.clear()

        // Reject the first focus request
        run {
            menu.processEvent(clickEvent)
            assertEquals(0, component.rejectedCounter)

            assertEquals(1, feedback.size)
            assertTrue(feedback.removeLast() is RequestKeyboardFocusFeedback)
            menu.processEvent(KeyboardFocusRejectedEvent())

            assertEquals("", component.currentText)
            assertEquals(0, component.acquireCounter)
            assertEquals(0, component.lostCounter)
            assertEquals(1, component.rejectedCounter)
        }

        // Accept the second focus request
        run {
            menu.processEvent(clickEvent)
            assertEquals(1, component.rejectedCounter)

            assertEquals(1, feedback.size)
            assertTrue(feedback.removeLast() is RequestKeyboardFocusFeedback)
            menuHasFocus = true
            menu.processEvent(KeyboardFocusAcquiredEvent())

            assertEquals("", component.currentText)
            assertEquals(1, component.acquireCounter)
            assertEquals(0, component.lostCounter)
            assertEquals(1, component.rejectedCounter)
        }

        // Type hi
        menu.processEvent(KeyTypeEvent('H'.code))
        assertEquals("H", component.currentText)
        assertEquals(1, component.acquireCounter)
        assertEquals(0, component.lostCounter)
        assertEquals(1, component.rejectedCounter)
        menu.processEvent(KeyTypeEvent('i'.code))
        assertEquals("Hi", component.currentText)

        // Press the escape button to lose focus
        menu.processEvent(KeyPressEvent(Key(0, KeyType.Escape), false))
        assertEquals("Hi", component.currentText)
        assertEquals(1, component.acquireCounter)
        assertEquals(1, component.lostCounter)
        assertEquals(1, component.rejectedCounter)

        // The next character should be ignored because focus is lost
        menu.processEvent(KeyTypeEvent('n'.code))
        assertEquals("Hi", component.currentText)

        // Click on the component to regain focus
        menu.processEvent(clickEvent)
        menu.processEvent(KeyTypeEvent('i'.code))
        assertEquals("Hii", component.currentText)
        assertEquals(2, component.acquireCounter)
        assertEquals(1, component.lostCounter)
        assertEquals(1, component.rejectedCounter)

        // Take focus away from the menu
        menu.processEvent(KeyboardFocusLostEvent())
        assertEquals(2, component.lostCounter)
        assertEquals(1, component.rejectedCounter)
    }

    @Test
    fun testKeyboardFeedbackSpam() {
        class KeyboardSpamComponent : Component() {

            var keyPressCounter = 0
            var focusLostCounter = 0
            var focusRejectedCounter = 0
            var focusAcquireCounter = 0

            override fun subscribeToEvents() {
                agent.subscribe(KeyPressEvent::class)
                agent.subscribe(CursorClickEvent::class)
                agent.subscribe(KeyboardFocusAcquiredEvent::class)
                agent.subscribe(KeyboardFocusLostEvent::class)
                agent.subscribe(KeyboardFocusRejectedEvent::class)
            }

            override fun processEvent(event: Event) {
                if (event is KeyPressEvent) keyPressCounter += 1
                if (event is KeyboardFocusLostEvent) focusLostCounter += 1
                if (event is KeyboardFocusRejectedEvent) focusRejectedCounter += 1
                if (event is KeyboardFocusAcquiredEvent) focusAcquireCounter += 1

                if (event is CursorClickEvent || event is KeyboardFocusLostEvent || event is KeyboardFocusAcquiredEvent) {
                    agent.giveFeedback(RequestKeyboardFocusFeedback())
                }
            }

            override fun render(target: GraviksTarget, force: Boolean) = RenderResult(drawnRegion = RectangularDrawnRegion(
                0f, 0f, 1f, 1f
            ), propagateMissedCursorEvents = false)

        }

        class OtherComponent : Component() {

            var keyPressCounter = 0

            override fun subscribeToEvents() {
                agent.subscribe(CursorClickEvent::class)
                agent.subscribe(KeyPressEvent::class)
            }

            override fun processEvent(event: Event) {
                if (event is KeyPressEvent) keyPressCounter += 1
                else agent.giveFeedback(RequestKeyboardFocusFeedback())
            }

            override fun render(target: GraviksTarget, force: Boolean) = RenderResult(drawnRegion = RectangularDrawnRegion(
                0f, 0f, 1f, 1f
            ), propagateMissedCursorEvents = false)
        }

        val menu = SimpleFlatMenu(SpaceLayout.Simple, Color.WHITE)
        val spamComponent = KeyboardSpamComponent()
        menu.initAgent(ComponentAgent(DummyCursorTracker(), DUMMY_FEEDBACK) { true })
        val otherComponent = OtherComponent()
        menu.addComponent(spamComponent, RectRegion.percentage(0, 0, 50, 50))
        menu.addComponent(otherComponent, RectRegion.percentage(50, 50, 100, 100))
        menu.render(DummyGraviksTarget(), true)

        assertEquals(0, spamComponent.keyPressCounter)
        assertEquals(0, spamComponent.focusLostCounter)
        assertEquals(0, spamComponent.focusRejectedCounter)
        assertEquals(0, spamComponent.focusAcquireCounter)

        assertEquals(0, otherComponent.keyPressCounter)

        menu.processEvent(CursorClickEvent(Cursor(0), EventPosition(0.25f, 0.25f), 0))
        assertEquals(1, spamComponent.focusAcquireCounter)
        menu.processEvent(KeyPressEvent(Key(1, KeyType.Enter), false))
        assertEquals(1, spamComponent.keyPressCounter)
        assertEquals(0, spamComponent.focusLostCounter)
        assertEquals(0, spamComponent.focusRejectedCounter)
        assertEquals(0, otherComponent.keyPressCounter)

        menu.processEvent(CursorClickEvent(Cursor(0), EventPosition(0.75f, 0.75f), 1))
        assertEquals(1, spamComponent.keyPressCounter)
        assertEquals(1, spamComponent.focusLostCounter)
        assertEquals(1, spamComponent.focusRejectedCounter)
        assertEquals(1, spamComponent.focusAcquireCounter)
        menu.processEvent(KeyPressEvent(Key(1, KeyType.Enter), false))
        assertEquals(1, spamComponent.keyPressCounter)
        assertEquals(1, otherComponent.keyPressCounter)
    }

    @Test
    fun testPropagateUpdateEvents() {
        class ShouldUpdateComponent : Component() {

            var receivedUpdate = false

            override fun subscribeToEvents() {
                agent.subscribe(UpdateEvent::class)
            }

            override fun processEvent(event: Event) {
                assertTrue(event is UpdateEvent)
                receivedUpdate = true
            }

            override fun render(target: GraviksTarget, force: Boolean): RenderResult {
                return RenderResult(
                        drawnRegion = RectangularDrawnRegion(0f, 0f, 1f, 1f),
                        propagateMissedCursorEvents = false
                )
            }
        }

        class ShouldNotUpdateComponent : Component() {
            override fun subscribeToEvents() {}

            override fun processEvent(event: Event) {
                fail("This component shouldn't get any events")
            }

            override fun render(target: GraviksTarget, force: Boolean): RenderResult {
                return RenderResult(
                        drawnRegion = RectangularDrawnRegion(0f, 0f, 1f, 1f),
                        propagateMissedCursorEvents = false
                )
            }
        }

        val menu = SimpleFlatMenu(SpaceLayout.Simple, Color.BLACK)
        menu.initAgent(ComponentAgent(DummyCursorTracker(), DUMMY_FEEDBACK) { false })
        menu.subscribeToEvents()

        val updateComponent = ShouldUpdateComponent()
        menu.addComponent(updateComponent, RectRegion.percentage(10, 20, 30, 40))
        menu.addComponent(ShouldNotUpdateComponent(), RectRegion.percentage(60, 70, 80, 90))

        menu.processEvent(UpdateEvent())
        assertTrue(updateComponent.receivedUpdate)
    }

    @Test
    fun testPropagateRemoveEvent() {

        var wasRemoved = false
        class RemoveListener : Component() {
            override fun subscribeToEvents() {
                agent.subscribe(RemoveEvent::class)
            }

            override fun processEvent(event: Event) {
                assertTrue(event is RemoveEvent)
                wasRemoved = true
            }

            override fun render(target: GraviksTarget, force: Boolean): RenderResult {
                throw UnsupportedOperationException()
            }
        }

        val menu = SimpleFlatMenu(SpaceLayout.Simple, Color.BLACK)
        menu.initAgent(ComponentAgent(DummyCursorTracker(), DUMMY_FEEDBACK) { false })
        menu.subscribeToEvents()
        menu.addComponent(RemoveListener(), RectRegion.percentage(10, 20, 30, 40))
        menu.addComponent(SimpleColorFillComponent(Color.BLUE), RectRegion.percentage(50, 60, 70, 80))

        menu.processEvent(RemoveEvent())
        assertTrue(wasRemoved)
    }

    @Test
    fun testRemoveComponent() {
        var removeCounter = 0

        class TestComponent(
                val shouldListenRemove: Boolean,
                val onClick: () -> Unit,
                val onRender: () -> Unit
        ) : Component() {
            override fun subscribeToEvents() {
                agent.subscribe(CursorClickEvent::class)
                if (shouldListenRemove) agent.subscribe(RemoveEvent::class)
            }

            override fun processEvent(event: Event) {
                if (event is CursorClickEvent) onClick()
                else if (shouldListenRemove && event is RemoveEvent) removeCounter += 1
                else throw UnsupportedOperationException("Event is $event")
            }

            override fun render(target: GraviksTarget, force: Boolean): RenderResult {
                target.fillRect(0f, 0f, 1f, 1f, Color.GREEN)
                onRender()
                return RenderResult(
                        drawnRegion = RectangularDrawnRegion(0f, 0f, 1f, 1f),
                        propagateMissedCursorEvents = false
                )
            }

        }

        var clickCounter1 = 0
        var renderCounter1 = 0
        var clickCounter2 = 0
        var renderCounter2 = 0
        val target = DummyGraviksTarget()

        val menu = SimpleFlatMenu(SpaceLayout.Simple, Color.RED)
        menu.initAgent(ComponentAgent(DummyCursorTracker(), DUMMY_FEEDBACK) { false })
        menu.subscribeToEvents()

        val component1 = menu.addComponent(
                TestComponent(true, { clickCounter1 += 1 }, { renderCounter1 += 1 }),
                RectRegion.percentage(10, 20, 30, 40)
        )
        val component2 = menu.addComponent(
                TestComponent(false, { clickCounter2 += 1 }, { renderCounter2 += 1 }),
                RectRegion.percentage(50, 50, 70, 90)
        )
        assertTrue(menu.regionsToRedrawBeforeNextRender().isEmpty())
        val renderResult1 = menu.render(target, true)
        assertEquals(1, renderCounter1)
        assertEquals(1, renderCounter2)
        assertEquals(3, target.fillRectCounter)

        val entireRegion = RectangularDrawnRegion(0f, 0f, 1f, 1f)
        assertEquals(entireRegion, renderResult1.drawnRegion)
        assertEquals(entireRegion, renderResult1.recentDrawnRegion)

        assertEquals(0, clickCounter1)
        menu.processEvent(CursorClickEvent(Cursor(0), EventPosition(0.25f, 0.25f), 0))
        assertEquals(1, clickCounter1)

        assertEquals(0, removeCounter)
        menu.removeComponent(component1)
        menu.processEvent(UpdateEvent())
        assertEquals(1, removeCounter)

        menu.processEvent(CursorClickEvent(Cursor(0), EventPosition(0.25f, 0.25f), 0))
        assertEquals(1, clickCounter1)
        assertEquals(0, clickCounter2)
        menu.processEvent(CursorClickEvent(Cursor(0), EventPosition(0.6f, 0.6f), 0))
        assertEquals(1, clickCounter2)

        assertTrue(menu.regionsToRedrawBeforeNextRender().isEmpty())
        val renderResult2 = menu.render(target, false)
        assertEquals(1, renderCounter1)
        assertEquals(1, renderCounter2)

        // 3 for the previous render + 1 fillRect to overwrite component1 with the background color
        assertEquals(4, target.fillRectCounter)

        assertEquals(entireRegion, renderResult2.drawnRegion)
        assertEquals(RectangularDrawnRegion(0.1f, 0.2f, 0.3f, 0.4f), renderResult2.recentDrawnRegion)

        println("renderResult1 is $renderResult1")
        println("renderResult2 is $renderResult2")

        menu.removeComponent(component2)
        assertEquals(1, removeCounter)
        menu.processEvent(CursorClickEvent(Cursor(0), EventPosition(0.6f, 0.6f), 0))
        assertEquals(1, clickCounter2)

        assertTrue(menu.regionsToRedrawBeforeNextRender().isEmpty())
        val renderResult3 = menu.render(target, false)
        assertEquals(1, renderCounter2)
        assertEquals(5, target.fillRectCounter) // It should have overwritten component2
        assertEquals(entireRegion, renderResult3.drawnRegion)
        assertEquals(RectangularDrawnRegion(0.5f, 0.5f, 0.7f, 0.9f), renderResult3.recentDrawnRegion)

        assertTrue(menu.regionsToRedrawBeforeNextRender().isEmpty())
        val renderResult4 = menu.render(target, false)
        assertEquals(5, target.fillRectCounter)
        assertEquals(entireRegion, renderResult4.drawnRegion)
        assertNull(renderResult4.recentDrawnRegion)
    }

    @Test
    fun testRemoveComponentFromTranslucentMenu() {
        val menu = SimpleFlatMenu(SpaceLayout.Simple, Color.rgbaInt(200, 0, 0, 100))
        menu.initAgent(ComponentAgent(DummyCursorTracker(), DUMMY_FEEDBACK) { false })
        menu.subscribeToEvents()

        val target = DummyGraviksTarget()
        val entireRegion = RectangularDrawnRegion(0f, 0f, 1f, 1f)
        val componentRegion = RectangularDrawnRegion(0.5f, 0.5f, 1f, 1f)

        val component = menu.addComponent(
                SimpleColorFillComponent(Color.GREEN),
                RectRegion.percentage(50, 50, 100, 100)
        )
        assertTrue(menu.regionsToRedrawBeforeNextRender().isEmpty())
        val renderResult1 = menu.render(target, false)
        assertEquals(2, target.fillRectCounter)
        assertEquals(entireRegion, renderResult1.drawnRegion)
        assertEquals(entireRegion, renderResult1.recentDrawnRegion)

        menu.removeComponent(component)

        val redrawRegions2 = menu.regionsToRedrawBeforeNextRender()
        assertEquals(1, redrawRegions2.size)
        assertEquals(BackgroundRegion(0.5f, 0.5f, 1f, 1f), redrawRegions2.iterator().next())
        val renderResult2 = menu.render(target, false)
        assertEquals(3, target.fillRectCounter)
        assertEquals(entireRegion, renderResult2.drawnRegion)
        assertEquals(componentRegion, renderResult2.recentDrawnRegion)

        assertTrue(menu.regionsToRedrawBeforeNextRender().isEmpty())
        val renderResult3 = menu.render(target, false)
        assertEquals(3, target.fillRectCounter)
        assertEquals(entireRegion, renderResult3.drawnRegion)
        assertNull(renderResult3.recentDrawnRegion)
    }

    @Test
    fun testRemoveComponentFromTransparentMenu() {
        val menu = SimpleFlatMenu(SpaceLayout.Simple, Color.TRANSPARENT)
        menu.initAgent(ComponentAgent(DummyCursorTracker(), DUMMY_FEEDBACK) { false })
        menu.subscribeToEvents()

        val target = DummyGraviksTarget()
        val transformedRegion = TransformedDrawnRegion(
                region = RectangularDrawnRegion(0f, 0f, 1f, 1f),
                0.5f, 0.5f, 1f, 1f
        )

        val component = menu.addComponent(
                SimpleColorFillComponent(Color.GREEN),
                RectRegion.percentage(50, 50, 100, 100)
        )
        assertTrue(menu.regionsToRedrawBeforeNextRender().isEmpty())
        val renderResult1 = menu.render(target, false)
        assertEquals(1, target.fillRectCounter)
        assertEquals(transformedRegion, renderResult1.drawnRegion)
        assertEquals(transformedRegion, renderResult1.recentDrawnRegion)

        menu.removeComponent(component)

        val redrawRegions2 = menu.regionsToRedrawBeforeNextRender()
        assertEquals(1, redrawRegions2.size)
        assertEquals(BackgroundRegion(0.5f, 0.5f, 1f, 1f), redrawRegions2.iterator().next())
        val renderResult2 = menu.render(target, false)
        assertEquals(1, target.fillRectCounter)
        assertNull(renderResult2.drawnRegion)
        assertNull(renderResult2.recentDrawnRegion)

        assertTrue(menu.regionsToRedrawBeforeNextRender().isEmpty())
        val renderResult3 = menu.render(target, false)
        assertEquals(1, target.fillRectCounter)
        assertNull(renderResult2.drawnRegion)
        assertNull(renderResult3.recentDrawnRegion)
    }
}
