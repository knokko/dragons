package dragons.world.chunk

import dragons.space.Position
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestChunkLocation {

    @Test
    fun testPositionConstructor() {

        // Basic positive cases + rounding
        assertEquals(ChunkLocation(0, 0, 0), ChunkLocation(Position.nanoMeters(0, 0, 0)))
        assertEquals(
            ChunkLocation(0, 0, 0),
            ChunkLocation(Position.meters(100, 100, 100) - Position.nanoMeters(1, 1, 1))
        )
        assertEquals(ChunkLocation(1, 1, 1), ChunkLocation(Position.meters(100, 100, 100)))

        // Basic negative cases + rounding
        assertEquals(ChunkLocation(-1, -1, -1), ChunkLocation(Position.nanoMeters(-1, -1, -1)))
        assertEquals(ChunkLocation(-1, -1, -1), ChunkLocation(Position.meters(-100, -100, -100)))
        assertEquals(
            ChunkLocation(-2, -2, -2),
            ChunkLocation(Position.meters(-100, -100, -100) - Position.nanoMeters(1, 1, 1))
        )

        // Mixed cases
        assertEquals(ChunkLocation(-1, 0, 1), ChunkLocation(Position.meters(-20, 30, 140)))
        assertEquals(ChunkLocation(-5, 5, 6), ChunkLocation(Position.meters(-480, 510, 699)))
    }
}
