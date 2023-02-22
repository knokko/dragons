package gruviks.component.text

import graviks2d.resource.text.FontReference
import graviks2d.resource.text.TextStyle
import graviks2d.util.Color
import gruviks.component.HorizontalComponentAlignment
import gruviks.component.VerticalComponentAlignment

class TextButtonStyle(
    val baseTextStyle: TextStyle,
    val baseBackgroundColor: Color,
    val baseBorderColor: Color,
    val hoverTextStyle: TextStyle,
    val hoverBackgroundColor: Color,
    val hoverBorderColor: Color,
    val lineWidth: Float = 0.15f,
    val iconHeight: Float = 0.8f,
    val horizontalAlignment: HorizontalComponentAlignment,
    val verticalAlignment: VerticalComponentAlignment
) {
    companion object {
        fun textAndBorder(
            baseColor: Color, hoverColor: Color, font: FontReference? = null,
            lineWidth: Float = 0.15f, iconHeight: Float = 0.8f,
            horizontalAlignment: HorizontalComponentAlignment = HorizontalComponentAlignment.Middle,
            verticalAlignment: VerticalComponentAlignment = VerticalComponentAlignment.Middle
        ) = TextButtonStyle(
            baseTextStyle = TextStyle(fillColor = baseColor, font = font),
            baseBackgroundColor = Color.TRANSPARENT,
            baseBorderColor = baseColor,
            hoverTextStyle = TextStyle(fillColor = hoverColor, font = font),
            hoverBackgroundColor = Color.TRANSPARENT,
            hoverBorderColor = hoverColor,
            lineWidth = lineWidth,
            iconHeight = iconHeight,
            horizontalAlignment = horizontalAlignment,
            verticalAlignment = verticalAlignment
        )
    }
}
