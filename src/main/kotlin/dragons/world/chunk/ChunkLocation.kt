package dragons.world.chunk

class ChunkLocation(
    val chunkX: Int, val chunkY: Int, val chunkZ: Int
) {
    override fun equals(other: Any?) = other is ChunkLocation
            && this.chunkX == other.chunkX && this.chunkY == other.chunkY && this.chunkZ == other.chunkZ

    override fun hashCode() = chunkX + 31 * chunkY + 731 * chunkZ

    override fun toString() = "ChunkLocation($chunkX, $chunkY, $chunkZ)"
}
