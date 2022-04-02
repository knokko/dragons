package graviks2d.resource.text

internal class TextCacheArea(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
) {
    val width: Float
    get() = maxX - minX

    val height: Float
    get() = maxY - minY
}
