package dragons.util

fun nextMultipleOf(factor: Long, value: Long): Long {
    return if (value % factor == 0L) {
        value
    } else {
        (value / factor + 1) * factor
    }
}
