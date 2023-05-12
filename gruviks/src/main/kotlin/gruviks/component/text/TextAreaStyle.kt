package gruviks.component.text

import graviks2d.resource.text.TextStyle
import graviks2d.target.GraviksTarget
import graviks2d.util.Color
import gruviks.component.RectangularDrawnRegion

class TextAreaStyle(
        val defaultTextStyle: TextStyle,
        val focusTextStyle: TextStyle,
        val placeholderTextStyle: TextStyle?,
        val drawDefaultBackground: (GraviksTarget) -> Pair<RectangularDrawnRegion, Float>,
        val drawFocusBackground: (GraviksTarget) -> Pair<RectangularDrawnRegion, Float>
)

fun squareTextAreaStyle(
        defaultTextStyle: TextStyle,
        defaultBackgroundColor: Color,
        focusTextStyle: TextStyle,
        focusBackgroundColor: Color,
        lineHeight: Float,
        placeholderStyle: TextStyle?
) = TextAreaStyle(
        defaultTextStyle = defaultTextStyle,
        focusTextStyle = focusTextStyle,
        placeholderTextStyle = placeholderStyle,
        drawDefaultBackground = { target ->
            target.fillRect(0f, 0f, 1f, 1f, defaultBackgroundColor)
            Pair(RectangularDrawnRegion(0f, 0f, 1f, 1f), lineHeight)
        }, drawFocusBackground = { target ->
            target.fillRect(0f, 0f, 1f, 1f, focusBackgroundColor)
            Pair(RectangularDrawnRegion(0f, 0f, 1f, 1f), lineHeight)
        }
)