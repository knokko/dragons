package dragons.space.shape

import dragons.geometry.Coordinate
import dragons.geometry.Distance
import dragons.geometry.shape.intersection.determineLineLineIntersection
import org.joml.Vector2f
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.absoluteValue

class TestLineLineIntersection {

    @Test
    fun testLineLineIntersectionParallel() {
        // These 2 horizontal lines overlap
        assertNull(
            determineLineLineIntersection(
            Coordinate.meters(10), Coordinate.meters(-40), Vector2f(1f, 0f),
            Coordinate.meters(100), Coordinate.meters(-40), Vector2f(1f, 0f)
        )
        )

        // These 2 horizontal lines are parallel, but don't overlap
        assertNull(
            determineLineLineIntersection(
            Coordinate.meters(10), Coordinate.meters(-20), Vector2f(1f, 0f),
            Coordinate.meters(100), Coordinate.meters(-40), Vector2f(0.1f, 0f)
        )
        )

        // These 2 vertical lines overlap
        assertNull(
            determineLineLineIntersection(
            Coordinate.meters(50), Coordinate.meters(120), Vector2f(0f, 1f),
            Coordinate.meters(50), Coordinate.meters(20), Vector2f(0f, 2f)
        )
        )

        // These 2 vertical lines are parallel, but don't overlap
        assertNull(
            determineLineLineIntersection(
            Coordinate.meters(40), Coordinate.meters(120), Vector2f(0f, 3f),
            Coordinate.meters(50), Coordinate.meters(20), Vector2f(0f, 1f)
        )
        )

        // These 2 lines overlap
        assertNull(
            determineLineLineIntersection(
            Coordinate.meters(0), Coordinate.meters(0), Vector2f(-2f, 5f),
            Coordinate.meters(-40), Coordinate.meters(80), Vector2f(-1f, 2.5f)
        )
        )
        // These 2 lines are parallel, but don't overlap
        assertNull(
            determineLineLineIntersection(
            Coordinate.meters(0), Coordinate.meters(80), Vector2f(-2f, 5f),
            Coordinate.meters(-40), Coordinate.meters(80), Vector2f(-1f, 2.5f)
        )
        )
    }

    private fun assertHit(expectedDistance: Distance, actual: Distance?) {
        assertTrue((expectedDistance - actual!!).milliMeters.toFloat().absoluteValue < 1)
    }

    @Test
    fun testLineLineIntersectionAxisAligned() {
        // Horizontal line against vertical line
        assertHit(
            Distance.meters(30), determineLineLineIntersection(
            Coordinate.meters(20), Coordinate.meters(-30), Vector2f(1f, 0f),
            Coordinate.meters(50), Coordinate.meters(100), Vector2f(0f, 3f)
        )
        )

        // Vertical line against horizontal line
        assertHit(
            Distance.meters(-10), determineLineLineIntersection(
            Coordinate.meters(100), Coordinate.meters(200), Vector2f(0f, 10f),
            Coordinate.meters(-60), Coordinate.meters(100), Vector2f(0.01f, 0f)
        )
        )
    }

    @Test
    fun testLineLineIntersectionGeneral() {
        // General test
        assertHit(
            Distance.meters(-5), determineLineLineIntersection(
            Coordinate.meters(-50), Coordinate.meters(10), Vector2f(2f, 1f),
            Coordinate.meters(0), Coordinate.meters(0), Vector2f(12f, -1f)
        )
        )

        // Horizontal line against diagonal line
        assertHit(
            Distance.meters(20), determineLineLineIntersection(
            Coordinate.meters(30), Coordinate.meters(10), Vector2f(1f, 0f),
            Coordinate.meters(70), Coordinate.meters(30), Vector2f(1f, 1f)
        )
        )

        // Vertical line against diagonal line
        assertHit(
            Distance.meters(1), determineLineLineIntersection(
            Coordinate.meters(-50), Coordinate.meters(0), Vector2f(0f, -40f),
            Coordinate.meters(-60), Coordinate.meters(-30), Vector2f(10f, -10f)
        )
        )
    }
}
