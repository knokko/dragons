package gruviks.component.text

import graviks2d.resource.text.TextStyle
import graviks2d.target.GraviksTarget
import graviks2d.util.Color
import gruviks.component.RectangularDrawnRegion
import gruviks.component.RenderResult

typealias TextFieldBackgroundFunction = (target: GraviksTarget, placeholder: String, error: String) ->
        Triple<TextStyle, RectangularDrawnRegion, Boolean>

class TextFieldStyle(
    val drawDefaultBackground: TextFieldBackgroundFunction,
    val computeDefaultResult: (renderedTextPosition: RectangularDrawnRegion?) -> RenderResult,
    val drawFocusBackground: TextFieldBackgroundFunction,
    val computeFocusResult: (renderedTextPosition: RectangularDrawnRegion?) -> RenderResult
)

fun transparentTextFieldStyle(
    defaultStyle: TextStyle, focusStyle: TextStyle, lineHeight: Float = 0.05f, placeholderHeight: Float = 0.4f
) = TextFieldStyle(
    drawDefaultBackground = { target, _, error ->
        var style = defaultStyle
        if (error.isNotEmpty()) style = defaultStyle.createChild(fillColor = Color.RED)
        target.fillRect(0f, 0f, 1f, lineHeight, defaultStyle.fillColor)
        Triple(style, RectangularDrawnRegion(0f, lineHeight, 1f, 1f - placeholderHeight / 2f), true)
    }, drawFocusBackground = { target, placeholder, error ->
        target.fillRect(0f, 0f, 1f, lineHeight, focusStyle.fillColor)
        val maxY = if (placeholder.isEmpty()) 1f else 1f - placeholderHeight

        val errorStyle = focusStyle.createChild(fillColor = Color.RED)

        if (error.isNotEmpty()) target.drawString(0f, maxY, 1f, 1f, error, errorStyle)
        else if (placeholder.isNotEmpty()) target.drawString(0f, maxY, 1f, 1f, placeholder, focusStyle)
        Triple(if (error.isEmpty()) focusStyle else errorStyle, RectangularDrawnRegion(0f, lineHeight, 1f, maxY), false)
    }, computeDefaultResult = { textPosition -> RenderResult(drawnRegion = RectangularDrawnRegion(
        minX = 0f, minY = 0f, maxX = 1f, maxY = textPosition?.maxY ?: lineHeight
    ), propagateMissedCursorEvents = true)}, computeFocusResult = { RenderResult(
        drawnRegion = RectangularDrawnRegion(0f, 0f, 1f, 1f), propagateMissedCursorEvents = false
    )}
)
