package graviks2d.resource.text

import graviks2d.util.Color

class TextStyle(
    val fillColor: Color,
    val strokeColor: Color = fillColor,
    /**
     * Choose `null` to use the default font
     */
    val font: FontReference?,
    val alignment: TextAlignment,
    val overflowPolicy: TextOverflowPolicy
) {
    fun createChild(
        fillColor: Color = this.fillColor,
        strokeColor: Color = fillColor,
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
    Right
}

enum class TextOverflowPolicy {
    DiscardStart,
    DiscardEnd,
    DiscardLeft,
    DiscardRight,
    Downscale
}
