package gruviks.component

enum class HorizontalComponentAlignment {
    Left,
    Middle,
    Right
}

enum class VerticalComponentAlignment {
    Bottom,
    Middle,
    Top
}

fun computeBoundsX(minX: Float, maxX: Float, neededWidth: Float, alignment: HorizontalComponentAlignment): Pair<Float, Float> {
    return when (alignment) {
        HorizontalComponentAlignment.Left -> Pair(minX, minX + neededWidth)
        HorizontalComponentAlignment.Middle -> Pair(minX + 0.5f * (1f - neededWidth), maxX - 0.5f * (1f - neededWidth))
        HorizontalComponentAlignment.Right -> Pair(maxX - neededWidth, maxX)
    }
}

fun computeBoundsY(minY: Float, maxY: Float, neededHeight: Float, alignment: VerticalComponentAlignment): Pair<Float, Float> {
    val horizontalAlignment = when(alignment) {
        VerticalComponentAlignment.Bottom -> HorizontalComponentAlignment.Left
        VerticalComponentAlignment.Middle -> HorizontalComponentAlignment.Middle
        VerticalComponentAlignment.Top -> HorizontalComponentAlignment.Right
    }
    return computeBoundsX(minY, maxY, neededHeight, horizontalAlignment)
}
