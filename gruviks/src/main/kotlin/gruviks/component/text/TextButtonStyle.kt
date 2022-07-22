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
    val horizontalAlignment: HorizontalComponentAlignment,
    val verticalAlignment: VerticalComponentAlignment
) {
    companion object {
        fun textAndBorder(
            baseColor: Color, hoverColor: Color, font: FontReference? = null,
            horizontalAlignment: HorizontalComponentAlignment = HorizontalComponentAlignment.Middle,
            verticalAlignment: VerticalComponentAlignment = VerticalComponentAlignment.Middle
        ) = TextButtonStyle(
            baseTextStyle = TextStyle(fillColor = baseColor, font = font),
            baseBackgroundColor = Color.rgbaInt(0, 0, 0, 0),
            baseBorderColor = baseColor,
            hoverTextStyle = TextStyle(fillColor = hoverColor, font = font),
            hoverBackgroundColor = Color.rgbaInt(0, 0, 0, 0),
            hoverBorderColor = hoverColor,
            horizontalAlignment = horizontalAlignment,
            verticalAlignment = verticalAlignment
        )
    }
}
