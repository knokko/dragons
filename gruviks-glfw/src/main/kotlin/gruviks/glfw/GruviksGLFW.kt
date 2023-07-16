package gruviks.glfw

import graviks.glfw.GraviksWindow
import gruviks.component.Component
import gruviks.component.RectangularDrawnRegion
import gruviks.core.GruviksWindow
import gruviks.event.*
import gruviks.event.raw.RawCursorLeaveEvent
import gruviks.event.raw.RawCursorMoveEvent
import gruviks.event.raw.RawCursorPressEvent
import gruviks.event.raw.RawCursorReleaseEvent
import gruviks.util.optimizeRecentDrawnRegions
import gruviks.event.raw.*
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.vkDeviceWaitIdle
import org.lwjgl.vulkan.VkRectLayerKHR
import java.lang.Integer.max
import java.lang.Integer.min
import java.lang.System.currentTimeMillis
import java.lang.Thread.sleep

fun createAndControlGruviksWindow(
    graviksWindow: GraviksWindow,
    rootComponent: Component,
    destroyFunction: () -> Unit = { }
) {
    val gruviksWindow = GruviksWindow(rootComponent)

    val mouseCursor = Cursor(0)
    val regionsToPresent = mutableListOf<RectangularDrawnRegion>()

    var lastPresentTime = 0L
    var shiftDown = false
    var controlDown = false

    val windowHandle = graviksWindow.graviksInstance.troll.glfwWindow()

    glfwSetCursorPosCallback(windowHandle) { _, newRawX, newRawY ->
        graviksWindow.currentGraviksContext?.run {
            val newX = newRawX.toFloat() / this.width.toFloat()
            val newY = 1f - newRawY.toFloat() / this.height.toFloat()
            gruviksWindow.fireEvent(RawCursorMoveEvent(mouseCursor, EventPosition(newX, newY)))
        }
    }

    glfwSetMouseButtonCallback(windowHandle) { _, button, action, _ ->
        if (action == GLFW_PRESS) {
            gruviksWindow.fireEvent(RawCursorPressEvent(mouseCursor, button))
        }
        if (action == GLFW_RELEASE) {
            gruviksWindow.fireEvent(RawCursorReleaseEvent(mouseCursor, button))
        }
    }

    glfwSetScrollCallback(windowHandle) { _, x, y ->

        // There is no science behind the magic value 0.1: it just feels like a reasonable scale
        val scale = 0.1f

        if (x != 0.0) {
            gruviksWindow.fireEvent(RawCursorScrollEvent(mouseCursor, x.toFloat() * scale, ScrollDirection.Horizontal))
        }
        if (y != 0.0) {
            val direction = if (shiftDown) ScrollDirection.Horizontal else if (controlDown) ScrollDirection.Zoom else ScrollDirection.Vertical
            gruviksWindow.fireEvent(RawCursorScrollEvent(mouseCursor, y.toFloat() * -scale, direction))
        }
    }

    glfwSetCursorEnterCallback(windowHandle) { _, entered ->
        if (!entered) {
            gruviksWindow.fireEvent(RawCursorLeaveEvent(mouseCursor))
        }
    }

    glfwSetCharCallback(windowHandle) { _, codePoint ->
        gruviksWindow.fireEvent(RawKeyTypeEvent(codePoint))
    }

    glfwSetKeyCallback(windowHandle) { _, keyCode, _, wasPressed, _ ->
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

        if (keyCode == GLFW_KEY_LEFT_SHIFT || keyCode == GLFW_KEY_RIGHT_SHIFT) {
            if (wasPressed == GLFW_PRESS || wasPressed == GLFW_REPEAT) shiftDown = true
            if (wasPressed == GLFW_RELEASE) shiftDown = false
        }
        if (keyCode == GLFW_KEY_LEFT_CONTROL || keyCode == GLFW_KEY_RIGHT_CONTROL) {
            if (wasPressed == GLFW_PRESS || wasPressed == GLFW_REPEAT) controlDown = true
            if (wasPressed == GLFW_RELEASE) controlDown = false
        }
    }

    glfwSetWindowRefreshCallback(graviksWindow.troll.glfwWindow()) {
        graviksWindow.drawAndPresent(false, { graviksContext ->
            gruviksWindow.render(graviksContext, true, mutableListOf())
        }, null)
    }

    glfwSetWindowPosCallback(graviksWindow.troll.glfwWindow()) { _, _, _ ->
        graviksWindow.drawAndPresent(false, { graviksContext ->
            gruviksWindow.render(graviksContext, false, mutableListOf())
        }, null)
    }

    var lastContext = graviksWindow.currentGraviksContext
    while (!glfwWindowShouldClose(windowHandle) && !gruviksWindow.shouldExit()) {
        glfwPollEvents()

        val currentContext = graviksWindow.currentGraviksContext

        gruviksWindow.fireEvent(RawUpdateEvent())

        currentContext?.run {
            gruviksWindow.render(this, currentContext != lastContext, regionsToPresent)

            if (regionsToPresent.isNotEmpty()) {

                // Avoid piling up frames to be presented (since vsync is enabled) because that would increase latency
                run {
                    val currentTime = currentTimeMillis()
                    if (currentTime - lastPresentTime < 16) {
                        sleep(lastPresentTime + 17 - currentTime)
                    }
                }
                lastPresentTime = currentTimeMillis()

                var width = this.width
                var height = this.height

                graviksWindow.drawAndPresent(false, { newContext ->
                    // This work-around is needed to avoid a blink after resizing
                    if (newContext !== this) {
                        regionsToPresent.clear()
                        gruviksWindow.render(newContext, true, regionsToPresent)
                        width = newContext.width
                        height = newContext.height
                    }
                }) { stack ->
                    val optimizedRegions = optimizeRecentDrawnRegions(regionsToPresent)
                    allocatePresentRegions(stack, optimizedRegions, width, height)
                }
                regionsToPresent.clear()
            }
        }
        lastContext = currentContext

        sleep(1)
    }

    vkDeviceWaitIdle(graviksWindow.troll.vkDevice())
    destroyFunction()
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
