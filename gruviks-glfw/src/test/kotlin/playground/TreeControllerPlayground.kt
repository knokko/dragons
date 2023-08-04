package playground

import graviks.glfw.GraviksWindow
import graviks2d.context.GraviksContext
import graviks2d.resource.text.TextStyle
import graviks2d.util.Color
import gruviks.component.HorizontalComponentAlignment
import gruviks.component.VerticalComponentAlignment
import gruviks.component.menu.SimpleFlatMenu
import gruviks.component.menu.controller.TreeViewController
import gruviks.component.text.TextButton
import gruviks.component.text.TextButtonStyle
import gruviks.component.text.TextComponent
import gruviks.glfw.createAndControlGruviksWindow
import gruviks.space.Coordinate
import gruviks.space.Point
import gruviks.space.RectRegion
import gruviks.space.SpaceLayout
import org.lwjgl.vulkan.VK10.VK_MAKE_VERSION
import java.io.File

private val nameStyle = TextStyle(
    fillColor = Color.rgbInt(0, 0, 50),
    font = null
)

private val toggleButtonStyle = TextButtonStyle(
    baseTextStyle = TextStyle(fillColor = Color.BLACK, font = null),
    baseBackgroundColor = Color.rgbInt(90, 100, 180),
    baseBorderColor = Color.TRANSPARENT,
    hoverTextStyle = TextStyle(fillColor = Color.rgbInt(50, 50, 50), font = null),
    hoverBackgroundColor = Color.rgbInt(110, 120, 220),
    hoverBorderColor = Color.TRANSPARENT,
    horizontalAlignment = HorizontalComponentAlignment.Middle,
    verticalAlignment = VerticalComponentAlignment.Middle
)

private fun createMenu(): SimpleFlatMenu {
    val tree = TreeViewController.Node(File("troll-demos"), null)
    val menu = SimpleFlatMenu(SpaceLayout.GrowDown, Color.rgbInt(60, 60, 100))
    menu.addController(TreeViewController(
        tree,
        { parentPoint -> Point(parentPoint.x + Coordinate.percentage(3), parentPoint.y) },
        { childPoint -> Point(childPoint.x - Coordinate.percentage(3), childPoint.y) }
    ) { node, _, position, components, refreshTree ->
        val basePosition = position ?: Point.percentage(1, 99)

        components.add(Pair(TextComponent(node.element.name, nameStyle), RectRegion(
            basePosition.x + Coordinate.percentage(3), basePosition.y - Coordinate.percentage(2),
            basePosition.x + Coordinate.percentage(35), basePosition.y
        )))

        if (node.element.isDirectory) {
            val region = RectRegion(
                basePosition.x, basePosition.y - Coordinate.percentage(2),
                basePosition.x + Coordinate.percentage(3), basePosition.y
            )
            if (node.children != null) {
                components.add(Pair(TextButton("<", null, toggleButtonStyle) { _, _ ->
                    node.children = null
                    refreshTree()
                }, region))
            } else {
                components.add(Pair(TextButton(">", null, toggleButtonStyle) { _, _ ->
                    node.children = node.element.listFiles()!!.map {
                        TreeViewController.Node(it, null)
                    }.toMutableList()
                    refreshTree()
                }, region))
            }
        }

        Point(basePosition.x, basePosition.y - Coordinate.percentage(2))
    })

    return menu
}

fun main() {
    val graviksWindow = GraviksWindow(
        1200, 900, true, "TreeControllerPlayground",
        VK_MAKE_VERSION(0, 1, 0), false
    ) { instance, width, height -> GraviksContext(instance, width, height) }

    createAndControlGruviksWindow(graviksWindow, createMenu())
}
