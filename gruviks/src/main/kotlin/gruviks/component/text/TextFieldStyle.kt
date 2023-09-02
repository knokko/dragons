package gruviks.component.text

import graviks2d.resource.text.TextStyle
import graviks2d.target.GraviksTarget
import graviks2d.util.Color
import gruviks.component.RectangularDrawnRegion
import gruviks.component.RenderResult

class TextFieldBackgroundResult(
    val style: TextStyle, val textRegion: RectangularDrawnRegion, val drawPlaceholder: Boolean
)

typealias TextFieldBackgroundFunction = (
    target: GraviksTarget, hasFocus: Boolean, placeholder: String, error: String,
        minSelectionX: Float?, maxSelectionX: Float?
) -> TextFieldBackgroundResult

class TextFieldStyle(
    val drawBackground: TextFieldBackgroundFunction,
    val computeResult: (renderedTextPosition: RectangularDrawnRegion?, hasFocus: Boolean) -> RenderResult,
)

fun transparentTextFieldStyle(
    defaultStyle: TextStyle, focusStyle: TextStyle, selectionBackgroundColor: Color = Color.rgbInt(100, 200, 250),
    lineHeight: Float = 0.05f, placeholderHeight: Float = 0.4f
) = TextFieldStyle(
    drawBackground = { target, hasFocus, placeholder, error, minSelectionX, maxSelectionX ->
        val originalStyle = if (hasFocus) focusStyle else defaultStyle
        var style = originalStyle
        if (error.isNotEmpty()) style = style.createChild(fillColor = Color.RED)

        target.fillRect(0f, 0f, 1f, lineHeight, originalStyle.fillColor)

        if (hasFocus) {
            val maxY = if (placeholder.isEmpty()) 1f else 1f - placeholderHeight
            if (minSelectionX != null && maxSelectionX != null) {
                target.fillRect(minSelectionX, lineHeight, maxSelectionX, maxY, selectionBackgroundColor)
            }

            if (error.isNotEmpty()) {
                target.drawString(0f, maxY, 1f, 1f, error, style)
            } else {
                target.drawString(0f, maxY, 1f, 1f, placeholder, style)
            }
            TextFieldBackgroundResult(style, RectangularDrawnRegion(0f, lineHeight, 1f, maxY), false)
        } else {
            TextFieldBackgroundResult(
                style,
                RectangularDrawnRegion(0f, lineHeight, 1f, 1f - placeholderHeight / 2f),
                true
            )
        }
    }, computeResult = { textPosition, hasFocus ->
        val maxY = if (hasFocus) 1f else textPosition?.maxY ?: lineHeight
        RenderResult(
            drawnRegion = RectangularDrawnRegion(0f, 0f, 1f, maxY),
            propagateMissedCursorEvents = true
        )
    }
)
