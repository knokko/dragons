package gruviks.component.text

import graviks2d.resource.text.TextStyle
import graviks2d.target.GraviksTarget
import graviks2d.util.Color
import gruviks.component.RectangularDrawnRegion

typealias TextInputFunction = (allLines: List<String>) -> ((lineIndex: Int) -> TextStyle)

class TextAreaStyle(
        val defaultTextStyle: TextInputFunction,
        val focusTextStyle: TextInputFunction,
        val placeholderTextStyle: TextInputFunction?,
        val determineTextRegionAndLineHeight: (
                target: GraviksTarget, hasFocus: Boolean, isPlaceholder: Boolean
        ) -> Pair<RectangularDrawnRegion, Float>,
        val drawBackgroundAndDecorations: (
                target: GraviksTarget, lines: List<LineToDraw>, hasFocus: Boolean, isPlaceholder: Boolean
        ) -> Unit
)

fun squareTextAreaStyle(
        defaultTextStyle: TextStyle,
        defaultBackgroundColor: Color,
        focusTextStyle: TextStyle,
        focusBackgroundColor: Color,
        lineHeight: Float,
        placeholderStyle: TextStyle?,
        selectionBackgroundColor: Color
) = TextAreaStyle(
        defaultTextStyle = { { defaultTextStyle }},
        focusTextStyle = { { focusTextStyle }},
        placeholderTextStyle = if (placeholderStyle == null) null else { _ -> { placeholderStyle }},
        determineTextRegionAndLineHeight = { _, _, _ ->
                Pair(RectangularDrawnRegion(0f, 0f, 1f, 1f), lineHeight)
        },
        drawBackgroundAndDecorations = { target, lines, hasFocus, _ ->
                val backgroundColor = if (hasFocus) focusBackgroundColor else defaultBackgroundColor
                target.fillRect(0f, 0f, 1f, 1f, backgroundColor)
                for (line in lines) {
                    if (line.minSelectionX != null && line.maxSelectionX != null) {
                        target.fillRect(
                            line.minSelectionX, line.minY, line.maxSelectionX, line.maxY,
                            selectionBackgroundColor
                        )
                    }
                }
        },
)
