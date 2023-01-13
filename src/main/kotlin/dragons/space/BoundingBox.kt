package dragons.space

import dragons.util.max
import dragons.util.min

class BoundingBox(corner1: Position, corner2: Position) {

    val min: Position
    val max: Position

    init {
        val minX = min(corner1.x, corner2.x)
        val minY = min(corner1.y, corner2.y)
        val minZ = min(corner1.z, corner2.z)
        val maxX = max(corner1.x, corner2.x)
        val maxY = max(corner1.y, corner2.y)
        val maxZ = max(corner1.z, corner2.z)

        this.min = Position(minX, minY, minZ)
        this.max = Position(maxX, maxY, maxZ)
    }

    fun contains(position: Position) = min.x <= position.x && position.x <= max.x &&
            min.y <= position.y && position.y <= max.y && min.z <= position.z && position.z <= max.z

    fun intersects(other: BoundingBox) = min.x <= other.max.x && other.min.x <= max.x &&
            min.y <= other.max.y && other.min.y <= max.y && min.z <= other.max.z && other.min.z <= max.z

    override fun toString() = "BoundingBox($min, $max)"
}
