package dragons.space

import org.joml.Math.sqrt
import org.joml.Vector3f

class Position(val x: Distance, val y: Distance, val z: Distance) {

    override fun equals(other: Any?) = other is Position && this.x == other.x && this.y == other.y && this.z == other.z

    override fun hashCode() = x.hashCode() + 31 * y.hashCode() - 127 * z.hashCode()

    override fun toString() = "Position($x, $y, $z)"

    operator fun minus(right: Position) = Position(x - right.x, y - right.y, z - right.z)

    operator fun plus(other: Position) = Position(x + other.x, y + other.y, z + other.z)

    operator fun times(scalar: Float) = Position(x * scalar, y * scalar, z * scalar)

    operator fun times(scalar: Long) = Position(x * scalar, y * scalar, z * scalar)

    fun distanceTo(other: Position): Distance {
        val dx = (this.x - other.x).meters
        val dy = (this.y - other.y).meters
        val dz = (this.z - other.z).meters
        return Distance.meters(sqrt(dx * dx + dy * dy + dz * dz))
    }

    companion object {
        fun nanoMeters(x: Long, y: Long, z: Long) = Position(
            Distance.nanoMeters(x),
            Distance.nanoMeters(y),
            Distance.nanoMeters(z)
        )

        fun milliMeters(x: Long, y: Long, z: Long) = Position(
            Distance.milliMeters(x),
            Distance.milliMeters(y),
            Distance.milliMeters(z)
        )

        fun meters(x: Float, y: Float, z: Float) = Position(
            Distance.meters(x),
            Distance.meters(y),
            Distance.meters(z)
        )

        fun meters(vector: Vector3f) = meters(vector.x, vector.y, vector.z)

        fun meters(x: Int, y: Int, z: Int) = Position(
            Distance.meters(x),
            Distance.meters(y),
            Distance.meters(z)
        )

        fun meters(x: Long, y: Long, z: Long) = Position(
            Distance.meters(x),
            Distance.meters(y),
            Distance.meters(z)
        )

        fun kiloMeters(x: Long, y: Long, z: Long) = Position(
            Distance.kiloMeters(x),
            Distance.kiloMeters(y),
            Distance.kiloMeters(z)
        )
    }
}
