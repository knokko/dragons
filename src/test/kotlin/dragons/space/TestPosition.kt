package dragons.space

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestPosition {

    @Test
    fun testDistanceTo() {
        // Simple tests
        assertEquals(150f, Position.meters(0, -100, 0).distanceTo(Position.meters(0, 50, 0)).meters, 0.001f)
        assertEquals(5f, Position.meters(10, 13, 100).distanceTo(Position.meters(14, 10, 100)).meters, 0.001f)

        // Test floating point cancellation resistance
        val farAway = Position.kiloMeters(1_000_000, -1_000_000, 1_000_000)
        assertEquals(10f, farAway.distanceTo(farAway + Position.milliMeters(0, 0, 10)).milliMeters, 0.001f)
    }
}
