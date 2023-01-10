package dragons.world.tile

import dragons.space.Position
import dragons.space.shape.Shape

/**
 * Represents immutable properties of either a `SmallTile` or a `BigTile`.
 */
abstract class TileProperties(val position: Position, val shape: Shape) {

    val bounds = shape.createBoundingBox(position)

    abstract fun getPersistentClassID(): String
}
