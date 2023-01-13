package dragons.world.chunk

import dragons.space.Distance
import dragons.space.Position
import dragons.space.shape.CylinderShape
import dragons.world.tile.TemporaryTile
import dragons.world.tile.TileProperties
import dragons.world.tile.TileState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.*

val testShape = CylinderShape(halfHeight = Distance.Companion.meters(50), radius = Distance.meters(100))

class TestTemporaryChunk {

    @Test
    fun testBounds() {
        val chunk = TemporaryChunk()
        assertNull(chunk.bounds)

        val id1 = UUID.randomUUID()
        chunk.tiles[id1] = TemporaryTile(id1, TestTile(Position.meters(500, 1000, 500)), TestTileState())
        assertNull(chunk.bounds)
        chunk.updateBounds()
        assertEquals(Position.meters(400, 950, 400), chunk.bounds!!.min)
        assertEquals(Position.meters(600, 1050, 600), chunk.bounds!!.max)

        val id2 = UUID.randomUUID()
        chunk.tiles[id2] = TemporaryTile(id2, TestTile(Position.meters(-1000, 500, 1000)), TestTileState())
        assertEquals(Position.meters(400, 950, 400), chunk.bounds!!.min)
        assertEquals(Position.meters(600, 1050, 600), chunk.bounds!!.max)
        chunk.updateBounds()
        assertEquals(Position.meters(-1100, 450, 400), chunk.bounds!!.min)
        assertEquals(Position.meters(600, 1050, 1100), chunk.bounds!!.max)

        chunk.tiles.remove(id1)
        assertEquals(Position.meters(-1100, 450, 400), chunk.bounds!!.min)
        assertEquals(Position.meters(600, 1050, 1100), chunk.bounds!!.max)
        chunk.updateBounds()
        assertEquals(Position.meters(-1100, 450, 900), chunk.bounds!!.min)
        assertEquals(Position.meters(-900, 550, 1100), chunk.bounds!!.max)

        chunk.tiles.remove(id2)
        assertEquals(Position.meters(-1100, 450, 900), chunk.bounds!!.min)
        assertEquals(Position.meters(-900, 550, 1100), chunk.bounds!!.max)
        chunk.updateBounds()
        assertNull(chunk.bounds)
    }
}

class TestTile(position: Position) : TileProperties(position, testShape) {
    override fun getPersistentClassID() = "TestTemporaryChunk:TestTile"
}

class TestTileState: TileState
