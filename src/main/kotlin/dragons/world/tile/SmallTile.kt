package dragons.world.tile

import java.util.*

/**
 * A tile that is guaranteed to be smaller than a chunk in all dimensions, and anchored in a single chunk.
 *
 * ## Implications
 * So if the anchor of a small tile is placed within the bounds of some chunk, the bounds of the tile may overlap the
 * bounds of directly adjacent chunks, but no other chunks. (Note that most small tiles will be placed entirely in
 * only 1 chunk, but small tiles close to the edge of their anchored chunk may overlap adjacent chunks.)
 */
class SmallTile(
    val id: UUID,
    val properties: TileProperties,
    val copyState: () -> TileState,
    val setState: (TileState) -> Unit
)
