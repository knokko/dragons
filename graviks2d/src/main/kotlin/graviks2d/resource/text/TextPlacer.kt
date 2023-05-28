package graviks2d.resource.text

import java.lang.Integer.max
import kotlin.math.roundToInt

internal class PlacedCharacter(
    val codepoint: Int,
    val pixelWidth: Int,
    val pixelHeight: Int,
    val shouldMirror: Boolean,
    val position: CharacterPosition,
    val originalIndex: Int
) {
    override fun toString() = "PlacedCharacter(codepoint=$codepoint, size=($pixelWidth, $pixelHeight)"
}

class CharacterPosition(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float,
    val isLeftToRight: Boolean
)

internal fun placeText(
    minX: Float, yBottom: Float, maxX: Float, yTop: Float,
    string: String, style: TextStyle, font: StbTrueTypeFont, viewportWidth: Int, viewportHeight: Int,
    suggestLeftToRight: Boolean
): List<PlacedCharacter> {
    val orderedChars = orderChars(string.codePoints().toArray(), suggestLeftToRight)

    // Good text rendering requires exact placement on pixels
    var pixelMinY = (yBottom * viewportHeight.toFloat()).roundToInt()
    var pixelBoundY = (yTop * viewportHeight.toFloat()).roundToInt()
    var finalMinY = pixelMinY.toFloat() / viewportHeight.toFloat()
    var finalMaxY = pixelBoundY.toFloat() / viewportHeight.toFloat()

    fun determineCharWidths(roundDown: Boolean) = orderedChars.chars.indices.map { index ->
        val codepoint = orderedChars.chars[index]
        val advanceWidth = font.getAdvanceWidth(codepoint)

        val charHeight = pixelBoundY - pixelMinY
        val scale = charHeight.toFloat() / (font.ascent - font.descent)
        val unroundedWidth = advanceWidth.toFloat() * scale
        val unroundedExtra = if (index > 0) {
            font.getExtraAdvance(orderedChars.chars[index - 1], codepoint) * scale
        } else {
            0f
        }
        if (roundDown) {
            Pair(unroundedWidth.toInt(), unroundedExtra.toInt())
        } else {
            Pair(unroundedWidth.roundToInt(), unroundedExtra.roundToInt())
        }
    }

    var charWidths = determineCharWidths(false)

    var totalWidth = charWidths.sumOf { it.first + it.second }

    val pixelAvailableMinX = (minX * viewportWidth.toFloat()).roundToInt()
    val pixelAvailableBoundX = (maxX * viewportWidth.toFloat()).roundToInt()
    val availableWidth = pixelAvailableBoundX - pixelAvailableMinX

    val textPosition = if (style.alignment == TextAlignment.Left) {
        -1
    } else if (style.alignment == TextAlignment.Right) {
        1
    } else if (style.alignment == TextAlignment.Natural) {
        if (orderedChars.isPrimarilyLeftToRight) -1 else 1
    } else if (style.alignment == TextAlignment.ReversedNatural) {
        if (orderedChars.isPrimarilyLeftToRight) 1 else -1
    } else if (style.alignment == TextAlignment.Centered) {
        0
    } else {
        throw UnsupportedOperationException("Unsupported text alignment: ${style.alignment}")
    }

    val (pixelUsedMinX, charsToDraw) = if (totalWidth <= availableWidth) {
        if (textPosition == -1) {
            Pair(pixelAvailableMinX, orderedChars.chars.indices)
        } else if (textPosition == 1) {
            Pair(pixelAvailableBoundX - totalWidth, orderedChars.chars.indices)
        } else {
            Pair(pixelAvailableMinX + (availableWidth - totalWidth) / 2, orderedChars.chars.indices)
        }
    } else {
        if (style.overflowPolicy == TextOverflowPolicy.Downscale) {

            val downscaleFactor = availableWidth.toFloat() / totalWidth.toFloat()
            val deltaY = yTop - yBottom
            pixelMinY = ((yBottom + 0.5f * (1f - downscaleFactor) * deltaY) * viewportHeight.toFloat()).roundToInt()
            pixelBoundY = ((yTop - 0.5f * (1f - downscaleFactor) * deltaY) * viewportHeight.toFloat()).roundToInt()
            finalMinY = pixelMinY.toFloat() / viewportHeight.toFloat()
            finalMaxY = pixelBoundY.toFloat() / viewportHeight.toFloat()

            charWidths = determineCharWidths(true)
            totalWidth = charWidths.sumOf { it.first + it.second }

            val pixelMinX = if (textPosition <= 0) { pixelAvailableMinX } else { pixelAvailableBoundX - totalWidth }
            Pair(pixelMinX, orderedChars.chars.indices)
        } else {
            val discardRight = if (style.overflowPolicy == TextOverflowPolicy.DiscardRight) {
                true
            } else if (style.overflowPolicy == TextOverflowPolicy.DiscardLeft) {
                false
            } else if (style.overflowPolicy == TextOverflowPolicy.DiscardEnd) {
                orderedChars.isPrimarilyLeftToRight
            } else if (style.overflowPolicy == TextOverflowPolicy.DiscardStart) {
                !orderedChars.isPrimarilyLeftToRight
            } else {
                throw UnsupportedOperationException("Unsupported overflow policy: ${style.overflowPolicy}")
            }

            var remainingWidth = availableWidth
            var numCharsToDraw = 0
            for ((charWidth, extraAdvance) in if (discardRight) { charWidths } else { charWidths.reversed() }) {
                val effectiveWidth = charWidth + extraAdvance
                if (remainingWidth >= 0) {
                    remainingWidth -= effectiveWidth
                    numCharsToDraw += 1
                } else {
                    break
                }
            }

            val charsToDraw = if (discardRight) {
                0 until numCharsToDraw
            } else {
                orderedChars.chars.size - numCharsToDraw until orderedChars.chars.size
            }
            val reducedWidth = availableWidth - max(0, remainingWidth)
            if (textPosition == -1) {
                Pair(pixelAvailableMinX, charsToDraw)
            } else if (textPosition == 1) {
                Pair(pixelAvailableBoundX - reducedWidth, charsToDraw)
            } else {
                Pair((pixelAvailableMinX + pixelAvailableBoundX - reducedWidth) / 2, charsToDraw)
            }
        }
    }

    val placedCharacters = mutableListOf<PlacedCharacter>()
    var currentPixelMinX = pixelUsedMinX

    for (charIndex in charsToDraw) {
        val codepoint = orderedChars.chars[charIndex]
        val charHeight = pixelBoundY - pixelMinY
        val (charWidth, extraAdvance) = charWidths[charIndex]

        currentPixelMinX += extraAdvance

        val currentPixelBoundX = currentPixelMinX + charWidth

        val currentDrawMinX = currentPixelMinX.toFloat() / viewportWidth.toFloat()
        val currentDrawMaxX = currentPixelBoundX.toFloat() / viewportWidth.toFloat()

        placedCharacters.add(PlacedCharacter(
            codepoint = codepoint,
            pixelWidth = charWidth,
            pixelHeight = charHeight,
            shouldMirror = orderedChars.shouldMirror[charIndex],
            position = CharacterPosition(
                minX = currentDrawMinX,
                minY = finalMinY,
                maxX = currentDrawMaxX,
                maxY = finalMaxY,
                isLeftToRight = orderedChars.isLeftToRight[charIndex]
            ),
            originalIndex = orderedChars.originalIndices[charIndex]
        ))
        currentPixelMinX = currentPixelBoundX
    }

    return placedCharacters
}

