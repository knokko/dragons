package graviks2d.resource.text

import graviks2d.util.Color

class TextStyle(
    /**
     * The primary color that will be used to draw the characters.
     */
    val fillColor: Color,
    /**
     * The color of the strokes around the characters that will be drawn.
     * I wouldn't recommend using this on small text.
     */
    val strokeColor: Color = fillColor,
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
    fun createChild(
        fillColor: Color = this.fillColor,
        strokeColor: Color = if (this.fillColor == this.strokeColor) { fillColor } else { this.strokeColor },

        font: FontReference? = this.font,
        alignment: TextAlignment = this.alignment,
        overflowPolicy: TextOverflowPolicy = this.overflowPolicy
    ) = TextStyle(
        fillColor = fillColor,
        strokeColor = strokeColor,
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
