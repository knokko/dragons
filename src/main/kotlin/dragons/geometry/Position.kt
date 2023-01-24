package dragons.geometry

import org.joml.Vector3f

class Position(val x: Coordinate, val y: Coordinate, val z: Coordinate) {

    override fun equals(other: Any?) = other is Position && this.x == other.x && this.y == other.y && this.z == other.z

    override fun hashCode() = x.hashCode() + 31 * y.hashCode() - 127 * z.hashCode()

    override fun toString() = "Position($x, $y, $z)"

    operator fun plus(other: Vector) = Position(x + other.x, y + other.y, z + other.z)

    operator fun minus(right: Position) = Vector(x - right.x, y - right.y, z - right.z)

    operator fun minus(right: Vector) = Position(x - right.x, y - right.y, z - right.z)

    fun distanceTo(other: Position): Distance {
        val dx = this.x - other.x
        val dy = this.y - other.y
        val dz = this.z - other.z
        return (dx * dx + dy * dy + dz * dz).squareRoot()
    }

    companion object {

        fun milliMeters(x: Long, y: Long, z: Long) = Position(
            Coordinate.milliMeters(x),
            Coordinate.milliMeters(y),
            Coordinate.milliMeters(z)
        )

        fun meters(x: Float, y: Float, z: Float) = Position(
            Coordinate.meters(x),
            Coordinate.meters(y),
            Coordinate.meters(z)
        )

        fun meters(vector: Vector3f) = meters(vector.x, vector.y, vector.z)

        fun meters(x: Int, y: Int, z: Int) = Position(
            Coordinate.meters(x),
            Coordinate.meters(y),
            Coordinate.meters(z)
        )

        fun meters(x: Long, y: Long, z: Long) = Position(
            Coordinate.meters(x),
            Coordinate.meters(y),
            Coordinate.meters(z)
        )

        fun kiloMeters(x: Int, y: Int, z: Int) = Position(
            Coordinate.kiloMeters(x),
            Coordinate.kiloMeters(y),
            Coordinate.kiloMeters(z)
        )
    }
}
