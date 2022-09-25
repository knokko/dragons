package dragons.util

import org.joml.Vector3f

fun nextMultipleOf(factor: Long, value: Long): Long {
    return if (value % factor == 0L) {
        value
    } else {
        (value / factor + 1) * factor
    }
}

fun printFloat(value: Float): String {
    return String.format("%.2f", value)
}

fun printVector(value: Vector3f?): String {
    if (value == null) return "null"
    return "(${printFloat(value.x)}, ${printFloat(value.y)}, ${printFloat(value.z)})"
}
