package dragons.world.chunk

import dragons.space.Position
import java.lang.Math.floorDiv

class ChunkLocation(
    val chunkX: Int, val chunkY: Int, val chunkZ: Int
) {
    constructor(position: Position) : this(
        floorDiv(position.x.nanoMetersInt, Chunk.SIZE.nanoMetersInt).toInt(),
        floorDiv(position.y.nanoMetersInt, Chunk.SIZE.nanoMetersInt).toInt(),
        floorDiv(position.z.nanoMetersInt, Chunk.SIZE.nanoMetersInt).toInt()
    )

    override fun equals(other: Any?) = other is ChunkLocation
            && this.chunkX == other.chunkX && this.chunkY == other.chunkY && this.chunkZ == other.chunkZ

    override fun hashCode() = chunkX + 31 * chunkY + 731 * chunkZ

    override fun toString() = "ChunkLocation($chunkX, $chunkY, $chunkZ)"

    companion object {
        val ZERO = ChunkLocation(0, 0, 0)
    }
}
