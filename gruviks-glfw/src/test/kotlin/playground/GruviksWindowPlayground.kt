package playground

import graviks.glfw.GraviksWindow
import graviks2d.context.GraviksContext
import graviks2d.context.TranslucentPolicy
import graviks2d.resource.text.TextAlignment
import graviks2d.resource.text.TextStyle
import graviks2d.util.Color
import gruviks.component.HorizontalComponentAlignment
import gruviks.component.VerticalComponentAlignment
import gruviks.component.menu.SimpleFlatMenu
import gruviks.component.text.TextButton
import gruviks.component.text.TextButtonStyle
import gruviks.component.text.TextComponent
import gruviks.feedback.*
import gruviks.glfw.createAndControlGruviksWindow
import gruviks.space.Coordinate
import gruviks.space.RectRegion
import gruviks.space.SpaceLayout
import org.lwjgl.vulkan.VK10.VK_MAKE_VERSION

private val baseButtonStyle = TextButtonStyle(
    baseTextStyle = TextStyle(
        fillColor = Color.WHITE, font = null, alignment = TextAlignment.Centered
    ),
    baseBackgroundColor = Color.TRANSPARENT,
    baseBorderColor = Color.WHITE,
    hoverTextStyle = TextStyle(
        fillColor = Color.rgbInt(127, 127, 127), font = null, alignment = TextAlignment.Centered
    ),
    hoverBackgroundColor = Color.WHITE,
    hoverBorderColor = Color.WHITE,
    horizontalAlignment = HorizontalComponentAlignment.Middle,
    verticalAlignment = VerticalComponentAlignment.Middle
)

private fun createTitleScreen(): SimpleFlatMenu {
    val backgroundColor = Color.rgbInt(72, 72, 72)
    val menu = SimpleFlatMenu(SpaceLayout.Simple, backgroundColor)
    //val testIcon = ImageReference.classLoaderPath("test-icon.png", false)

    val titleStyle = TextStyle(
        fillColor = Color.rgbInt(240, 87, 87),
        font = null,
        alignment = TextAlignment.Centered
    )
    val subtitleStyle = TextStyle(
        fillColor = Color.WHITE,
        font = null,
        alignment = TextAlignment.Centered
    )

    val exitButtonStyle = TextButtonStyle(
        baseTextStyle = TextStyle(
            fillColor = Color.rgbInt(250, 40, 70), font = null, alignment = TextAlignment.Centered
        ),
        baseBackgroundColor = Color.TRANSPARENT,
        baseBorderColor = Color.rgbInt(250, 40, 70),
        hoverTextStyle = TextStyle(
            fillColor = Color.rgbInt(125, 20, 35), font = null, alignment = TextAlignment.Centered
        ),
        hoverBackgroundColor = Color.rgbInt(250, 40, 70),
        hoverBorderColor = Color.rgbInt(250, 40, 70),
        horizontalAlignment = HorizontalComponentAlignment.Middle,
        verticalAlignment = VerticalComponentAlignment.Middle
    )

    menu.addComponent(
        TextComponent("Knokko's", titleStyle, backgroundColor),
        RectRegion.percentage(20, 87, 80, 98)
    )
    menu.addComponent(
        TextComponent("Custom Items Editor", subtitleStyle, backgroundColor),
        RectRegion.percentage(20, 83, 80, 87)
    )

    menu.addComponent(TextButton("New Item Set", null, baseButtonStyle) { _, giveFeedback ->
        giveFeedback(ReplaceYouFeedback(::createNewItemSetMenu))
    }, RectRegion.percentage(20, 46, 80, 56))
    menu.addComponent(TextButton("Edit Item Set", null, baseButtonStyle) { _, giveFeedback ->
        giveFeedback(ShiftCameraFeedback(Coordinate.percentage(-10), Coordinate.percentage(-10)))
    }, RectRegion.percentage(20, 33, 80, 43))
    menu.addComponent(TextButton("Combine Item Sets", null, baseButtonStyle) { _, giveFeedback ->
        giveFeedback(ShiftCameraFeedback(Coordinate.percentage(10), Coordinate.percentage(10)))
    }, RectRegion.percentage(20, 20, 80, 30))
    menu.addComponent(TextButton("Exit Editor", null, exitButtonStyle) { _, giveFeedback ->
        giveFeedback(AddressedFeedback(null, ExitFeedback()))
    }, RectRegion.percentage(20, 7, 80, 17))

    return menu
}

fun createNewItemSetMenu(): SimpleFlatMenu {
    val menu = SimpleFlatMenu(SpaceLayout.Simple, Color.BLUE)
    menu.addComponent(TextButton(
        "Back", null, baseButtonStyle
    ) { _, giveFeedback ->
        giveFeedback(ReplaceYouFeedback(::createTitleScreen))
    }, RectRegion.percentage
    (10, 40, 90, 60))
    return menu
}

fun main() {
    val graviksWindow = GraviksWindow(
        1000, 800, "Gruviks Tester", true, "Gruviks Tester",
        VK_MAKE_VERSION(0, 1, 0), true
    ) { instance, width, height ->
        GraviksContext(instance, width, height, TranslucentPolicy.Manual)
    }

    createAndControlGruviksWindow(graviksWindow, createTitleScreen())
}
