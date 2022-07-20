package playground

import graviks.glfw.GraviksWindow
import graviks2d.context.GraviksContext
import graviks2d.context.TranslucentPolicy
import graviks2d.util.Color
import gruviks.component.fill.SimpleColorFillComponent
import gruviks.component.test.ColorShuffleComponent
import gruviks.glfw.createAndControlGruviksWindow
import org.lwjgl.vulkan.VK10.VK_MAKE_VERSION

fun main() {
    val graviksWindow = GraviksWindow(
        1000, 800, "Gruviks Tester", true, "Gruviks Tester",
        VK_MAKE_VERSION(0, 1, 0), false
    ) { instance, width, height ->
        GraviksContext(instance, width, height, TranslucentPolicy.Manual)
    }

    createAndControlGruviksWindow(graviksWindow, ColorShuffleComponent())
}
