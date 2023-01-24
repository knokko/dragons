package dragons.world.chunk

import dragons.geometry.Position
import java.lang.Math.floorDiv

class ChunkLocation(
    val chunkX: Int, val chunkY: Int, val chunkZ: Int
) {
    constructor(position: Position) : this(
        floorDiv(position.x.rawValue.raw, Chunk.SIZE.rawValue.raw).toInt(),
        floorDiv(position.y.rawValue.raw, Chunk.SIZE.rawValue.raw).toInt(),
        floorDiv(position.z.rawValue.raw, Chunk.SIZE.rawValue.raw).toInt()
    )

    override fun equals(other: Any?) = other is ChunkLocation
            && this.chunkX == other.chunkX && this.chunkY == other.chunkY && this.chunkZ == other.chunkZ

    override fun hashCode() = chunkX + 31 * chunkY + 731 * chunkZ

    override fun toString() = "ChunkLocation($chunkX, $chunkY, $chunkZ)"

    companion object {
        val ZERO = ChunkLocation(0, 0, 0)
    }
}
