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
import java.lang.System.currentTimeMillis
import java.lang.Thread.sleep

fun main() {
    val window = GraviksWindow(
        initialWidth = 800, initialHeight = 600, title = "GraviksWindow",
        enableValidation = true, applicationName = "TestGraviksWindow", applicationVersion = VK_MAKE_VERSION(0, 1, 0),
        preferPowerfulDevice = false, monitor = VK_NULL_HANDLE, shareWindow = VK_NULL_HANDLE
    ) { instance, width, height -> GraviksContext(
        instance = instance, width = width, height = height, translucentPolicy = TranslucentPolicy.Manual
    )}

    fun drawFunction() {
        val graviks = window.graviksContext
        if (graviks != null) {
            println("draw a frame")
            graviks.fillRect(0f, 0f, 0.5f, 0.5f, Color.rgbInt(255, 0, 0))
            graviks.fillRect(0.1f, 0.1f, 0.6f, 0.6f, Color.rgbInt(200, 200, 0))
        }
    }

    drawFunction()
    window.presentFrame()

    glfwSetCursorPosCallback(window.windowHandle) { _, rawX, rawY ->
        val (x, y) = stackPush().use { stack ->

            val pWidth = stack.callocInt(1)
            val pHeight = stack.callocInt(1)
            glfwGetFramebufferSize(window.windowHandle, pWidth, pHeight)

            Pair(rawX.toFloat() / pWidth[0].toFloat(), 1f - rawY.toFloat() / pHeight[0].toFloat())
        }

        val radius = 0.01f
        window.graviksContext?.fillRect(x - radius, y - radius, x + radius, y + radius, Color.rgbInt(0, 100, 200))
        window.presentFrame()
    }

    var typedString = ""
    val lineHeight = 0.05f

    fun drawTypedString() {
        val graviks = window.graviksContext
        if (graviks != null) {
            val backgroundColor = Color.rgbInt(250, 250, 250)
            val textStyle = TextStyle(
                fillColor = Color.rgbInt(0, 0, 0), font = null
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
            window.presentFrame()
        }
    }

    glfwSetCharCallback(window.windowHandle) { _, charCode ->
        val startTime = currentTimeMillis()
        typedString += String(Character.toChars(charCode))
        drawTypedString()
        val endTime = currentTimeMillis()
        println("Took ${endTime - startTime} ms")
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
            window.presentFrame()
        }

        sleep(1)
    }

    window.destroy()
}
