package graviks2d.playground

import graviks.glfw.GraviksWindow
import graviks2d.context.GraviksContext
import graviks2d.resource.text.TextStyle
import graviks2d.util.Color
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.VK_MAKE_VERSION
import java.lang.Thread.sleep
import java.util.*

fun main() {
    val window = GraviksWindow(
        initialWidth = 800, initialHeight = 600, enableValidation = true,
        applicationName = "TestGraviksWindow", applicationVersion = VK_MAKE_VERSION(0, 1, 0),
        preferPowerfulDevice = false
    ) { instance, width, height -> GraviksContext(
        instance = instance, width = width, height = height
    )}

    var shouldPresentAgain = true

    fun drawFunction() {
        val graviks = window.currentGraviksContext
        val rng = Random()
        if (graviks != null) {
            graviks.fillRect(0f, 0f, 0.5f, 0.5f, Color.RED)
            graviks.fillRect(0.1f, 0.1f, 0.6f, 0.6f, Color.rgbInt(200, 200, 0))

            val textStyle = TextStyle(
                fillColor = Color.BLACK, font = null
            )
            for (x in arrayOf(0f, 0.3f, 0.6f, 0.9f)) {
                for (counter in 0 until 100) {
                    graviks.drawString(
                        x, 0.99f - counter * 0.01f, 1f, 1f - counter * 0.01f,
                        "The random numbers are ${rng.nextLong()} and ${rng.nextLong()}", textStyle
                    )
                }
            }
        }
    }

    drawFunction()

    glfwSetCursorPosCallback(window.troll.glfwWindow()) { _, rawX, rawY ->
        val (x, y) = stackPush().use { stack ->

            val pWidth = stack.callocInt(1)
            val pHeight = stack.callocInt(1)
            glfwGetFramebufferSize(window.troll.glfwWindow(), pWidth, pHeight)

            Pair(rawX.toFloat() / pWidth[0].toFloat(), 1f - rawY.toFloat() / pHeight[0].toFloat())
        }

        val radius = 0.01f
        window.currentGraviksContext?.fillRect(x - radius, y - radius, x + radius, y + radius, Color.rgbInt(0, 100, 200))
        shouldPresentAgain = true
    }

    var typedString = ""
    val lineHeight = 0.05f

    fun drawTypedString() {
        val graviks = window.currentGraviksContext
        if (graviks != null) {
            val backgroundColor = Color.rgbInt(250, 250, 250)
            val textStyle = TextStyle(
                fillColor = Color.BLACK, font = null
            )
            graviks.fillRect(0f, 0f, 1f, 1f, backgroundColor)

            val lines = typedString.split("`")
            for ((index, line) in lines.withIndex()) {
                graviks.drawString(
                    0f,
                    1f - (index + 1) * lineHeight,
                    1f,
                    1f - index * lineHeight,
                    line,
                    textStyle
                )
            }
            shouldPresentAgain = true
        }
    }

    glfwSetCharCallback(window.troll.glfwWindow()) { _, charCode ->
        typedString += String(Character.toChars(charCode))
        drawTypedString()
    }

    glfwSetKeyCallback(window.troll.glfwWindow()) { _, keyCode, _, action, _ ->
        if (keyCode == GLFW_KEY_ENTER && action == GLFW_PRESS) {
            typedString += '`'
        }
        if (keyCode == GLFW_KEY_BACKSPACE && action == GLFW_PRESS && typedString.isNotEmpty()) {
            typedString = typedString.substring(0 until typedString.length - 1)
            drawTypedString()
        }
    }

    var oldContext = window.currentGraviksContext
    while (!glfwWindowShouldClose(window.troll.glfwWindow())) {
        glfwPollEvents()

        val currentContext = window.currentGraviksContext
        if (currentContext != oldContext) {
            drawFunction()
            window.presentFrame(false, null)
        } else if (shouldPresentAgain) {
            shouldPresentAgain = false
            window.presentFrame(false, null)
        }
        oldContext = currentContext

        sleep(1)
    }

    window.destroy()
}
