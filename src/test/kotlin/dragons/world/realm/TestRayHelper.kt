package dragons.world.realm

import dragons.geometry.BoundingBox
import dragons.geometry.Position
import dragons.world.chunk.ChunkLocation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestRayHelper {

    @Test
    fun testGetRelevantChunksForRaytrace() {

        // Almost minimal test: the ray trace goes from chunk (2, 4, 7) to chunk (3, 4, 7)
        // Note that all adjacent chunks must also be used, so this is still a rather big set of chunks
        assertEquals(getPotentiallyIntersectingChunks(
            BoundingBox(
            Position.meters(250, 450, 750), Position.meters(350, 450, 750)
        )
        ).toSet(), setOf(
            ChunkLocation(1, 3, 6), ChunkLocation(2, 3, 6),
            ChunkLocation(3, 3, 6), ChunkLocation(4, 3, 6),

            ChunkLocation(1, 4, 6), ChunkLocation(2, 4, 6),
            ChunkLocation(3, 4, 6), ChunkLocation(4, 4, 6),

            ChunkLocation(1, 5, 6), ChunkLocation(2, 5, 6),
            ChunkLocation(3, 5, 6), ChunkLocation(4, 5, 6),


            ChunkLocation(1, 3, 7), ChunkLocation(2, 3, 7),
            ChunkLocation(3, 3, 7), ChunkLocation(4, 3, 7),

            ChunkLocation(1, 4, 7), ChunkLocation(2, 4, 7),
            ChunkLocation(3, 4, 7), ChunkLocation(4, 4, 7),

            ChunkLocation(1, 5, 7), ChunkLocation(2, 5, 7),
            ChunkLocation(3, 5, 7), ChunkLocation(4, 5, 7),


            ChunkLocation(1, 3, 8), ChunkLocation(2, 3, 8),
            ChunkLocation(3, 3, 8), ChunkLocation(4, 3, 8),

            ChunkLocation(1, 4, 8), ChunkLocation(2, 4, 8),
            ChunkLocation(3, 4, 8), ChunkLocation(4, 4, 8),

            ChunkLocation(1, 5, 8), ChunkLocation(2, 5, 8),
            ChunkLocation(3, 5, 8), ChunkLocation(4, 5, 8)
        ))

        // Test just the X axis
        assertEquals(getPotentiallyIntersectingChunks(
            BoundingBox(
            Position.meters(2050, -250, 1050), Position.meters(2550, -250, 1050)
        )
        ).toSet(), (19 .. 26).flatMap { x -> (-4 .. -2).flatMap { y -> (9 .. 11).map { z -> ChunkLocation(x, y, z) }}}.toSet())

        // Test just the Y axis
        assertEquals(getPotentiallyIntersectingChunks(
            BoundingBox(
            Position.meters(2050, -250, 1050), Position.meters(2050, 250, 1050)
        )
        ).toSet(), (19 .. 21).flatMap { x -> (-4 .. 3).flatMap { y -> (9 .. 11).map { z -> ChunkLocation(x, y, z) }}}.toSet())
    }
}
