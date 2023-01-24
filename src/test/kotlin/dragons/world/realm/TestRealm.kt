package dragons.world.realm

import dragons.geometry.Distance
import dragons.geometry.Position
import dragons.geometry.shape.CylinderShape
import dragons.world.entity.EntityProperties
import dragons.world.entity.EntityState
import dragons.world.tile.TileProperties
import dragons.world.tile.TileState
import org.joml.Vector3f
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class TestRealm {

    @Test
    fun testRaytrace() {
        val realm = InMemoryRealm(UUID.randomUUID(), "Test Realm", true)

        // Small tile at the origin
        val originTile = realm.addTile(TestTile(Position.meters(0, 0, 0), 5, 15), TestTileState())

        // Tile located in chunk (1, 0, 0), but still overlaps with the origin chunk
        val farBigTile = realm.addTile(TestTile(Position.meters(190, 50, 50), 30, 95), TestTileState())

        // Simple tile in the origin chunk
        val simpleTile = realm.addTile(TestTile(Position.meters(70, 70, 20), 10, 10), TestTileState())

        // Simple entity in the origin chunk
        val entity = realm.addEntity(TestEntity(10, 10), TestEntityState(Position.meters(20, 20, 20)))

        // Hit the origin tile from below
        assertHit(originTile, Position.meters(0, -5, 0), realm.raytrace(
            Position.meters(0, -50, 0), Vector3f(0f, 1f, 0f), Distance.meters(200), null
        ))
        // Hit the origin tile again, but exclude its id
        assertNull(realm.raytrace(
            Position.meters(0, -50, 0), Vector3f(0f, 1f, 0f), Distance.meters(200), originTile.id
        ))
        // Same ray, but the ray is barely long enough
        assertHit(originTile, Position.meters(0, -5, 0), realm.raytrace(
            Position.meters(0, -50, 0), Vector3f(0f, 1f, 0f), Distance.meters(45.1f), null
        ))
        // Again same ray, but this one is too short
        assertNull(realm.raytrace(
            Position.meters(0, -50, 0), Vector3f(0f, 1f, 0f), Distance.meters(44.9f), null
        ))

        // Hit the simple tile far from above
        assertHit(simpleTile, Position.meters(70, 80, 20), realm.raytrace(
            Position.meters(70, 2000, 20), Vector3f(0f, -1f, 0f), Distance.kiloMeters(2), null
        ))
        // Slightly miss the simple tile far from above
        assertNull(realm.raytrace(
            Position.meters(62, 2000, 28), Vector3f(0f, -1f, 0f), Distance.kiloMeters(2), null
        ))

        // Hit both the simple tile and simple entity, but the simple entity first
        assertHit(entity, Position.meters(10, 10, 20), realm.raytrace(
            Position.meters(-25, -25, 20), Vector3f(1f, 1f, 0f), Distance.kiloMeters(1), null
        ))
        // Hit both the simple tile and simple entity, hit the simple entity first, but exclude its id
        assertHit(simpleTile, Position.meters(60, 60, 20), realm.raytrace(
            Position.meters(-25, -25, 20), Vector3f(1f, 1f, 0f), Distance.kiloMeters(1), entity.id
        ))
        // Hit both the simple tile and simple entity, but the simple tile first
        assertHit(simpleTile, Position.meters(80, 80, 20), realm.raytrace(
            Position.meters(125, 125, 20), Vector3f(-1f, -1f, 0f), Distance.kiloMeters(1), null
        ))
        // Hit both the simple tile and simple entity, hit the simple tile first, but exclude its id
        assertHit(entity, Position.meters(30, 30, 20), realm.raytrace(
            Position.meters(125, 125, 20), Vector3f(-1f, -1f, 0f), Distance.kiloMeters(1), simpleTile.id
        ))

        // Hit farBigTile with a ray that does NOT intersect its chunk
        assertHit(farBigTile, Position.meters(95, 50, 50), realm.raytrace(
            Position.meters(50, 50, 50), Vector3f(5f, 0f, 0f), Distance.meters(46), null
        ))
    }

    private fun assertHit(expectedObject: Any, expectedPosition: Position, actual: Pair<Any, Position>?) {
        assertEquals(expectedObject, actual!!.first)
        assertTrue(expectedPosition.distanceTo(actual.second) < Distance.milliMeters(1))
    }
}

private class TestTile(position: Position, halfHeight: Int, radius: Int): TileProperties(
    position, CylinderShape(halfHeight = Distance.meters(halfHeight), radius = Distance.meters(radius))
) {
    override fun getPersistentClassID() = "TestRealm:TestTile"
}

private class TestTileState: TileState

private class TestEntity(val halfHeight: Int, val radius: Int): EntityProperties() {
    override fun getPersistentClassID() = "TestRealm:TestEntity"

    override fun getShape(state: EntityState) = CylinderShape(
        halfHeight = Distance.meters(halfHeight), radius = Distance.meters(radius)
    )
}

private class TestEntityState(position: Position): EntityState(position)
