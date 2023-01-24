package dragons.world.chunk

import dragons.geometry.BoundingBox
import dragons.geometry.Coordinate
import dragons.geometry.Position
import dragons.world.tile.TemporaryTile
import java.util.*

/**
 * This class holds the state of a `Chunk`, but instances of this class can be removed at any time by its `Realm` and
 * replaced with a new instance that represents the same `Chunk`. Therefore, instances of this class should **not** be
 * kept by any class other than `Realm` and its subclasses!
 */
internal class TemporaryChunk {

    val tiles = mutableMapOf<UUID, TemporaryTile>()

    var bounds: BoundingBox? = null
        private set

    init {
        updateBounds()
    }

    fun updateBounds() {
        var newMinX: Coordinate? = null
        var newMinY: Coordinate? = null
        var newMinZ: Coordinate? = null

        fun updateMin(current: Coordinate?, candidate: Coordinate): Coordinate {
            return if (current == null || current > candidate) candidate else current
        }

        var newMaxX: Coordinate? = null
        var newMaxY: Coordinate? = null
        var newMaxZ: Coordinate? = null

        fun updateMax(current: Coordinate?, candidate: Coordinate): Coordinate {
            return if (current == null || current < candidate) candidate else current
        }

        for (tile in tiles.values) {
            val minTileBound = tile.properties.bounds.min
            newMinX = updateMin(newMinX, minTileBound.x)
            newMinY = updateMin(newMinY, minTileBound.y)
            newMinZ = updateMin(newMinZ, minTileBound.z)

            val maxTileBound = tile.properties.bounds.max
            newMaxX = updateMax(newMaxX, maxTileBound.x)
            newMaxY = updateMax(newMaxY, maxTileBound.y)
            newMaxZ = updateMax(newMaxZ, maxTileBound.z)
        }

        if (newMinX != null) this.bounds = BoundingBox(
            Position(newMinX, newMinY!!, newMinZ!!), Position(newMaxX!!, newMaxY!!, newMaxZ!!)
        ) else this.bounds = null
    }
}
