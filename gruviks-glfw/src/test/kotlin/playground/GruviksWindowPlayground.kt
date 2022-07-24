package playground

import graviks.glfw.GraviksWindow
import graviks2d.context.GraviksContext
import graviks2d.context.TranslucentPolicy
import graviks2d.resource.image.ImageReference
import graviks2d.util.Color
import gruviks.component.text.TextButton
import gruviks.component.text.TextButtonStyle
import gruviks.glfw.createAndControlGruviksWindow
import org.lwjgl.vulkan.VK10.VK_MAKE_VERSION

fun main() {
    val graviksWindow = GraviksWindow(
        1000, 800, "Gruviks Tester", true, "Gruviks Tester",
        VK_MAKE_VERSION(0, 1, 0), true
    ) { instance, width, height ->
        GraviksContext(instance, width, height, TranslucentPolicy.Manual)
    }

    createAndControlGruviksWindow(graviksWindow, TextButton(
        "Play!!", ImageReference.classLoaderPath("test-icon.png", false), TextButtonStyle.textAndBorder(
            Color.rgbInt(200, 70, 0), Color.rgbInt(250, 100, 0)
        )
    ) {
        println("Clicked at ${it.position}")
    })
    //createAndControlGruviksWindow(graviksWindow, ColorShuffleComponent())
}
