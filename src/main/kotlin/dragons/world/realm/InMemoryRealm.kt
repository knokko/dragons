package dragons.world.realm

import dragons.geometry.BoundingBox
import dragons.world.chunk.ChunkLocation
import dragons.world.chunk.TemporaryChunk
import dragons.world.entity.Entity
import dragons.world.entity.EntityProperties
import dragons.world.entity.EntityState
import dragons.world.entity.TemporaryEntity
import java.util.*

/**
 * An implementation of `Realm` where **all** chunks are stored in memory (as fields of this class). Please don't use
 * this for big realms.
 */
class InMemoryRealm(id: UUID, displayName: String, isInDesigner: Boolean) : Realm(id, displayName, isInDesigner) {

    private val allChunks = mutableMapOf<ChunkLocation, TemporaryChunk>()
    private val allEntities = mutableMapOf<UUID, TemporaryEntity>()

    override fun hasChunk(location: ChunkLocation) = allChunks.containsKey(location)

    override fun getTemporaryChunk(location: ChunkLocation) = allChunks.computeIfAbsent(location) { TemporaryChunk() }

    override fun hasEntity(id: UUID) = allEntities.containsKey(id)

    override fun getTemporaryEntity(id: UUID) = allEntities[id]?: throw IllegalArgumentException("No entity has ID $id")

    override fun addEntity(properties: EntityProperties, initialState: EntityState): Entity {
        val tempEntity = TemporaryEntity(UUID.randomUUID(), properties, initialState)
        allEntities[tempEntity.id] = tempEntity
        return getEntity(tempEntity.id)
    }

    override fun queryEntityIDsBetween(bounds: BoundingBox): Collection<UUID> {
        return allEntities.filter { bounds.contains(it.value.state.position) }.map { it.key }
    }
}
