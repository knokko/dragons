package dragons.world.tile

import dragons.space.Position

/**
 * Represents immutable properties of either a `SmallTile` or a `BigTile`.
 */
abstract class TileProperties(val position: Position) {

    abstract fun getPersistentClassID(): String
}
