package dragons.space.shape

import dragons.space.Distance
import org.joml.Math.sqrt
import org.joml.Vector2f
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.absoluteValue

class TestLineCircleIntersection {

    @Test
    fun testDetermineLineCircleIntersectionMiss() {
        // Horizontal line below the circle
        assertNull(determineLineCircleIntersections(
            centerX = Distance.meters(50), centerY = Distance.meters(100), radius = Distance.meters(20),
            lineX = Distance.meters(10), lineY = Distance.meters(40), unitDirection = Vector2f(1f, 0f)
        ))

        // Horizontal line above the circle
        assertNull(determineLineCircleIntersections(
            centerX = Distance.meters(50), centerY = Distance.meters(100), radius = Distance.meters(20),
            lineX = Distance.meters(10), lineY = Distance.meters(140), unitDirection = Vector2f(1f, 0f)
        ))

        // Vertical line on the left of the circle
        assertNull(determineLineCircleIntersections(
            centerX = Distance.meters(50), centerY = Distance.meters(100), radius = Distance.meters(20),
            lineX = Distance.meters(10), lineY = Distance.meters(40), unitDirection = Vector2f(0f, 1f)
        ))

        // Vertical line on the right of the circle
        assertNull(determineLineCircleIntersections(
            centerX = Distance.meters(50), centerY = Distance.meters(100), radius = Distance.meters(20),
            lineX = Distance.meters(100), lineY = Distance.meters(40), unitDirection = Vector2f(0f, 1f)
        ))

        // Diagonal line below the circle
        assertNull(determineLineCircleIntersections(
            centerX = Distance.meters(50), centerY = Distance.meters(100), radius = Distance.meters(20),
            lineX = Distance.meters(10), lineY = Distance.meters(10), unitDirection = Vector2f(0.7f, 0.7f)
        ))

        // Diagonal line that approaches the circle very closely
        assertNull(determineLineCircleIntersections(
            centerX = Distance.meters(0), centerY = Distance.meters(0), radius = Distance.meters(10),
            lineX = Distance.meters(0), lineY = Distance.meters(-14.5f),
            unitDirection = Vector2f(0.5f * sqrt(2f))
        ))
    }

    private fun assertHit(distance1: Float, distance2: Float, result: Pair<Distance, Distance>?) {
        assertTrue((distance1 - result!!.first.meters).absoluteValue < 0.01f)
        assertTrue((distance2 - result.second.meters).absoluteValue < 0.01f)
    }

    @Test
    fun testDetermineLineCircleIntersectionHorizontalHit() {
        // Horizontal line through the lower half of the circle
        assertHit(40f - sqrt(300f), 40f + sqrt(300f), determineLineCircleIntersections(
            centerX = Distance.meters(50), centerY = Distance.meters(100), radius = Distance.meters(20),
            lineX = Distance.meters(10), lineY = Distance.meters(90), unitDirection = Vector2f(1f, 0f)
        ))

        // Horizontal right-to-left line straight through the center of the circle
        assertHit(80f, 120f, determineLineCircleIntersections(
            centerX = Distance.meters(50), centerY = Distance.meters(100), radius = Distance.meters(20),
            lineX = Distance.meters(150), lineY = Distance.meters(100), unitDirection = Vector2f(-1f, 0f)
        ))

        // Horizontal line through the upper half of the circle
        assertHit(40f - sqrt(300f), 40f + sqrt(300f), determineLineCircleIntersections(
            centerX = Distance.meters(50), centerY = Distance.meters(100), radius = Distance.meters(20),
            lineX = Distance.meters(10), lineY = Distance.meters(110), unitDirection = Vector2f(1f, 0f)
        ))
    }

    @Test
    fun testDetermineLineCircleIntersectionVerticalHit() {
        // Vertical line that intersects at the left of the center
        assertHit(60f - sqrt(300f), 60f + sqrt(300f), determineLineCircleIntersections(
            centerX = Distance.meters(50), centerY = Distance.meters(100), radius = Distance.meters(20),
            lineX = Distance.meters(40), lineY = Distance.meters(40), unitDirection = Vector2f(0f, 1f)
        ))

        // Vertical down-going line that goes straight through the center
        assertHit(40f, 80f, determineLineCircleIntersections(
            centerX = Distance.meters(50), centerY = Distance.meters(100), radius = Distance.meters(20),
            lineX = Distance.meters(50), lineY = Distance.meters(160), unitDirection = Vector2f(0f, -1f)
        ))

        // Vertical line that intersects at the right of the center
        assertHit(60f - sqrt(300f), 60f + sqrt(300f), determineLineCircleIntersections(
            centerX = Distance.meters(50), centerY = Distance.meters(100), radius = Distance.meters(20),
            lineX = Distance.meters(60), lineY = Distance.meters(40), unitDirection = Vector2f(0f, 1f)
        ))
    }

    @Test
    fun testDetermineLineCircleIntersectionDiagonalHit() {
        // Right-down diagonal line that intersects the circle at 90 degrees and 0 degrees
        assertHit(sqrt(5000f), sqrt(7200f), determineLineCircleIntersections(
            centerX = Distance.meters(100), centerY = Distance.meters(100), radius = Distance.meters(10),
            lineX = Distance.meters(50), lineY = Distance.meters(160),
            unitDirection = Vector2f(0.5f * sqrt(2f), -0.5f * sqrt(2f))
        ))

        // Right-up diagonal line that goes through the center of the circle
        assertHit(sqrt(20_000f) - 10f, sqrt(20_000f) + 10f, determineLineCircleIntersections(
            centerX = Distance.meters(100), centerY = Distance.meters(0), radius = Distance.meters(10),
            lineX = Distance.meters(0), lineY = Distance.meters(-100),
            unitDirection = Vector2f(0.5f * sqrt(2f))
        ))

        // Left-up diagonal line that intersects the circle at 270 degrees and 180 degrees
        assertHit(sqrt(20_000f), sqrt(24_200f), determineLineCircleIntersections(
            centerX = Distance.meters(0), centerY = Distance.meters(0), radius = Distance.meters(10),
            lineX = Distance.meters(100), lineY = Distance.meters(-110f),
            unitDirection = Vector2f(-0.5f * sqrt(2f), 0.5f * sqrt(2f))
        ))
    }
}
