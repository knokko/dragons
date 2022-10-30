package dragons.world.realm

import dragons.space.Position
import dragons.util.max
import dragons.util.min
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

    override fun queryEntityIDsBetween(a: Position, b: Position): Collection<UUID> {
        val minX = min(a.x, b.x)
        val minY = min(a.y, b.y)
        val minZ = min(a.z, b.z)
        val maxX = max(a.x, b.x)
        val maxY = max(a.y, b.y)
        val maxZ = max(a.z, b.z)

        return allEntities.filter {
            val position = it.value.state.position
            position.x in minX..maxX && position.y in minY..maxY && position.z in minZ..maxZ
        }.map { it.key }
    }
}
