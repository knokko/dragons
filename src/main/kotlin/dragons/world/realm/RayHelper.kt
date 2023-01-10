package dragons.world.realm

import dragons.space.BoundingBox
import dragons.world.chunk.ChunkLocation

internal fun getPotentiallyIntersectingChunks(bounds: BoundingBox): Array<ChunkLocation> {
    val chunkMin = ChunkLocation(bounds.min)
    val chunkMax = ChunkLocation(bounds.max)

    val minChunkX = chunkMin.chunkX - 1
    val minChunkY = chunkMin.chunkY - 1
    val minChunkZ = chunkMin.chunkZ - 1
    val maxChunkX = chunkMax.chunkX + 1
    val maxChunkY = chunkMax.chunkY + 1
    val maxChunkZ = chunkMax.chunkZ + 1

    val numChunks = (1 + maxChunkX - minChunkX) * (1 + maxChunkY - minChunkY) * (1 + maxChunkZ - minChunkZ)

    val chunks = Array(numChunks) { ChunkLocation.ZERO }
    var nextIndex = 0

    for (z in minChunkZ .. maxChunkZ) {
        for (y in minChunkY .. maxChunkY) {
            for (x in minChunkX .. maxChunkX) {
                chunks[nextIndex++] = ChunkLocation(x, y, z)
            }
        }
    }

    return chunks
}
