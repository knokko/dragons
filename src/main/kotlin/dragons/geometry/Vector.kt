package dragons.geometry

import org.joml.Vector3f

class Vector(val x: Distance, val y: Distance, val z: Distance) {

    override fun equals(other: Any?) = other is Vector && this.x == other.x && this.y == other.y && this.z == other.z

    override fun hashCode() = x.hashCode() + 31 * y.hashCode() - 127 * z.hashCode()

    override fun toString() = "Vector($x, $y, $z)"

    operator fun plus(other: Vector) = Vector(x + other.x, y + other.y, z + other.z)

    operator fun plus(other: Position) = Position(x + other.x, y + other.y, z + other.z)

    operator fun minus(right: Vector) = Vector(x - right.x, y - right.y, z - right.z)

    operator fun times(scalar: Int) = Vector(x * scalar, y * scalar, z * scalar)

    operator fun times(scalar: Long) = Vector(x * scalar, y * scalar, z * scalar)

    operator fun times(scalar: Float) = Vector(x * scalar, y * scalar, z * scalar)

    operator fun times(scalar: Double) = Vector(x * scalar, y * scalar, z * scalar)

    operator fun div(scalar: Int) = Vector(x / scalar, y / scalar, z / scalar)

    operator fun div(scalar: Long) = Vector(x / scalar, y / scalar, z / scalar)

    operator fun div(scalar: Float) = Vector(x / scalar, y / scalar, z / scalar)

    operator fun div(scalar: Double) = Vector(x / scalar, y / scalar, z / scalar)

    companion object {

        fun milliMeters(x: Long, y: Long, z: Long) = Vector(
            Distance.milliMeters(x),
            Distance.milliMeters(y),
            Distance.milliMeters(z)
        )

        fun meters(x: Float, y: Float, z: Float) = Vector(
            Distance.meters(x),
            Distance.meters(y),
            Distance.meters(z)
        )

        fun meters(vector: Vector3f) = meters(vector.x, vector.y, vector.z)

        fun meters(x: Int, y: Int, z: Int) = Vector(
            Distance.meters(x),
            Distance.meters(y),
            Distance.meters(z)
        )

        fun meters(x: Long, y: Long, z: Long) = Vector(
            Distance.meters(x),
            Distance.meters(y),
            Distance.meters(z)
        )
    }
}
