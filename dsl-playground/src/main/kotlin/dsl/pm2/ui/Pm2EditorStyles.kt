package dsl.pm2.ui

import graviks2d.resource.text.TextStyle
import graviks2d.util.Color
import gruviks.component.HorizontalComponentAlignment
import gruviks.component.VerticalComponentAlignment
import gruviks.component.text.TextButtonStyle

internal val backgroundColor = Color.rgbInt(30, 0, 90)

private val colorTheme = Pm2ColorTheme.TEST
internal val textAreaStyle = pm2SyntaxTextAreaStyle(colorTheme, 0.05f)

internal val tabStyle = TextButtonStyle.textAndBorder(
    baseColor = Color.WHITE.scale(0.8f),
    hoverColor = Color.WHITE,
    font = null
)

internal val fileButtonStyle = TextButtonStyle(
    baseTextStyle = TextStyle(fillColor = Color.WHITE.scale(0.8f), font = null),
    baseBackgroundColor = Color.TRANSPARENT,
    baseBorderColor = Color.TRANSPARENT,
    hoverTextStyle = TextStyle(fillColor = Color.WHITE, font = null),
    hoverBackgroundColor = Color.TRANSPARENT,
    hoverBorderColor = Color.TRANSPARENT,
    horizontalAlignment = HorizontalComponentAlignment.Left,
    verticalAlignment = VerticalComponentAlignment.Middle
)
