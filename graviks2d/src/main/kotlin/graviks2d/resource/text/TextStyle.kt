package graviks2d.resource.text

import graviks2d.util.Color

class TextStyle(
    /**
     * The primary color that will be used to draw the characters.
     */
    val fillColor: Color,
    /**
     * The primary color that will be used to draw each character. When null, `fillColor` will be used for
     * all characters.
     * This function can be used to give each character a different color.
     */
    val fillColorFunction: ((charIndex: Int) -> Color)? = null,
    /**
     * The color of the strokes around the characters that will be drawn. When null, the stroke color will be
     * the same as the fill color.
     * I wouldn't recommend using this on small text.
     */
    val strokeColor: Color? = null,
    /**
     * The color of the strokes around each character that will be drawn. When null, `strokeColor` will be used for
     * all characters.
     * This function can be used to give each character a different stroke color.
     */
    val strokeColorFunction: ((charIndex: Int) -> Color)? = null,
    /**
     * This parameter can be used to determine how fat the strokes around the
     * characters will be. The default value is 0.01. Increasing this value will
     * make the strokes thicker.
     */
    val strokeHeightFraction: Float = if (fillColor != strokeColor) { 0.01f } else { 0f },
    /**
     * Choose `null` to use the default font
     */
    val font: FontReference?,
    val alignment: TextAlignment = TextAlignment.Natural,
    val overflowPolicy: TextOverflowPolicy = TextOverflowPolicy.DiscardEnd
) {
    fun getFillColor(charIndex: Int) = fillColorFunction?.invoke(charIndex) ?: fillColor

    fun getStrokeColor(charIndex: Int) = strokeColorFunction?.invoke(charIndex) ?: strokeColor ?: getFillColor(charIndex)

    fun createChild(
        fillColor: Color = this.fillColor,
        strokeColor: Color? = this.strokeColor,
        fillColorFunction: ((charIndex: Int) -> Color)? = this.fillColorFunction,
        strokeColorFunction: ((charIndex: Int) -> Color)? = null,
        font: FontReference? = this.font,
        alignment: TextAlignment = this.alignment,
        overflowPolicy: TextOverflowPolicy = this.overflowPolicy
    ) = TextStyle(
        fillColor = fillColor,
        fillColorFunction = fillColorFunction,
        strokeColor = strokeColor,
        strokeColorFunction = strokeColorFunction,
        font = font,
        alignment = alignment,
        overflowPolicy = overflowPolicy
    )
}

enum class TextAlignment {
    /**
     * The 'natural' text alignment. In English, text will be aligned to the left. In Arabic, text will be aligned to
     * the right.
     */
    Natural,

    /**
     * The opposite of the 'natural' text alignment. In English, text will be aligned to the right. In Arabic, text
     * will be aligned to the left.
     */
    ReversedNatural,

    /**
     * The text will be aligned to the left, regardless of the language.
     */
    Left,

    /**
     * The text will be aligned to the right, regardless of the language.
     */
    Right,

    /**
     * The text will be centered
     */
    Centered
}

enum class TextOverflowPolicy {
    DiscardStart,
    DiscardEnd,
    DiscardLeft,
    DiscardRight,
    Downscale
}