internal enum class TextDirection {
    LeftToRight,
    RightToLeft,
    // Numbers are always left-to-right, but do need special treatment in some cases
    Number,
    Neutral
}

private val STRONG_LEFT_TO_RIGHT_DIRECTIONS = arrayOf(
    Character.DIRECTIONALITY_LEFT_TO_RIGHT,
    Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE
)

private val NUMBER_DIRECTIONS = arrayOf(
    Character.DIRECTIONALITY_EUROPEAN_NUMBER,
    Character.DIRECTIONALITY_ARABIC_NUMBER
)

private val RIGHT_TO_LEFT_DIRECTIONS = arrayOf(
    Character.DIRECTIONALITY_RIGHT_TO_LEFT,
    Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
)

private fun getCharacterDirection(codepoint: Int): TextDirection {
    val direction = Character.getDirectionality(codepoint)
    if (STRONG_LEFT_TO_RIGHT_DIRECTIONS.contains(direction)) return TextDirection.LeftToRight
    if (RIGHT_TO_LEFT_DIRECTIONS.contains(direction)) return TextDirection.RightToLeft
    if (NUMBER_DIRECTIONS.contains(direction)) return TextDirection.Number
    return TextDirection.Neutral
}

internal fun getPrimaryDirection(codepoints: IntArray, suggestLeftToRight: Boolean): TextDirection {

    for (codepoint in codepoints) {
        val direction = getCharacterDirection(codepoint)
        if (direction == TextDirection.LeftToRight || direction == TextDirection.RightToLeft) {
            return direction
        }
    }

    return if (suggestLeftToRight) TextDirection.LeftToRight else TextDirection.RightToLeft
}

