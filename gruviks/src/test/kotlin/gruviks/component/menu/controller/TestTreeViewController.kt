package gruviks.component.menu.controller

import graviks2d.util.Color
import gruviks.component.agent.ComponentAgent
import gruviks.component.agent.DUMMY_FEEDBACK
import gruviks.component.agent.DummyCursorTracker
import gruviks.component.fill.SimpleColorClickComponent
import gruviks.component.fill.SimpleColorFillComponent
import gruviks.component.menu.SimpleFlatMenu
import gruviks.event.Cursor
import gruviks.event.CursorClickEvent
import gruviks.event.EventPosition
import gruviks.event.UpdateEvent
import gruviks.space.Coordinate
import gruviks.space.Point
import gruviks.space.RectRegion
import gruviks.space.SpaceLayout
import gruviks.util.DummyGraviksTarget
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class TestTreeViewController {

    @Test
    fun testWithTwoChildren() {
        val greenNode = TreeViewController.Node(Color.GREEN, null)
        val tree = TreeViewController.Node(Color.BLACK, mutableListOf(greenNode))

        val menu = SimpleFlatMenu(SpaceLayout.Simple, Color.WHITE)
        menu.initAgent(ComponentAgent(DummyCursorTracker(), DUMMY_FEEDBACK) { false })

        var lastID: UUID? = null
        val controller = TreeViewController(
            tree,
            { parent -> Point(parent.x + Coordinate.percentage(10), parent.y) },
            { child -> Point(child.x - Coordinate.percentage(10), child.y) },
            { node, _, position, components, _ ->
                val componentPosition = position ?: Point.percentage(20, 70)
                val component = SimpleColorFillComponent(node.element)
                components.add(Pair(component, RectRegion(
                    componentPosition.x, componentPosition.y - Coordinate.percentage(20),
                    componentPosition.x + Coordinate.percentage(40), componentPosition.y
                )))
                lastID = component.id
                Point(componentPosition.x, componentPosition.y - Coordinate.percentage(25))
            }
        )

        menu.addController(controller)

        assertEquals(0, menu.getComponentIDs().size)
        menu.processEvent(UpdateEvent())
        val greenID = lastID!!
        val rootID = menu.getComponentIDs().first { it != greenID }
        assertEquals(2, menu.getComponentIDs().size)
        assertTrue(menu.getComponentIDs().contains(greenID))

        val redChild = TreeViewController.Node(Color.RED, null)
        greenNode.children = mutableListOf(redChild)
        controller.refresh()
        menu.processEvent(UpdateEvent())

        assertEquals(3, menu.getComponentIDs().size)

        // The green node should have been regenerated because its number of children changed
        assertTrue(menu.getComponentIDs().contains(rootID))
        assertFalse(menu.getComponentIDs().contains(greenID))
        val redID = menu.getComponentIDs().first { it != rootID && it != greenID }

        val blueChild = TreeViewController.Node(Color.BLUE, null)
        greenNode.children!!.add(blueChild)
        controller.refresh()
        menu.processEvent(UpdateEvent())

        assertEquals(4, menu.getComponentIDs().size)
        assertTrue(menu.getComponentIDs().contains(rootID))
        assertFalse(menu.getComponentIDs().contains(greenID))
        assertTrue(menu.getComponentIDs().contains(redID))
        val blueID = menu.getComponentIDs().first { it != rootID && it != redID && it != greenID }

        greenNode.children = mutableListOf(blueChild)
        controller.refresh()
        menu.processEvent(UpdateEvent())

        assertEquals(3, menu.getComponentIDs().size)
        assertTrue(menu.getComponentIDs().contains(rootID))
        assertFalse(menu.getComponentIDs().contains(redID))

        // The blue component should have been replaced because its position changed when the red component was removed
        assertFalse(menu.getComponentIDs().contains(blueID))
        // The green component should have been replaced because its number of children changed
        assertFalse(menu.getComponentIDs().contains(greenID))
    }

    @Test
    fun testInteractiveTree() {
        val menu = SimpleFlatMenu(SpaceLayout.Simple, Color.RED)
        menu.initAgent(ComponentAgent(DummyCursorTracker(), DUMMY_FEEDBACK) { false })

        val tree = TreeViewController.Node(Color.rgbInt(1, 0, 0), null)

        var lastColor = Color.WHITE

        val controller = TreeViewController(
            tree,
            { parent -> Point(parent.x + Coordinate.percentage(10), parent.y) },
            { child -> Point(child.x - Coordinate.percentage(10), child.y) },
            { node, _, position, components, refreshController ->
                val componentPosition = position ?: Point.percentage(0, 100)

                components.add(Pair(SimpleColorClickComponent(node.element) { event, _ ->
                    if (event.button == 1) {
                        node.element = Color.rgbInt(node.element.red * 2, 0, 0)

                        val children = node.children ?: mutableListOf()
                        children.add(TreeViewController.Node(Color.rgbInt(node.element.red + 1, 0, 0), null))
                        node.children = children

                        refreshController()
                    } else if (event.button == 2) lastColor = node.element
                    else {
                        node.children = null
                        refreshController()
                    }
                }, RectRegion(
                    componentPosition.x, componentPosition.y - Coordinate.percentage(10),
                    componentPosition.x + Coordinate.percentage(10), componentPosition.y
                )))

                Point(componentPosition.x, componentPosition.y - Coordinate.percentage(10))
            }
        )
        menu.addController(controller)

        val cursor = Cursor(12)
        val target = DummyGraviksTarget()
        menu.processEvent(UpdateEvent())
        menu.render(target, false)

        // The root node (top-left corner) should have the color black
        menu.processEvent(CursorClickEvent(cursor, EventPosition(0.05f, 0.95f), 2))
        assertEquals(Color.rgbInt(1, 0, 0), lastColor)

        // Trigger the root node, which should double its red, and create a child node
        menu.processEvent(CursorClickEvent(cursor, EventPosition(0.05f, 0.95f), 1))
        menu.processEvent(CursorClickEvent(cursor, EventPosition(0.05f, 0.95f), 2))
        assertEquals(Color.rgbInt(2, 0, 0), lastColor)

        menu.processEvent(UpdateEvent())
        menu.render(target, false)
        menu.processEvent(CursorClickEvent(cursor, EventPosition(0.15f, 0.85f), 2))
        assertEquals(Color.rgbInt(3, 0, 0), lastColor)

        // Trigger the child of the root node
        menu.processEvent(CursorClickEvent(cursor, EventPosition(0.15f, 0.85f), 1))
        menu.processEvent(UpdateEvent())
        menu.render(target, false)
        menu.processEvent(CursorClickEvent(cursor, EventPosition(0.15f, 0.85f), 2))
        assertEquals(Color.rgbInt(6, 0, 0), lastColor)
        menu.processEvent(CursorClickEvent(cursor, EventPosition(0.25f, 0.75f), 2))
        assertEquals(Color.rgbInt(7, 0, 0), lastColor)

        // Collapse all children
        menu.processEvent(CursorClickEvent(cursor, EventPosition(0.05f, 0.95f), 3))
        menu.processEvent(UpdateEvent())
        menu.render(target, false)
        assertEquals(1, menu.getComponentIDs().size)
    }
}
