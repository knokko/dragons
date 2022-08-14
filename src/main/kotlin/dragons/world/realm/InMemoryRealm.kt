package dragons.world.realm

import dragons.world.chunk.ChunkLocation
import dragons.world.chunk.TemporaryChunk
import java.util.*

/**
 * An implementation of `Realm` where **all** chunks are stored in memory (as fields of this class). Please don't use
 * this for big realms.
 */
class InMemoryRealm(id: UUID, displayName: String, isInDesigner: Boolean) : Realm(id, displayName, isInDesigner) {

    private val allChunks = mutableMapOf<ChunkLocation, TemporaryChunk>()

    override fun hasChunk(location: ChunkLocation) = allChunks.containsKey(location)

    override fun getTemporaryChunk(location: ChunkLocation) = allChunks.computeIfAbsent(location) { TemporaryChunk() }
}
