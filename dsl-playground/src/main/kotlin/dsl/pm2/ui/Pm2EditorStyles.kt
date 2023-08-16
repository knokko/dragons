package dsl.pm2.ui

import graviks2d.resource.text.TextStyle
import graviks2d.util.Color
import gruviks.component.HorizontalComponentAlignment
import gruviks.component.VerticalComponentAlignment
import gruviks.component.text.TextButtonStyle
import gruviks.component.text.squareTextAreaStyle

internal val backgroundColor = Color.rgbInt(30, 0, 90)

internal val textAreaStyle = squareTextAreaStyle(
    defaultTextStyle = TextStyle(fillColor = Color.WHITE.scale(0.8f), font = null),
    defaultBackgroundColor = backgroundColor,
    focusTextStyle = TextStyle(fillColor = Color.WHITE, font = null),
    focusBackgroundColor = backgroundColor.scale(1.1f),
    lineHeight = 0.05f, placeholderStyle = null
)

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
