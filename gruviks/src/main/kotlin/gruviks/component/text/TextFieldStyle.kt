package gruviks.component.text

import graviks2d.resource.text.TextStyle
import graviks2d.target.GraviksTarget
import gruviks.component.RectangularDrawnRegion
import gruviks.component.RenderResult

class TextFieldStyle(
    val drawDefaultBackground: (target: GraviksTarget, placeholder: String) -> Triple<TextStyle, RectangularDrawnRegion, Boolean>,
    val computeDefaultResult: (renderedTextPosition: RectangularDrawnRegion?) -> RenderResult,
    val drawFocusBackground: (target: GraviksTarget, placeholder: String) -> Triple<TextStyle, RectangularDrawnRegion, Boolean>,
    val computeFocusResult: (renderedTextPosition: RectangularDrawnRegion?) -> RenderResult
)

fun transparentTextFieldStyle(
    defaultStyle: TextStyle, focusStyle: TextStyle, lineHeight: Float = 0.05f, placeholderHeight: Float = 0.4f
) = TextFieldStyle(
    drawDefaultBackground = { target, _ ->
        target.fillRect(0f, 0f, 1f, lineHeight, defaultStyle.fillColor)
        Triple(defaultStyle, RectangularDrawnRegion(0f, lineHeight, 1f, 1f - placeholderHeight / 2f), true)
    }, drawFocusBackground = { target, placeholder ->
        target.fillRect(0f, 0f, 1f, lineHeight, focusStyle.fillColor)
        val maxY = if (placeholder.isEmpty()) 1f else 1f - placeholderHeight
        if (placeholder.isNotEmpty()) target.drawString(0f, maxY, 1f, 1f, placeholder, focusStyle)
        Triple(focusStyle, RectangularDrawnRegion(0f, lineHeight, 1f, maxY), false)
    }, computeDefaultResult = { textPosition -> RenderResult(drawnRegion = RectangularDrawnRegion(
        minX = 0f, minY = 0f, maxX = 1f, maxY = textPosition?.maxY ?: lineHeight
    ), propagateMissedCursorEvents = true)}, computeFocusResult = { RenderResult(
        drawnRegion = RectangularDrawnRegion(0f, 0f, 1f, 1f), propagateMissedCursorEvents = false
    )}
)
