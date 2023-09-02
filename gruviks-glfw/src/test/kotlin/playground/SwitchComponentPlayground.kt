package playground

import graviks.glfw.GraviksWindow
import graviks2d.context.GraviksContext
import graviks2d.resource.text.TextStyle
import graviks2d.util.Color
import gruviks.component.fill.SimpleColorFillComponent
import gruviks.component.menu.SimpleFlatMenu
import gruviks.component.text.TextArea
import gruviks.component.text.TextButton
import gruviks.component.text.TextButtonStyle
import gruviks.component.text.squareTextAreaStyle
import gruviks.component.util.SwitchComponent
import gruviks.event.CursorClickEvent
import gruviks.feedback.Feedback
import gruviks.glfw.createAndControlGruviksWindow
import gruviks.space.RectRegion
import gruviks.space.SpaceLayout
import org.lwjgl.vulkan.VK10.VK_MAKE_VERSION

private fun createRootMenu(): SimpleFlatMenu {
    val textAreaStyle = squareTextAreaStyle(
        defaultTextStyle = TextStyle(fillColor = Color.BLACK, font = null),
        defaultBackgroundColor = Color.rgbInt(150, 150, 150),
        focusTextStyle = TextStyle(fillColor = Color.BLACK, font = null),
        focusBackgroundColor = Color.rgbInt(200, 200, 200),
        lineHeight = 0.05f, placeholderStyle = null, selectionBackgroundColor = Color.rgbInt(0, 150, 200)
    )
    val buttonStyle = TextButtonStyle.textAndBorder(
        baseColor = Color.rgbInt(0, 230, 120),
        hoverColor = Color.rgbInt(0, 255, 150),
    )
    val nope = { _: CursorClickEvent, _: (Feedback) -> Unit -> }
    val backgroundComponent = SimpleColorFillComponent(Color.rgbaInt(200, 0, 0, 100))
    val switchComponent = SwitchComponent(backgroundComponent)

    val text1 = TextArea("Once, a long time\n ago...", textAreaStyle)
    val text2 = TextArea("Well...\nThis is the other text\narea I guess", textAreaStyle)

    val innerMenu = SimpleFlatMenu(SpaceLayout.Simple, Color.rgbaInt(0, 200, 0, 200))
    innerMenu.addComponent(
        TextButton("This does nothing", null, buttonStyle, nope),
        RectRegion.percentage(30, 70, 70, 90)
    )
    innerMenu.addComponent(
        TextButton("Switch to text2", null, buttonStyle) { _, _ ->
            switchComponent.setComponent(text2)
        }, RectRegion.percentage(30, 30, 70, 40)
    )

    val menu = SimpleFlatMenu(SpaceLayout.Simple, Color.rgbInt(100, 120, 200))
    menu.addComponent(switchComponent, RectRegion.percentage(10, 10, 60, 90))

    menu.addComponent(
        TextButton("Reset", null, buttonStyle) { _, _ -> switchComponent.setComponent(backgroundComponent) },
        RectRegion.percentage(65, 85, 75, 90)
    )
    menu.addComponent(
        TextButton("Switch to text area 1", null, buttonStyle) { _, _ -> switchComponent.setComponent(text1) },
        RectRegion.percentage(65, 79, 90, 84)
    )
    menu.addComponent(
        TextButton("Switch to inner menu", null, buttonStyle) { _, _ -> switchComponent.setComponent(innerMenu) },
        RectRegion.percentage(65, 73, 90, 78)
    )
    return menu
}

fun main() {

    val graviksWindow = GraviksWindow(
        1200, 900, true, "SwitchComponentPlayground",
        VK_MAKE_VERSION(0, 1, 0), false
    ) { instance, width, height -> GraviksContext(instance, width, height) }

    createAndControlGruviksWindow(graviksWindow, createRootMenu())
}