internal class DirectionGroup(
    val startIndex: Int,
    val boundIndex: Int,
    val direction: TextDirection
) {
    init {
        if (direction == TextDirection.Neutral) {
            throw IllegalArgumentException("Text direction can't be neutral (range is $startIndex until $boundIndex)")
        }
        if (startIndex >= boundIndex) {
            throw IllegalArgumentException("Start index ($startIndex) must be smaller than bound index ($boundIndex) (direction is $direction)")
        }
    }

    override fun toString(): String {
        return "Direction($startIndex until $boundIndex, $direction)"
    }

    override fun equals(other: Any?): Boolean {
        return other is DirectionGroup && this.startIndex == other.startIndex
                && this.boundIndex == other.boundIndex && this.direction == other.direction
    }

    override fun hashCode(): Int {
        var result = startIndex
        result = 31 * result + boundIndex
        result = 31 * result + direction.hashCode()
        return result
    }
}

internal fun groupText(
    codepoints: IntArray, primaryDirection: TextDirection
): List<DirectionGroup> {

    val groups = mutableListOf<DirectionGroup>()

    var oldDirection = primaryDirection
    var groupStartIndex = 0
    var lastStrongIndex = 0

    for ((currentIndex, codepoint) in codepoints.withIndex()) {
        var currentDirection = getCharacterDirection(codepoint)
        if (currentDirection == TextDirection.Neutral && oldDirection == TextDirection.Number) {
            currentDirection = primaryDirection
        }

        if (currentDirection != TextDirection.Neutral) {

            // When directionality changes, a new group needs to be made
            if (currentDirection != oldDirection) {

                if (oldDirection == primaryDirection) {
                    if (groupStartIndex < currentIndex) {
                        groups.add(
                            DirectionGroup(
                                startIndex = groupStartIndex,
                                boundIndex = currentIndex,
                                direction = oldDirection
                            )
                        )
                        groupStartIndex = currentIndex
                    }
                } else {
                    if (groupStartIndex < lastStrongIndex + 1) {
                        groups.add(
                            DirectionGroup(
                                startIndex = groupStartIndex,
                                boundIndex = lastStrongIndex + 1,
                                direction = oldDirection
                            )
                        )
                        groupStartIndex = lastStrongIndex + 1
                    }
                }

                oldDirection = currentDirection
            }

            lastStrongIndex = currentIndex
        }
    }

    if (groupStartIndex < codepoints.size) {

        if (oldDirection == primaryDirection) {
            groups.add(
                DirectionGroup(
                    startIndex = groupStartIndex,
                    boundIndex = codepoints.size,
                    direction = primaryDirection
                )
            )
        } else {
            groups.add(
                DirectionGroup(
                    startIndex = groupStartIndex,
                    boundIndex = lastStrongIndex + 1,
                    direction = oldDirection
                )
            )
            if (lastStrongIndex + 1 < codepoints.size) {
                groups.add(
                    DirectionGroup(
                        startIndex = lastStrongIndex + 1,
                        boundIndex = codepoints.size,
                        direction = primaryDirection
                    )
                )
            }
        }
    }

    return groups
}

internal class OrderedChars(
    val chars: IntArray,
    val originalIndices: IntArray,
    val shouldMirror: BooleanArray,
    val isLeftToRight: BooleanArray,
    val isPrimarilyLeftToRight: Boolean
)

internal fun orderChars(original: IntArray, suggestLeftToRight: Boolean): OrderedChars {
    val primaryDirection = getPrimaryDirection(original, suggestLeftToRight)

    val directionGroups = groupText(original, primaryDirection)

    val result = IntArray(original.size)
    val originalIndices = IntArray(original.size)
    val shouldMirror = BooleanArray(original.size)
    val isLeftToRight = BooleanArray(original.size)

    if (primaryDirection != TextDirection.RightToLeft) {
        var currentIndex = 0

        for (group in directionGroups) {
            var range: IntProgression = group.startIndex until group.boundIndex
            if (group.direction == TextDirection.RightToLeft) {
                range = range.reversed()
            }

            for (index in range) {
                result[currentIndex] = original[index]
                originalIndices[currentIndex] = index
                shouldMirror[currentIndex] = group.direction == TextDirection.RightToLeft && Character.isMirrored(original[index])
                isLeftToRight[currentIndex] = group.direction != TextDirection.RightToLeft
                currentIndex += 1
            }
        }
    } else {
        var currentIndex = original.size - 1

        for (group in directionGroups) {
            var range: IntProgression = group.startIndex until group.boundIndex
            if (group.direction != TextDirection.RightToLeft) {
                range = range.reversed()
            }

            for (index in range) {
                result[currentIndex] = original[index]
                originalIndices[currentIndex] = index
                shouldMirror[currentIndex] = group.direction == TextDirection.RightToLeft && Character.isMirrored(original[index])
                isLeftToRight[currentIndex] = group.direction != TextDirection.RightToLeft
                currentIndex -= 1
            }
        }
    }

    return OrderedChars(result, originalIndices, shouldMirror, isLeftToRight, primaryDirection != TextDirection.RightToLeft)
}
