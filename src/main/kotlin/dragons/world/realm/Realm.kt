package dragons.world.realm

import dragons.world.chunk.Chunk
import dragons.world.chunk.ChunkLocation
import dragons.world.chunk.TemporaryChunk
import dragons.world.tile.SmallTile
import dragons.world.tile.TileProperties
import dragons.world.tile.TileState
import java.util.*

abstract class Realm(
    val id: UUID,
    val displayName: String,
    private val isInDesigner: Boolean
) {

    /**
     * Returns true if and only if `getChunk(location)` has been called at least once
     */
    abstract fun hasChunk(location: ChunkLocation): Boolean

    internal abstract fun getTemporaryChunk(location: ChunkLocation): TemporaryChunk

    /**
     * Returns the chunk at the given coordinates, creating the chunk if it doesn't exist yet
     */
    fun getChunk(location: ChunkLocation) = Chunk(location, isInDesigner) { getTemporaryChunk(location) }

    fun addTile(properties: TileProperties, initialState: TileState): SmallTile {
        val chunkLocation = ChunkLocation(properties.position)
        return getChunk(chunkLocation).addTile(properties, initialState)
    }
}
