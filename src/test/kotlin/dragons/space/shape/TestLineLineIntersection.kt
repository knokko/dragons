package dragons.space.shape

import dragons.space.Distance
import org.joml.Vector2f
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.absoluteValue

class TestLineLineIntersection {

    @Test
    fun testLineLineIntersectionParallel() {
        // These 2 horizontal lines overlap
        assertNull(determineLineLineIntersection(
            Distance.meters(10), Distance.meters(-40), Vector2f(1f, 0f),
            Distance.meters(100), Distance.meters(-40), Vector2f(1f, 0f)
        ))

        // These 2 horizontal lines are parallel, but don't overlap
        assertNull(determineLineLineIntersection(
            Distance.meters(10), Distance.meters(-20), Vector2f(1f, 0f),
            Distance.meters(100), Distance.meters(-40), Vector2f(0.1f, 0f)
        ))

        // These 2 vertical lines overlap
        assertNull(determineLineLineIntersection(
            Distance.meters(50), Distance.meters(120), Vector2f(0f, 1f),
            Distance.meters(50), Distance.meters(20), Vector2f(0f, 2f)
        ))

        // These 2 vertical lines are parallel, but don't overlap
        assertNull(determineLineLineIntersection(
            Distance.meters(40), Distance.meters(120), Vector2f(0f, 3f),
            Distance.meters(50), Distance.meters(20), Vector2f(0f, 1f)
        ))

        // These 2 lines overlap
        assertNull(determineLineLineIntersection(
            Distance.meters(0), Distance.meters(0), Vector2f(-2f, 5f),
            Distance.meters(-40), Distance.meters(80), Vector2f(-1f, 2.5f)
        ))
        // These 2 lines are parallel, but don't overlap
        assertNull(determineLineLineIntersection(
            Distance.meters(0), Distance.meters(80), Vector2f(-2f, 5f),
            Distance.meters(-40), Distance.meters(80), Vector2f(-1f, 2.5f)
        ))
    }

    private fun assertHit(expectedDistance: Distance, actual: Distance?) {
        assertTrue((expectedDistance - actual!!).milliMeters.absoluteValue < 1)
    }

    @Test
    fun testLineLineIntersectionAxisAligned() {
        // Horizontal line against vertical line
        assertHit(Distance.meters(30), determineLineLineIntersection(
            Distance.meters(20), Distance.meters(-30), Vector2f(1f, 0f),
            Distance.meters(50), Distance.meters(100), Vector2f(0f, 3f)
        ))

        // Vertical line against horizontal line
        assertHit(Distance.meters(-10), determineLineLineIntersection(
            Distance.meters(100), Distance.meters(200), Vector2f(0f, 10f),
            Distance.meters(-60), Distance.meters(100), Vector2f(0.01f, 0f)
        ))
    }

    @Test
    fun testLineLineIntersectionGeneral() {
        // General test
        assertHit(Distance.meters(-5), determineLineLineIntersection(
            Distance.meters(-50), Distance.meters(10), Vector2f(2f, 1f),
            Distance.meters(0), Distance.meters(0), Vector2f(12f, -1f)
        ))

        // Horizontal line against diagonal line
        assertHit(Distance.meters(20), determineLineLineIntersection(
            Distance.meters(30), Distance.meters(10), Vector2f(1f, 0f),
            Distance.meters(70), Distance.meters(30), Vector2f(1f, 1f)
        ))

        // Vertical line against diagonal line
        assertHit(Distance.meters(1), determineLineLineIntersection(
            Distance.meters(-50), Distance.meters(0), Vector2f(0f, -40f),
            Distance.meters(-60), Distance.meters(-30), Vector2f(10f, -10f)
        ))
    }
}
