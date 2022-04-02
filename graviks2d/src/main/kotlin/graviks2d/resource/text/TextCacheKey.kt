package graviks2d.resource.text

internal class TextCacheKey(
    val codepoint: Int,
    val width: Int,
    val height: Int
) {
    override fun equals(other: Any?): Boolean {
        return if (other is TextCacheKey) {
            this.codepoint == other.codepoint && this.width == other.width &&
                    this.height == other.height
        } else {
            false
        }
    }

    override fun hashCode() = 7 * this.codepoint + 83 * this.width + 791 * this.height
}
