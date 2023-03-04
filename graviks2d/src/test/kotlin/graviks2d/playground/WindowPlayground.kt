package graviks2d.playground

import graviks.glfw.GraviksWindow
import graviks2d.context.GraviksContext
import graviks2d.context.TranslucentPolicy
import graviks2d.resource.text.TextStyle
import graviks2d.util.Color
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.VK_MAKE_VERSION
import org.lwjgl.vulkan.VK10.VK_NULL_HANDLE
import java.lang.Thread.sleep
import java.util.*

fun main() {
    val window = GraviksWindow(
        initialWidth = 800, initialHeight = 600, title = "GraviksWindow",
        enableValidation = true, applicationName = "TestGraviksWindow", applicationVersion = VK_MAKE_VERSION(0, 1, 0),
        preferPowerfulDevice = false, monitor = VK_NULL_HANDLE, shareWindow = VK_NULL_HANDLE
    ) { instance, width, height -> GraviksContext(
        instance = instance, width = width, height = height, translucentPolicy = TranslucentPolicy.Manual
    )}

    var shouldPresentAgain = true

    fun drawFunction() {
        val graviks = window.graviksContext
        val rng = Random()
        if (graviks != null) {
            graviks.fillRect(0f, 0f, 0.5f, 0.5f, Color.RED)
            graviks.fillRect(0.1f, 0.1f, 0.6f, 0.6f, Color.rgbInt(200, 200, 0))

            val textStyle = TextStyle(
                fillColor = Color.BLACK, font = null
            )
            val backgroundColor = Color.rgbInt(250, 250, 250)
            for (x in arrayOf(0f, 0.3f, 0.6f, 0.9f)) {
                for (counter in 0 until 100) {
                    graviks.drawString(
                        x, 0.99f - counter * 0.01f, 1f, 1f - counter * 0.01f,
                        "The random numbers are ${rng.nextLong()} and ${rng.nextLong()}", textStyle, backgroundColor
                    )
                }
            }
        }
    }

    drawFunction()

    glfwSetCursorPosCallback(window.windowHandle) { _, rawX, rawY ->
        val (x, y) = stackPush().use { stack ->

            val pWidth = stack.callocInt(1)
            val pHeight = stack.callocInt(1)
            glfwGetFramebufferSize(window.windowHandle, pWidth, pHeight)

            Pair(rawX.toFloat() / pWidth[0].toFloat(), 1f - rawY.toFloat() / pHeight[0].toFloat())
        }

        val radius = 0.01f
        window.graviksContext?.fillRect(x - radius, y - radius, x + radius, y + radius, Color.rgbInt(0, 100, 200))
        shouldPresentAgain = true
    }

    var typedString = ""
    val lineHeight = 0.05f

    fun drawTypedString() {
        val graviks = window.graviksContext
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
                    textStyle,
                    backgroundColor
                )
            }
            shouldPresentAgain = true
        }
    }

    glfwSetCharCallback(window.windowHandle) { _, charCode ->
        typedString += String(Character.toChars(charCode))
        drawTypedString()
    }

    glfwSetKeyCallback(window.windowHandle) { _, keyCode, _, action, _ ->
        if (keyCode == GLFW_KEY_ENTER && action == GLFW_PRESS) {
            typedString += '`'
        }
        if (keyCode == GLFW_KEY_BACKSPACE && action == GLFW_PRESS && typedString.isNotEmpty()) {
            typedString = typedString.substring(0 until typedString.length - 1)
            drawTypedString()
        }
    }

    while (!glfwWindowShouldClose(window.windowHandle)) {
        glfwPollEvents()

        if (window.shouldResize()) {
            window.resize()
            drawFunction()
            window.presentFrame(false)
        } else if (shouldPresentAgain) {
            shouldPresentAgain = false
            window.presentFrame(false)
        }

        sleep(1)
    }

    window.destroy()
}
