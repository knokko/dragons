package dragons.world.tile

/**
 * Represents immutable properties of either a `SmallTile` or a `BigTile`.
 */
interface TileProperties {

    fun getPersistentClassID(): String
}
