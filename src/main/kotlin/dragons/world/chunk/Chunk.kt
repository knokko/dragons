package dragons.world.chunk

import dragons.space.Distance
import dragons.world.tile.SmallTile
import dragons.world.tile.TemporaryTile
import dragons.world.tile.TileProperties
import dragons.world.tile.TileState
import java.util.*

class Chunk internal constructor(
    val location: ChunkLocation,
    private val isInDesigner: Boolean,
    internal val getTemporary: () -> TemporaryChunk
) {

    private fun getTemporaryTile(tileID: UUID): TemporaryTile {
        return getTemporary().tiles[tileID] ?: throw IllegalStateException("This tile has been deleted")
    }

    private fun constructStableTile(temporaryTile: TemporaryTile): SmallTile {
        val id = temporaryTile.id
        return SmallTile(
            id, temporaryTile.properties, { getTemporaryTile(id).state }, { newState -> getTemporaryTile(id).state = newState }
        )
    }

    fun getAllTiles(): Collection<SmallTile> {
        return getTemporary().tiles.values.map(::constructStableTile)
    }

    fun getTileState(id: UUID) = getTemporaryTile(id).state

    internal fun addTile(properties: TileProperties, initialState: TileState): SmallTile {
        if (!isInDesigner) throw UnsupportedOperationException("Adding tiles is only allowed in the designer")

        val temporaryTile = TemporaryTile(UUID.randomUUID(), properties, initialState)
        getTemporary().tiles[temporaryTile.id] = temporaryTile
        return constructStableTile(temporaryTile)
    }

    fun removeTile(tile: SmallTile) {
        if (!isInDesigner) throw UnsupportedOperationException("Removing tiles is only allowed in the designer")
        if (getTemporary().tiles.remove(tile.id) == null) throw IllegalArgumentException("This tile didn't exist")
    }

    companion object {
        val SIZE = Distance.meters(100)
    }
}