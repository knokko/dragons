package dragons.space.shape

import dragons.geometry.Coordinate
import dragons.geometry.Distance
import dragons.geometry.shape.intersection.determineLineCircleIntersections
import org.joml.Math.sqrt
import org.joml.Vector2f
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.absoluteValue

class TestLineCircleIntersection {

    @Test
    fun testDetermineLineCircleIntersectionMiss() {
        // Horizontal line below the circle
        assertNull(
            determineLineCircleIntersections(
            centerX = Coordinate.meters(50), centerY = Coordinate.meters(100), radius = Distance.meters(20),
            lineX = Coordinate.meters(10), lineY = Coordinate.meters(40), direction = Vector2f(1f, 0f)
        )
        )

        // Horizontal line above the circle
        assertNull(
            determineLineCircleIntersections(
            centerX = Coordinate.meters(50), centerY = Coordinate.meters(100), radius = Distance.meters(20),
            lineX = Coordinate.meters(10), lineY = Coordinate.meters(140), direction = Vector2f(1f, 0f)
        )
        )

        // Vertical line on the left of the circle
        assertNull(
            determineLineCircleIntersections(
            centerX = Coordinate.meters(50), centerY = Coordinate.meters(100), radius = Distance.meters(20),
            lineX = Coordinate.meters(10), lineY = Coordinate.meters(40), direction = Vector2f(0f, 1f)
        )
        )

        // Vertical line on the right of the circle
        assertNull(
            determineLineCircleIntersections(
            centerX = Coordinate.meters(50), centerY = Coordinate.meters(100), radius = Distance.meters(20),
            lineX = Coordinate.meters(100), lineY = Coordinate.meters(40), direction = Vector2f(0f, 1f)
        )
        )

        // Diagonal line below the circle
        assertNull(
            determineLineCircleIntersections(
            centerX = Coordinate.meters(50), centerY = Coordinate.meters(100), radius = Distance.meters(20),
            lineX = Coordinate.meters(10), lineY = Coordinate.meters(10), direction = Vector2f(0.7f, 0.7f)
        )
        )

        // Diagonal line that approaches the circle very closely
        assertNull(
            determineLineCircleIntersections(
            centerX = Coordinate.meters(0), centerY = Coordinate.meters(0), radius = Distance.meters(10),
            lineX = Coordinate.meters(0), lineY = Coordinate.meters(-14.5f),
            direction = Vector2f(0.5f * sqrt(2f))
        )
        )
    }

    private fun assertHit(distance1: Float, distance2: Float, result: Pair<Distance, Distance>?) {
        assertTrue((distance1 - result!!.first.meters.toFloat()).absoluteValue < 0.01f)
        assertTrue((distance2 - result.second.meters.toFloat()).absoluteValue < 0.01f)
    }

    @Test
    fun testDetermineLineCircleIntersectionHorizontalHit() {
        // Horizontal line through the lower half of the circle
        assertHit(20f - 5f * sqrt(3f), 20f + 5f * sqrt(3f), determineLineCircleIntersections(
            centerX = Coordinate.meters(50), centerY = Coordinate.meters(100), radius = Distance.meters(20),
            lineX = Coordinate.meters(10), lineY = Coordinate.meters(90), direction = Vector2f(2f, 0f)
        )
        )

        // Horizontal right-to-left line straight through the center of the circle
        assertHit(80f, 120f, determineLineCircleIntersections(
            centerX = Coordinate.meters(50), centerY = Coordinate.meters(100), radius = Distance.meters(20),
            lineX = Coordinate.meters(150), lineY = Coordinate.meters(100), direction = Vector2f(-1f, 0f)
        )
        )

        // Horizontal line through the upper half of the circle
        assertHit(40f - sqrt(300f), 40f + sqrt(300f), determineLineCircleIntersections(
            centerX = Coordinate.meters(50), centerY = Coordinate.meters(100), radius = Distance.meters(20),
            lineX = Coordinate.meters(10), lineY = Coordinate.meters(110), direction = Vector2f(1f, 0f)
        )
        )

        // Horizontal line in the wrong direction
        assertHit(-60f, -40f, determineLineCircleIntersections(
            centerX = Coordinate.meters(50), centerY = Coordinate.meters(50), radius = Distance.meters(10),
            lineX = Coordinate.meters(100), lineY = Coordinate.meters(50), direction = Vector2f(1f, 0f)
        )
        )
    }

