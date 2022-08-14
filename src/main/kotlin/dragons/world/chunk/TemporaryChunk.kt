package dragons.world.chunk

import dragons.world.tile.TemporaryTile
import java.util.*

/**
 * This class holds the state of a `Chunk`, but instances of this class can be removed at any time by its `Realm` and
 * replaced with a new instance that represents the same `Chunk`. Therefore, instances of this class should **not** be
 * kept by any class other than `Realm` and its subclasses!
 */
internal class TemporaryChunk {

    val tiles = mutableMapOf<UUID, TemporaryTile>()
}
