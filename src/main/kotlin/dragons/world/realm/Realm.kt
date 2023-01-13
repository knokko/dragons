package dragons.world.realm

import dragons.space.BoundingBox
import dragons.space.Distance
import dragons.space.Position
import dragons.world.chunk.Chunk
import dragons.world.chunk.ChunkLocation
import dragons.world.chunk.TemporaryChunk
import dragons.world.entity.Entity
import dragons.world.entity.EntityProperties
import dragons.world.entity.EntityState
import dragons.world.entity.TemporaryEntity
import dragons.world.tile.SmallTile
import dragons.world.tile.TileProperties
import dragons.world.tile.TileState
import org.joml.Vector3f
import java.util.*

abstract class Realm(
    val id: UUID,
    val displayName: String,
    private val isInDesigner: Boolean
) {

    /**
     * Returns true if and only if `getChunk(location)` has been called at least once
     */
    abstract fun hasChunk(location: ChunkLocation): Boolean

    internal abstract fun getTemporaryChunk(location: ChunkLocation): TemporaryChunk

    /**
     * Returns the chunk at the given coordinates, creating the chunk if it doesn't exist yet
     */
    fun getChunk(location: ChunkLocation) = Chunk(location, isInDesigner) { getTemporaryChunk(location) }

    fun addTile(properties: TileProperties, initialState: TileState): SmallTile {
        val chunkLocation = ChunkLocation(properties.position)
        return getChunk(chunkLocation).addTile(properties, initialState)
    }

    abstract fun hasEntity(id: UUID): Boolean

    /**
     * Throws `IllegalArgumentException` if the entity with the given ID does not exist (anymore)
     */
    @Throws(IllegalArgumentException::class)
    internal abstract fun getTemporaryEntity(id: UUID): TemporaryEntity

    fun getEntity(id: UUID): Entity {
        val properties = getTemporaryEntity(id).properties
        return Entity(
            id, properties, { getTemporaryEntity(id).state },
            { newState -> getTemporaryEntity(id).state = newState }
        )
    }

    abstract fun addEntity(properties: EntityProperties, initialState: EntityState): Entity

    abstract fun queryEntityIDsBetween(bounds: BoundingBox): Collection<UUID>

    fun raytrace(rayStart: Position, direction: Vector3f, distance: Distance): Pair<Any, Position>? {
        val unitDirection = direction.normalize(Vector3f())
        if (!unitDirection.isFinite) return null

        var currentRayLength = distance
        var currentChunkLocation: ChunkLocation? = null
        var currentTileID: UUID? = null

        fun createRayBounds() = BoundingBox(rayStart, rayStart + currentRayLength * unitDirection)
        var rayBounds = createRayBounds()

        val chunkLocations = getPotentiallyIntersectingChunks(rayBounds)

        for (location in chunkLocations) {
            val temporaryChunk = getTemporaryChunk(location)
            val chunkBounds = temporaryChunk.bounds
            if (chunkBounds != null && chunkBounds.intersects(rayBounds)) {

                for ((tileID, tile) in temporaryChunk.tiles) {
                    if (tile.properties.bounds.intersects(rayBounds)) {
                        val rayTileDistance = tile.properties.shape.findRayIntersection(
                            tile.properties.position, rayStart, unitDirection, currentRayLength
                        )
                        if (rayTileDistance != null) {
                            currentRayLength = rayTileDistance
                            currentChunkLocation = location
                            currentTileID = tileID
                            rayBounds = createRayBounds()
                        }
                    }
                }
            }
        }

        var currentEntityID: UUID? = null

        for (entity in queryEntityIDsBetween(rayBounds).map(this::getTemporaryEntity)) {
            val shape = entity.properties.getShape(entity.state)
            if (rayBounds.intersects(shape.createBoundingBox(entity.state.position))) {
                val rayEntityDistance = shape.findRayIntersection(
                    entity.state.position, rayStart, unitDirection, currentRayLength
                )
                if (rayEntityDistance != null) {
                    currentRayLength = rayEntityDistance
                    currentChunkLocation = null
                    currentTileID = null
                    currentEntityID = entity.id
                    rayBounds = createRayBounds()
                }
            }
        }

        if (currentTileID != null) {
            return Pair(getChunk(currentChunkLocation!!).getTile(currentTileID), rayStart + currentRayLength * unitDirection)
        }

        if (currentEntityID != null) {
            return Pair(getEntity(currentEntityID), rayStart + currentRayLength * unitDirection)
        }

        return null
    }
}
