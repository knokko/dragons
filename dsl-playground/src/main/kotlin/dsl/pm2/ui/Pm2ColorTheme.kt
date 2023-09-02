package dsl.pm2.ui

import graviks2d.resource.text.FontReference
import graviks2d.util.Color

class Pm2ColorTheme(
    val defaultText: Color,
    val background: Color,
    val selectionBackground: Color,
    val error: Color,
    val keyword: Color,
    val functionDeclaration: Color,
    val builtinFunctionCall: Color,
    val builtinTypeName: Color,
    val customTypeName: Color,
    val floatLiteral: Color,
    val intLiteral: Color,
    val stringLiteral: Color,
    val font: FontReference? = null
) {
    companion object {
        val TEST = Pm2ColorTheme(
            defaultText = Color.WHITE,
            background = Color.rgbInt(30, 0, 90),
            selectionBackground = Color.rgbInt(0, 50, 150),
            error = Color.RED,
            keyword = Color.rgbInt(250, 0, 200),
            functionDeclaration = Color.rgbInt(250, 100, 0),
            builtinFunctionCall = Color.rgbInt(0, 150, 250),
            builtinTypeName = Color.rgbInt(0, 200, 200),
            customTypeName = Color.rgbInt(150, 150, 250),
            floatLiteral = Color.rgbInt(0, 250, 50),
            intLiteral = Color.rgbInt(50, 200, 100),
            stringLiteral = Color.rgbInt(0, 150, 50)
        )
    }
}
