package dragons.util

import org.joml.Vector3f

fun nextMultipleOf(factor: Long, value: Long): Long {
    return if (value % factor == 0L) {
        value
    } else {
        (value / factor + 1) * factor
    }
}

fun nextPowerOf2(value: Long): Long {
    if (value < 0) throw IllegalArgumentException("Value ($value) must be non-negative")

    return when (val oneBit = java.lang.Long.highestOneBit(value)) {
        0L -> 1
        value -> value
        else -> oneBit shl 1
    }
}

fun printFloat(value: Float): String {
    return String.format("%.2f", value)
}

fun printVector(value: Vector3f?): String {
    if (value == null) return "null"
    return "(${printFloat(value.x)}, ${printFloat(value.y)}, ${printFloat(value.z)})"
}

fun <T: Comparable<T>> min(a: T, b: T): T {
    return if (a <= b) a else b
}

fun <T: Comparable<T>> max(a: T, b: T): T {
    return if (a >= b) a else b
}
