package playground

import graviks.glfw.GraviksWindow
import graviks2d.context.GraviksContext
import graviks2d.resource.text.TextStyle
import graviks2d.util.Color
import gruviks.component.Component
import gruviks.component.HorizontalComponentAlignment
import gruviks.component.VerticalComponentAlignment
import gruviks.component.menu.SimpleFlatMenu
import gruviks.component.menu.controller.SimpleListViewController
import gruviks.component.text.*
import gruviks.glfw.createAndControlGruviksWindow
import gruviks.space.Coordinate
import gruviks.space.Point
import gruviks.space.RectRegion
import gruviks.space.SpaceLayout
import org.lwjgl.vulkan.VK10.VK_MAKE_VERSION
import java.lang.Integer.parseInt

private val nameStyle = TextStyle(
        fillColor = Color.rgbInt(0, 0, 50),
        font = null
)
private val countStyle = TextStyle(
        fillColor = Color.rgbInt(0, 50, 200),
        font = null
)
private val textFieldStyle = transparentTextFieldStyle(
        defaultStyle = nameStyle, focusStyle = TextStyle(fillColor = Color.rgbInt(30,  30, 100), font = null)
)

private val addButtonStyle = TextButtonStyle(
        baseTextStyle = TextStyle(fillColor = Color.rgbInt(0, 50, 0), font = null),
        baseBackgroundColor = Color.rgbInt(0, 200, 0),
        baseBorderColor = Color.TRANSPARENT,
        hoverTextStyle = TextStyle(fillColor = Color.rgbInt(0, 100, 0), font = null),
        hoverBackgroundColor = Color.GREEN,
        hoverBorderColor = Color.TRANSPARENT,
        horizontalAlignment = HorizontalComponentAlignment.Middle,
        verticalAlignment = VerticalComponentAlignment.Middle
)
private val rowButtonStyle = TextButtonStyle(
        baseTextStyle = TextStyle(fillColor = Color.BLACK, font = null),
        baseBackgroundColor = Color.rgbInt(90, 100, 180),
        baseBorderColor = Color.TRANSPARENT,
        hoverTextStyle = TextStyle(fillColor = Color.rgbInt(50, 50, 50), font = null),
        hoverBackgroundColor = Color.rgbInt(110, 120, 220),
        hoverBorderColor = Color.TRANSPARENT,
        horizontalAlignment = HorizontalComponentAlignment.Middle,
        verticalAlignment = VerticalComponentAlignment.Middle
)



private fun createMenu(): Component {
    class Element(val name: String, val count: Int)

    val elements = mutableListOf(
            Element("Hello, world", 2),
            Element("Test trolls", 6),
            Element("Clueless", 1)
    )
    val menu = SimpleFlatMenu(SpaceLayout.GrowDown, Color.rgbInt(220, 230, 170))

    val addNameField = TextField("Name", "Woohoo", textFieldStyle)
    val addCountField = TextField("Count", "5", textFieldStyle)

    val controller = SimpleListViewController(elements) { element, index, position, components, refreshController ->
        val basePosition = position?: Point.percentage(3, 90)

        components.add(Pair(TextComponent(element.name, nameStyle), RectRegion(
                basePosition.x,
                basePosition.y - Coordinate.percentage(4),
                basePosition.x + Coordinate.percentage(20),
                basePosition.y
        )))
        components.add(Pair(TextComponent(element.count.toString(), countStyle), RectRegion(
                basePosition.x + Coordinate.percentage(22),
                basePosition.y - Coordinate.percentage(4),
                basePosition.x + Coordinate.percentage(25),
                basePosition.y
        )))
        components.add(Pair(TextButton("Replace", icon = null, style = rowButtonStyle) { _, _ ->
            try {
                val replacement = Element(addNameField.getText(), parseInt(addCountField.getText()))
                elements[index] = replacement
                refreshController()
            } catch (invalidCount: NumberFormatException) {
                println("Invalid count")
            }
        }, RectRegion(
                basePosition.x + Coordinate.percentage(27),
                basePosition.y - Coordinate.percentage(4),
                basePosition.x + Coordinate.percentage(40),
                basePosition.y
        )))
        components.add(Pair(TextButton("X", icon = null, style = rowButtonStyle) { _, _ ->
            elements.removeAt(index)
            refreshController()
        }, RectRegion(
                basePosition.x + Coordinate.percentage(42),
                basePosition.y - Coordinate.percentage(4),
                basePosition.x + Coordinate.percentage(49),
                basePosition.y
        )))

        Point(basePosition.x, basePosition.y - Coordinate.percentage(6))
    }
    menu.addController(controller)

    menu.addComponent(TextButton(
            "Add element", icon = null, style = addButtonStyle
    ) { _, _ ->
        try {
            elements.add(Element(addNameField.getText(), parseInt(addCountField.getText())))
            controller.refresh()
        } catch (invalidCount: NumberFormatException) {
            println("Invalid count")
        }
    }, RectRegion.percentage(65, 90 ,95, 95))
    menu.addComponent(addNameField, RectRegion.percentage(60, 80, 90, 90))
    menu.addComponent(addCountField, RectRegion.percentage(92, 80, 100, 90))

    return menu
}

fun main() {
    val graviksWindow = GraviksWindow(
            1200, 900, true, "ListControllerPlayground",
            VK_MAKE_VERSION(0, 1, 0), false
    ) { instance, width, height -> GraviksContext(instance, width, height) }

    createAndControlGruviksWindow(graviksWindow, createMenu())
}
