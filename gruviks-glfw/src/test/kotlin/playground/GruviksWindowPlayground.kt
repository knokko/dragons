package playground

import com.github.knokko.memory.MemorySnapshot
import com.github.knokko.profiler.SampleProfiler
import com.github.knokko.profiler.storage.SampleStorage
import graviks.glfw.GraviksWindow
import graviks2d.context.GraviksContext
import graviks2d.resource.image.ImageReference
import graviks2d.resource.text.TextAlignment
import graviks2d.resource.text.TextStyle
import graviks2d.util.Color
import gruviks.component.HorizontalComponentAlignment
import gruviks.component.VerticalComponentAlignment
import gruviks.component.menu.SimpleFlatMenu
import gruviks.component.test.DiscoComponent
import gruviks.component.text.TextButton
import gruviks.component.text.TextButtonStyle
import gruviks.component.text.TextComponent
import gruviks.component.text.*
import gruviks.feedback.*
import gruviks.glfw.createAndControlGruviksWindow
import gruviks.space.Coordinate
import gruviks.space.RectRegion
import gruviks.space.SpaceLayout
import org.lwjgl.vulkan.VK10.VK_MAKE_VERSION
import java.io.File
import java.io.PrintWriter
import java.util.function.Predicate

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

private val textFieldStyle = transparentTextFieldStyle(
    defaultStyle = TextStyle(
        fillColor = Color.rgbInt(172, 172, 172), font = null, alignment = TextAlignment.Natural
    ),
    focusStyle = TextStyle(
        fillColor = Color.WHITE, font = null, alignment = TextAlignment.Natural
    )
)

private fun createTitleScreen(): SimpleFlatMenu {
    val backgroundColor = Color.rgbInt(72, 72, 72)
    val menu = SimpleFlatMenu(SpaceLayout.Simple, backgroundColor)
    val testIcon = ImageReference.classLoaderPath("test-icon.png", false)

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
        TextComponent("Knokko's", titleStyle),
        RectRegion.percentage(20, 87, 80, 98)
    )
    menu.addComponent(
        TextComponent("Custom Items Editor", subtitleStyle),
        RectRegion.percentage(20, 83, 80, 87)
    )

    menu.addComponent(
        TextField("Input Filename", "", textFieldStyle),
        RectRegion.percentage(10, 70, 90, 80)
    )

    menu.addComponent(TextButton("New Item Set", testIcon, baseButtonStyle) { _, giveFeedback ->
        giveFeedback(ReplaceYouFeedback(::createNewItemSetMenu))
    }, RectRegion.percentage(20, 46, 80, 56))
    menu.addComponent(TextButton("Edit Item Set", null, baseButtonStyle) { _, giveFeedback ->
        giveFeedback(ShiftCameraFeedback(Coordinate.percentage(-10), Coordinate.percentage(-10)))
    }, RectRegion.percentage(20, 33, 80, 43))
    menu.addComponent(TextButton("Combine Item Sets", null, baseButtonStyle) { _, giveFeedback ->
        giveFeedback(ShiftCameraFeedback(Coordinate.percentage(10), Coordinate.percentage(10)))
    }, RectRegion.percentage(20, 20, 80, 30))
    menu.addComponent(TextButton("Exit Editor", testIcon, exitButtonStyle) { _, giveFeedback ->
        giveFeedback(AddressedFeedback(null, ExitFeedback()))
    }, RectRegion.percentage(20, 7, 80, 17))

    menu.addComponent(DiscoComponent(), RectRegion.percentage(10, 80, 20, 90))
    menu.addComponent(DiscoComponent(), RectRegion.percentage(10, 60, 20, 70))
    menu.addComponent(DiscoComponent(), RectRegion.percentage(80, 80, 90, 90))
    menu.addComponent(DiscoComponent(), RectRegion.percentage(90, 0, 100, 10))
    menu.addComponent(DiscoComponent(), RectRegion.percentage(90, 10, 100, 20))
    menu.addComponent(DiscoComponent(), RectRegion.percentage(90, 20, 100, 30))

    return menu
}

fun createNewItemSetMenu(): SimpleFlatMenu {
    val menu = SimpleFlatMenu(SpaceLayout.Simple, Color.BLUE)
    menu.addComponent(TextButton(
        "Back", null, baseButtonStyle
    ) { _, giveFeedback ->
        giveFeedback(ReplaceYouFeedback(::createTitleScreen))
    }, RectRegion.percentage
    (10, 0, 90, 10))

    val textAreaStyle = squareTextAreaStyle(
            defaultTextStyle = TextStyle(fillColor = Color.rgbInt(50, 50, 50), font = null),
            defaultBackgroundColor = Color.rgbInt(200, 200, 200),
            focusTextStyle = TextStyle(fillColor = Color.BLACK, font = null),
            focusBackgroundColor = Color.WHITE,
            lineHeight = 0.04f,
            placeholderStyle = TextStyle(fillColor = Color.rgbInt(100, 100, 100), font = null),
            selectionBackgroundColor = Color.rgbInt(0, 150, 200)
    )
    menu.addComponent(TextArea(
            "test1234\ntest", textAreaStyle, "Just put some text here\n\rCan be whatever you want!"
    ), RectRegion.percentage(1, 11, 99, 99))
    return menu
}

fun main() {
    println("Initial memory usage:")
    MemorySnapshot.take().debugDump()
    println()

    val storage = SampleStorage.frequency()
    val profiler = SampleProfiler(storage)
    profiler.classNameFilter = Predicate { className: String -> className.contains("gruviks") }
    profiler.start()

    val graviksWindow = GraviksWindow(
        1000, 800, true, "Gruviks Tester",
        VK_MAKE_VERSION(0, 1, 0), true
    ) { instance, width, height ->
        GraviksContext(instance, width, height)
    }

    println("After window creation memory usage:")
    MemorySnapshot.take().debugDump()
    println()

    createAndControlGruviksWindow(graviksWindow, createTitleScreen())

    profiler.stop()
    storage.getThreadStorage(Thread.currentThread().threadId()).print(
        PrintWriter(File("performance-gruviks.log")), 10, 1.0
    )

    println("After window close memory usage:")
    MemorySnapshot.take().debugDump()
    println()
}
