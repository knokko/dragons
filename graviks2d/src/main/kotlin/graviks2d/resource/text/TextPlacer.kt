package graviks2d.resource.text

import kotlin.math.roundToInt

internal class PlacedCharacter(
    val codepoint: Int,
    val pixelWidth: Int,
    val pixelHeight: Int,
    val shouldMirror: Boolean,
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
)

internal fun placeText(
    minX: Float, yBottom: Float, maxX: Float, yTop: Float,
    string: String, style: TextStyle, font: StbTrueTypeFont, viewportWidth: Int, viewportHeight: Int
): List<PlacedCharacter> {
    val (orderedChars, shouldMirror, isPrimarilyLeftToRight) = orderChars(string.codePoints().toArray())

    // Good text rendering requires exact placement on pixels
    var pixelMinY = (yBottom * viewportHeight.toFloat()).roundToInt()
    var pixelBoundY = (yTop * viewportHeight.toFloat()).roundToInt()
    var finalMinY = pixelMinY.toFloat() / viewportHeight.toFloat()
    var finalMaxY = pixelBoundY.toFloat() / viewportHeight.toFloat()

    fun determineCharWidths(roundDown: Boolean) = (0 until orderedChars.size).map { index ->
        val codepoint = orderedChars[index]
        val glyphShape = font.getGlyphShape(codepoint)

        val charHeight = pixelBoundY - pixelMinY
        val shapeAspectRatio = glyphShape.advanceWidth.toFloat() / (font.ascent - font.descent).toFloat()
        val unroundedWidth = (charHeight.toFloat() * shapeAspectRatio)
        val unroundedExtra = if (index > 0) {
            val rawExtra = font.getExtraAdvance(orderedChars[index - 1], codepoint)
            unroundedWidth * (rawExtra.toFloat() / glyphShape.advanceWidth.toFloat())
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

    val startAtLeft = if (style.alignment == TextAlignment.Left) {
        true
    } else if (style.alignment == TextAlignment.Right) {
        false
    } else if (style.alignment == TextAlignment.Natural) {
        isPrimarilyLeftToRight
    } else if (style.alignment == TextAlignment.ReversedNatural) {
        !isPrimarilyLeftToRight
    } else {
        throw UnsupportedOperationException("Unsupported text alignment: ${style.alignment}")
    }

    val (pixelUsedMinX, charsToDraw) = if (totalWidth <= availableWidth) {
        if (startAtLeft) {
            Pair(pixelAvailableMinX, orderedChars.indices)
        } else {
            Pair(pixelAvailableBoundX - totalWidth, orderedChars.indices)
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

            val pixelMinX = if (startAtLeft) { pixelAvailableMinX } else { pixelAvailableBoundX - totalWidth }
            Pair(pixelMinX, orderedChars.indices)
        } else {
            val discardRight = if (style.overflowPolicy == TextOverflowPolicy.DiscardRight) {
                true
            } else if (style.overflowPolicy == TextOverflowPolicy.DiscardLeft) {
                false
            } else if (style.overflowPolicy == TextOverflowPolicy.DiscardEnd) {
                isPrimarilyLeftToRight
            } else if (style.overflowPolicy == TextOverflowPolicy.DiscardStart) {
                !isPrimarilyLeftToRight
            } else {
                throw UnsupportedOperationException("Unsupported overflow policy: ${style.overflowPolicy}")
            }

            var remainingWidth = availableWidth
            var numCharsToDraw = 0
            for ((charWidth, extraAdvance) in if (discardRight) { charWidths } else { charWidths.reversed() }) {
                val effectiveWidth = charWidth + extraAdvance
                if (remainingWidth >= effectiveWidth) {
                    remainingWidth -= effectiveWidth
                    numCharsToDraw += 1
                } else {
                    break
                }
            }

            val charsToDraw = if (discardRight) {
                0 until numCharsToDraw
            } else {
                orderedChars.size - numCharsToDraw until orderedChars.size
            }
            val reducedWidth = availableWidth - remainingWidth
            if (startAtLeft) {
                Pair(pixelAvailableMinX, charsToDraw)
            } else {
                Pair(pixelAvailableBoundX - reducedWidth, charsToDraw)
            }
        }
    }

    val placedCharacters = mutableListOf<PlacedCharacter>()
    var currentPixelMinX = pixelUsedMinX

    for (charIndex in charsToDraw) {
        val codepoint = orderedChars[charIndex]
        val glyphShape = font.getGlyphShape(codepoint)

        val charHeight = pixelBoundY - pixelMinY
        val (charWidth, extraAdvance) = charWidths[charIndex]

        currentPixelMinX += extraAdvance

        val currentPixelBoundX = currentPixelMinX + charWidth

        val currentDrawMinX = currentPixelMinX.toFloat() / viewportWidth.toFloat()
        val currentDrawMaxX = currentPixelBoundX.toFloat() / viewportWidth.toFloat()

        if (glyphShape.ttfVertices != null) {
            placedCharacters.add(PlacedCharacter(
                codepoint = codepoint,
                pixelWidth = charWidth,
                pixelHeight = charHeight,
                shouldMirror = shouldMirror[charIndex],
                minX = currentDrawMinX,
                minY = finalMinY,
                maxX = currentDrawMaxX,
                maxY = finalMaxY
            ))
        }
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

internal fun getPrimaryDirection(codepoints: IntArray): TextDirection {

    for (codepoint in codepoints) {
        val direction = getCharacterDirection(codepoint)
        if (direction == TextDirection.LeftToRight || direction == TextDirection.RightToLeft) {
            return direction
        }
    }

    return TextDirection.LeftToRight
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

internal fun orderChars(original: IntArray): Triple<IntArray, BooleanArray, Boolean> {
    val primaryDirection = getPrimaryDirection(original)

    val directionGroups = groupText(original, primaryDirection)

    val result = IntArray(original.size)
    val shouldMirror = BooleanArray(original.size)

    if (primaryDirection != TextDirection.RightToLeft) {
        var currentIndex = 0

        for (group in directionGroups) {
            var range: IntProgression = group.startIndex until group.boundIndex
            if (group.direction == TextDirection.RightToLeft) {
                range = range.reversed()
            }

            for (index in range) {
                result[currentIndex] = original[index]
                shouldMirror[currentIndex] = group.direction == TextDirection.RightToLeft && Character.isMirrored(original[index])
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
                shouldMirror[currentIndex] = group.direction == TextDirection.RightToLeft && Character.isMirrored(original[index])
                currentIndex -= 1
            }
        }
    }

    return Triple(result, shouldMirror, primaryDirection != TextDirection.RightToLeft)
}
