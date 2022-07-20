package gruviks.glfw

import graviks.glfw.GraviksWindow
import gruviks.component.Component
import gruviks.core.GruviksWindow
import gruviks.event.Cursor
import gruviks.event.EventPosition
import gruviks.event.raw.RawCursorLeaveEvent
import gruviks.event.raw.RawCursorMoveEvent
import gruviks.event.raw.RawCursorPressEvent
import gruviks.event.raw.RawCursorReleaseEvent
import org.lwjgl.glfw.GLFW.*
import java.lang.Thread.sleep

fun createAndControlGruviksWindow(graviksWindow: GraviksWindow, rootComponent: Component) {
    val gruviksWindow = GruviksWindow(rootComponent)

    val mouseCursor = Cursor(0)
    var shouldPresentAgain = true

    graviksWindow.graviksContext?.run { gruviksWindow.render(this, true) }

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
    
    while (!glfwWindowShouldClose(graviksWindow.windowHandle)) {
        glfwPollEvents()

        var forceRender = false
        if (graviksWindow.shouldResize()) {
            graviksWindow.resize()
            forceRender = true
        }

        graviksWindow.graviksContext?.run {
            if (gruviksWindow.render(this, forceRender)) {
                shouldPresentAgain = true
            }
        }

        if (shouldPresentAgain) {
            shouldPresentAgain = false
            graviksWindow.presentFrame()
        }

        sleep(1)
    }

    graviksWindow.destroy()
}
