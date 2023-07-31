package gruviks.component.menu.controller

import graviks2d.resource.text.TextStyle
import graviks2d.util.Color
import gruviks.component.agent.ComponentAgent
import gruviks.component.agent.DUMMY_FEEDBACK
import gruviks.component.agent.DummyCursorTracker
import gruviks.component.menu.SimpleFlatMenu
import gruviks.component.text.TextComponent
import gruviks.event.UpdateEvent
import gruviks.space.Coordinate
import gruviks.space.Point
import gruviks.space.RectRegion
import gruviks.space.SpaceLayout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TestSimpleListViewController {

    @Test
    fun testWithSingleElement() {
        val style = TextStyle(fillColor = Color.BLACK, font = null)
        var counter = 0

        val menu = SimpleFlatMenu(SpaceLayout.Simple, Color.GREEN)
        menu.initAgent(ComponentAgent(DummyCursorTracker(), DUMMY_FEEDBACK) { false })

        val elements = mutableListOf("Hello")
        val controller = SimpleListViewController(elements) { element, _, position, components, _ ->
            assertEquals(counter, 0)
            assertEquals(element, "Hello")
            counter += 1

            val componentPosition = position ?: Point.percentage(20, 80)
            components.add(Pair(TextComponent(element, style), RectRegion(
                    componentPosition.x, componentPosition.y,
                    componentPosition.x + Coordinate.percentage(40),
                    componentPosition.y + Coordinate.percentage(10)
            )))
            Point(componentPosition.x, componentPosition.y + Coordinate.percentage(15))
        }
        menu.addController(controller)

        // Before the first update, no components should have been added yet
        assertTrue(menu.getComponentIDs().isEmpty())

        // Updating the menu should cause the controller to update, which should add a single text component
        menu.processEvent(UpdateEvent())
        assertEquals(counter, 1)
        assertEquals(1, menu.getComponentIDs().size)

        // Removing the element should not change the menu until the controller is refreshed
        assertTrue(elements.remove("Hello"))
        menu.processEvent(UpdateEvent())
        assertEquals(1, menu.getComponentIDs().size)

        controller.refresh()
        menu.processEvent(UpdateEvent())
        assertTrue(menu.getComponentIDs().isEmpty())
    }

    @Test
    fun testReplaceMiddleElementWithEqualSizes() {
        val style = TextStyle(fillColor = Color.BLACK, font = null)

        val menu = SimpleFlatMenu(SpaceLayout.Simple, Color.GREEN)
        menu.initAgent(ComponentAgent(DummyCursorTracker(), DUMMY_FEEDBACK) { false })

        val elements = mutableListOf("This", "unit", "test", "will", "replace", "THIS", "element", "with", "dragon")
        val controller = SimpleListViewController(elements) { element, _, position, components, _ ->

            val componentPosition = position ?: Point.percentage(20, 80)
            components.add(Pair(TextComponent(element, style), RectRegion(
                    componentPosition.x, componentPosition.y,
                    componentPosition.x + Coordinate.percentage(40),
                    componentPosition.y + Coordinate.percentage(10)
            )))
            Point(componentPosition.x, componentPosition.y + Coordinate.percentage(15))
        }
        menu.addController(controller)

        menu.processEvent(UpdateEvent())
        val oldIDs = menu.getComponentIDs()
        assertEquals(9, oldIDs.size)

        elements[5] = "dragon"
        controller.refresh()
        menu.processEvent(UpdateEvent())
        val newIDs = menu.getComponentIDs()
        assertEquals(9, newIDs.size)

        // The other 8 elements should NOT have been replaced
        assertEquals(8, oldIDs.intersect(newIDs).size)
        assertEquals(10, oldIDs.union(newIDs).size)
    }

    @Test
    fun testReplaceMiddleElementWithDifferentSizes() {
        val style = TextStyle(fillColor = Color.BLACK, font = null)

        val menu = SimpleFlatMenu(SpaceLayout.Simple, Color.GREEN)
        menu.initAgent(ComponentAgent(DummyCursorTracker(), DUMMY_FEEDBACK) { false })

        val elements = mutableListOf("This", "unit", "test", "will", "replace", "THIS", "element", "with", "dragon")
        val controller = SimpleListViewController(elements) { element, index, position, components, _ ->

            val componentPosition = position ?: Point.percentage(20, 80)
            components.add(Pair(TextComponent(element, style), RectRegion(
                    componentPosition.x, componentPosition.y,
                    componentPosition.x + Coordinate.percentage(40),
                    componentPosition.y + Coordinate.percentage(10)
            )))
            if (index == 5 && element == "dragon") Point(componentPosition.x, componentPosition.y + Coordinate.percentage(16))
            else Point(componentPosition.x, componentPosition.y + Coordinate.percentage(15))
        }
        menu.addController(controller)

        menu.processEvent(UpdateEvent())
        val oldIDs = menu.getComponentIDs()
        assertEquals(9, oldIDs.size)

        elements[5] = "dragon"
        controller.refresh()
        menu.processEvent(UpdateEvent())
        val newIDs = menu.getComponentIDs()
        assertEquals(9, newIDs.size)

        // The first 5 components should NOT have been replaced, but all later components should have been replaced
        // because their position changed as a result of the special padding
        assertEquals(5, oldIDs.intersect(newIDs).size)
        assertEquals(13, oldIDs.union(newIDs).size)
    }
}
