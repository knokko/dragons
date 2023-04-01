package gruviks.glfw

import graviks.glfw.GraviksWindow
import gruviks.component.Component
import gruviks.component.RectangularDrawnRegion
import gruviks.core.GruviksWindow
import gruviks.event.Cursor
import gruviks.event.EventPosition
import gruviks.event.raw.RawCursorLeaveEvent
import gruviks.event.raw.RawCursorMoveEvent
import gruviks.event.raw.RawCursorPressEvent
import gruviks.event.raw.RawCursorReleaseEvent
import gruviks.util.optimizeRecentDrawnRegions
import gruviks.event.Key
import gruviks.event.KeyType
import gruviks.event.raw.*
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VkRectLayerKHR
import java.lang.Integer.max
import java.lang.Integer.min
import java.lang.System.currentTimeMillis
import java.lang.Thread.sleep

fun createAndControlGruviksWindow(graviksWindow: GraviksWindow, rootComponent: Component) {
    val gruviksWindow = GruviksWindow(rootComponent)

    val mouseCursor = Cursor(0)
    val regionsToPresent = mutableListOf<RectangularDrawnRegion>()

    var lastPresentTime = 0L

    graviksWindow.graviksContext?.run { gruviksWindow.render(this, true, null) }

    glfwSetCursorPosCallback(graviksWindow.windowHandle) { _, newRawX, newRawY ->
        graviksWindow.graviksContext?.run {
            val newX = newRawX.toFloat() / this.width.toFloat()
            val newY = 1f - newRawY.toFloat() / this.height.toFloat()
            gruviksWindow.fireEvent(RawCursorMoveEvent(mouseCursor, EventPosition(newX, newY)))
        }
    }

    glfwSetMouseButtonCallback(graviksWindow.windowHandle) { _, button, action, _ ->
        if (action == GLFW_PRESS) {
            gruviksWindow.fireEvent(RawCursorPressEvent(mouseCursor, button))
        }
        if (action == GLFW_RELEASE) {
            gruviksWindow.fireEvent(RawCursorReleaseEvent(mouseCursor, button))
        }
    }

    glfwSetCursorEnterCallback(graviksWindow.windowHandle) { _, entered ->
        if (!entered) {
            gruviksWindow.fireEvent(RawCursorLeaveEvent(mouseCursor))
        }
    }

    glfwSetCharCallback(graviksWindow.windowHandle) { _, codePoint ->
        gruviksWindow.fireEvent(RawKeyTypeEvent(codePoint))
    }

    glfwSetKeyCallback(graviksWindow.windowHandle) { _, keyCode, _, wasPressed, _ ->
        val keyType = when (keyCode) {
            GLFW_KEY_LEFT -> KeyType.Left
            GLFW_KEY_RIGHT -> KeyType.Right
            GLFW_KEY_UP -> KeyType.Up
            GLFW_KEY_DOWN -> KeyType.Down
            GLFW_KEY_ENTER -> KeyType.Enter
            GLFW_KEY_BACKSPACE -> KeyType.Backspace
            GLFW_KEY_ESCAPE -> KeyType.Escape
            GLFW_KEY_TAB -> KeyType.Tab
            else -> KeyType.Other
        }
        val key = Key(keyCode, keyType)
        if (wasPressed == GLFW_PRESS || wasPressed == GLFW_REPEAT) {
            gruviksWindow.fireEvent(RawKeyPressEvent(key, wasPressed == GLFW_REPEAT))
        }
        if (wasPressed == GLFW_RELEASE) {
            gruviksWindow.fireEvent(RawKeyReleaseEvent(key))
        }
    }
    
    while (!glfwWindowShouldClose(graviksWindow.windowHandle) && !gruviksWindow.shouldExit()) {
        glfwPollEvents()

        var forceRender = false
        if (graviksWindow.shouldResize()) {
            graviksWindow.resize()
            forceRender = true
        }

        graviksWindow.graviksContext?.run {
            gruviksWindow.render(this, forceRender, regionsToPresent)

            if (regionsToPresent.isNotEmpty()) {

                // Avoid piling up frames to be presented (since vsync is enabled) because that would increase latency
                run {
                    val currentTime = currentTimeMillis()
                    if (currentTime - lastPresentTime < 16) {
                        sleep(lastPresentTime + 17 - currentTime)
                    }
                }
                lastPresentTime = currentTimeMillis()

                graviksWindow.presentFrame(false) { stack ->
                    val optimizedRegions = optimizeRecentDrawnRegions(regionsToPresent)
                    allocatePresentRegions(stack, optimizedRegions, width, height)
                }
                regionsToPresent.clear()
            }
        }

        sleep(1)
    }

    graviksWindow.destroy()
}

internal fun allocatePresentRegions(
        stack: MemoryStack, regionsToPresent: List<RectangularDrawnRegion>, width: Int, height: Int
): VkRectLayerKHR.Buffer {
    val resultRegions = VkRectLayerKHR.calloc(regionsToPresent.size, stack)
    for ((index, region) in regionsToPresent.withIndex()) {
        resultRegions[index].offset().set(
                max(0, (region.minX * width).toInt() - 1),
                max(0, ((1f - region.maxY) * height).toInt() - 1)
        )
        val intBoundX = min(width, (region.maxX * width).toInt() + 1)
        val intBoundY = min(height, ((1f - region.minY) * height).toInt() + 1)
        resultRegions[index].extent().set(
                intBoundX - resultRegions[index].offset().x(),
                intBoundY - resultRegions[index].offset().y()
        )
        resultRegions[index].layer(0)
    }
    return resultRegions
}
