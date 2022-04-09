package graviks2d.resource.text

internal class PlacedCharacter(
    val codepoint: Int,
    val pixelWidth: Int,
    val pixelHeight: Int,
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
)

internal fun placeText(
    minX: Float, yBottom: Float, maxX: Float, yTop: Float,
    string: String, style: TextStyle
): Array<PlacedCharacter> {
    val codepoints = string.codePoints().toArray()
    TODO()
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

internal fun orderChars(original: IntArray): IntArray {
    val primaryDirection = getPrimaryDirection(original)

    val directionGroups = groupText(original, primaryDirection)

    val result = IntArray(original.size)
    if (primaryDirection != TextDirection.RightToLeft) {
        var currentIndex = 0

        for (group in directionGroups) {
            var range: IntProgression = group.startIndex until group.boundIndex
            if (group.direction == TextDirection.RightToLeft) {
                range = range.reversed()
            }

            for (index in range) {
                result[currentIndex] = original[index]
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
                currentIndex -= 1
            }
        }
    }

    // TODO Also let the result indicate which characters should be mirrored

    return result
}
