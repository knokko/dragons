package dragons.space

import org.joml.Vector3f

class Position(val x: Distance, val y: Distance, val z: Distance) {

    override fun equals(other: Any?) = other is Position && this.x == other.x && this.y == other.y && this.z == other.z

    override fun hashCode() = x.hashCode() + 31 * y.hashCode() - 127 * z.hashCode()

    override fun toString() = "Position($x, $y, $z)"

    operator fun minus(right: Position) = Position(x - right.x, y - right.y, z - right.z)

    operator fun plus(other: Position) = Position(x + other.x, y + other.y, z + other.z)

    companion object {
        fun nanoMeters(x: Long, y: Long, z: Long) = Position(
            Distance.nanoMeters(x),
            Distance.nanoMeters(y),
            Distance.nanoMeters(z)
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
    }
}