    @Test
    fun testDetermineLineCircleIntersectionVerticalHit() {
        // Vertical line that intersects at the left of the center
        assertHit(60f - sqrt(300f), 60f + sqrt(300f), determineLineCircleIntersections(
            centerX = Coordinate.meters(50), centerY = Coordinate.meters(100), radius = Distance.meters(20),
            lineX = Coordinate.meters(40), lineY = Coordinate.meters(40), direction = Vector2f(0f, 1f)
        )
        )

        // Vertical down-going line that goes straight through the center
        assertHit(80f, 160f, determineLineCircleIntersections(
            centerX = Coordinate.meters(50), centerY = Coordinate.meters(100), radius = Distance.meters(20),
            lineX = Coordinate.meters(50), lineY = Coordinate.meters(160), direction = Vector2f(0f, -0.5f)
        )
        )

        // Vertical line that intersects at the right of the center
        assertHit(60f - sqrt(300f), 60f + sqrt(300f), determineLineCircleIntersections(
            centerX = Coordinate.meters(50), centerY = Coordinate.meters(100), radius = Distance.meters(20),
            lineX = Coordinate.meters(60), lineY = Coordinate.meters(40), direction = Vector2f(0f, 1f)
        )
        )
    }

    @Test
    fun testDetermineLineCircleIntersectionDiagonalHit() {
        // Right-down diagonal line that intersects the circle at 90 degrees and 0 degrees
        assertHit(sqrt(5000f), sqrt(7200f), determineLineCircleIntersections(
            centerX = Coordinate.meters(100), centerY = Coordinate.meters(100), radius = Distance.meters(10),
            lineX = Coordinate.meters(50), lineY = Coordinate.meters(160),
            direction = Vector2f(0.5f * sqrt(2f), -0.5f * sqrt(2f))
        )
        )

        // Right-up diagonal line that goes through the center of the circle
        assertHit((sqrt(20_000f) - 10f) / 3f, (sqrt(20_000f) + 10f) / 3f, determineLineCircleIntersections(
            centerX = Coordinate.meters(100), centerY = Coordinate.meters(0), radius = Distance.meters(10),
            lineX = Coordinate.meters(0), lineY = Coordinate.meters(-100),
            direction = Vector2f(1.5f * sqrt(2f))
        )
        )

        // Left-up diagonal line that intersects the circle at 270 degrees and 180 degrees
        assertHit(sqrt(20_000f), sqrt(24_200f), determineLineCircleIntersections(
            centerX = Coordinate.meters(0), centerY = Coordinate.meters(0), radius = Distance.meters(10),
            lineX = Coordinate.meters(100), lineY = Coordinate.meters(-110f),
            direction = Vector2f(-0.5f * sqrt(2f), 0.5f * sqrt(2f))
        )
        )
    }

    @Test
    fun testDetermineLineCircleIntersectionZeroDirection() {
        // Zero-direction line that starts inside the circle
        assertHit(-100_000f, 100_000f, determineLineCircleIntersections(
            centerX = -Coordinate.meters(50), centerY = -Coordinate.meters(100), radius = Distance.meters(5),
            lineX = -Coordinate.meters(51), lineY = -Coordinate.meters(100), direction = Vector2f()
        )
        )

        // Zero-direction line that starts outside the circle
        assertNull(
            determineLineCircleIntersections(
            centerX = -Coordinate.meters(40), centerY = -Coordinate.meters(100), radius = Distance.meters(5),
            lineX = -Coordinate.meters(51), lineY = -Coordinate.meters(100), direction = Vector2f()
        )
        )
    }
}
