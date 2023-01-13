package dragons.space.shape

import dragons.space.Distance
import dragons.space.Position
import org.joml.Math.sqrt
import org.joml.Vector3f
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.absoluteValue

class TestCylinderShape {

    @Test
    fun testFindRayIntersectionMiss() {
        val cylinder = CylinderShape(halfHeight = Distance.meters(50), radius = Distance.meters(10))
        val position = Position.meters(0, 100, -100)

        // Complete miss
        assertNull(cylinder.findRayIntersection(
            position, Position.meters(0, 0, 0), Vector3f(1f, 0f, 0f), Distance.meters(1000)
        ))

        // Horizontal line that goes below the cylinder
        assertNull(cylinder.findRayIntersection(
            position, Position.meters(0, 45, 0), Vector3f(0f, 0f, -1f), Distance.meters(200)
        ))

        // Horizontal line that goes above the cylinder
        assertNull(cylinder.findRayIntersection(
            position, Position.meters(0, 155, 0), Vector3f(0f, 0f, -1f), Distance.meters(200)
        ))

        // Horizontal line that misses the cylinder on the right
        assertNull(cylinder.findRayIntersection(
            position, Position.meters(15, 100, 0), Vector3f(0f, 0f, -1f), Distance.meters(200)
        ))

        // Diagonal line that misses the cylinder on the bottom-right
        assertNull(cylinder.findRayIntersection(
            position, Position.meters(-35, 0, -100),
            Vector3f(0.5f * sqrt(2f), 0.5f * sqrt(2f), 0f), Distance.meters(200)
        ))

        // Horizontal diagonal line that misses the cylinder on the front-right
        assertNull(cylinder.findRayIntersection(
            position, Position.meters(-100, 100, 15),
            Vector3f(0.5f * sqrt(2f), 0f, -0.5f * sqrt(2f)), Distance.meters(200)
        ))

        // Horizontal line that is too short
        assertNull(cylinder.findRayIntersection(
            position, Position.meters(0, 100, 0), Vector3f(0f, 0f, -1f), Distance.meters(85)
        ))

        // Vertical line that misses it barely
        assertNull(cylinder.findRayIntersection(
            position, Position.meters(8, 0, -108), Vector3f(0f, 1f, 0f), Distance.meters(500)
        ))

        // Vertical line that is too short
        assertNull(cylinder.findRayIntersection(
            position, Position.meters(0, 0, 0), Vector3f(0f, 1f, 0f), Distance.meters(45)
        ))

        // Vertical line in the opposite direction
        assertNull(cylinder.findRayIntersection(
            position, Position.meters(0, 0, 0), Vector3f(0f, -1f, 0f), Distance.meters(400)
        ))

        // Line that misses the cylinder entirely
        assertNull(cylinder.findRayIntersection(
            position, Position.meters(0, 200, 0), Vector3f(0f, 1f, 0f), Distance.meters(300)
        ))
    }

    private fun assertHit(expected: Float, actual: Distance?) {
        assertTrue((expected - actual!!.meters).absoluteValue < 0.001f)
    }

    @Test
    fun testRayIntersectionHitBottom() {
        val cylinder = CylinderShape(halfHeight = Distance.meters(50), radius = Distance.meters(10))
        val position = Position.meters(0, 100, -100)

        // Vertical line that is barely long enough
        assertHit(50f, cylinder.findRayIntersection(
            position, Position.meters(0, 0, -100), Vector3f(0f, 1f, 0f), Distance.meters(51)
        ))

        // Vertical line that enters at the bottom and exits at the top
        assertHit(50f, cylinder.findRayIntersection(
            position, Position.meters(0, 0, -100), Vector3f(0f, 1f, 0f), Distance.meters(151)
        ))

        // Vertical line that almost misses
        assertHit(30f, cylinder.findRayIntersection(
            position, Position.meters(7, 20, -107), Vector3f(0f, 1f, 0f), Distance.meters(500)
        ))

        // Diagonal line that enters at the bottom and exits at the side
        assertHit(sqrt(2f), cylinder.findRayIntersection(
            position, position + Position.meters(8, -51, 0),
            Vector3f(0.5f * sqrt(2f), 0.5f * sqrt(2f), 0f), Distance.meters(100)
        ))
    }

    @Test
    fun testRayIntersectionHitTop() {
        val cylinder = CylinderShape(halfHeight = Distance.meters(50), radius = Distance.meters(10))
        val position = Position.meters(0, 100, -100)

        // Vertical line that is barely long enough
        assertHit(50f, cylinder.findRayIntersection(
            position, Position.meters(0, 200, -100), Vector3f(0f, -1f, 0f), Distance.meters(51)
        ))

        // Vertical line that enters at the top and exits at the bottom
        assertHit(50f, cylinder.findRayIntersection(
            position, Position.meters(0, 200, -100), Vector3f(0f, -1f, 0f), Distance.meters(151)
        ))

        // Vertical line that almost misses
        assertHit(30f, cylinder.findRayIntersection(
            position, Position.meters(7, 180, -107), Vector3f(0f, -1f, 0f), Distance.meters(500)
        ))

        // Diagonal line that enters at the top and exits at the side
        assertHit(sqrt(2f), cylinder.findRayIntersection(
            position, position + Position.meters(8, 51, 0),
            Vector3f(0.5f * sqrt(2f), -0.5f * sqrt(2f), 0f), Distance.meters(100)
        ))
    }

    @Test
    fun testRayIntersectionHitSide() {
        val cylinder = CylinderShape(halfHeight = Distance.meters(30), radius = Distance.meters(10))
        val position = Position.meters(0, 100, 200)

        // Horizontal line that intersects the right side of the cylinder
        assertHit(200 - 5f * sqrt(3f), cylinder.findRayIntersection(
            position, Position.meters(5, 90, 0), Vector3f(0f, 0f, 1f), Distance.meters(300)
        ))

        // The same line, but barely long enough
        assertHit(200 - 5f * sqrt(3f), cylinder.findRayIntersection(
            position, Position.meters(5, 90, 0), Vector3f(0f, 0f, 1f), Distance.meters(195)
        ))

        // Diagonal line that enters on the left side and exits on the right side
        assertHit(90.4534f, cylinder.findRayIntersection(
            position, Position.meters(-100, 80, 200),
            Vector3f(sqrt(1f - 0.1f * 0.1f), 0.1f, 0f), Distance.meters(200)
        ))

        // Horizontal line that intersects the left side of the cylinder
        assertHit(200 - 5f * sqrt(3f), cylinder.findRayIntersection(
            position, Position.meters(-5, 90, 0), Vector3f(0f, 0f, 1f), Distance.meters(300)
        ))

        // Diagonal line that enters on the right side and exits on the left side
        assertHit(90.4534f, cylinder.findRayIntersection(
            position, Position.meters(100, 80, 200),
            Vector3f(-sqrt(1f - 0.1f * 0.1f), 0.1f, 0f), Distance.meters(200)
        ))

        // Steep line that enters on the positive Z side and exits at the top
        assertHit(80.6226f, cylinder.findRayIntersection( // intersection at (0, 120, 210)
            position, Position.meters(0, 50, 250), Vector3f(0f, 0.86824f, -0.49614f),
            Distance.meters(150)
        ))

        // Same line, but barely long enough
        assertHit(80.6226f, cylinder.findRayIntersection( // intersection at (0, 120, 210)
            position, Position.meters(0, 50, 250), Vector3f(0f, 0.86824f, -0.49614f),
            Distance.meters(81)
        ))

        // Steep line that starts next to the cylinder
        assertHit(5f, cylinder.findRayIntersection( // intersection at (0, 75, 190)
            position, Position.meters(0, 80, 187), Vector3f(0f, -0.8f, 0.6f),
            Distance.meters(50)
        ))

        // Same line, but barely long enough
        assertHit(5f, cylinder.findRayIntersection( // intersection at (0, 75, 190)
            position, Position.meters(0, 80, 187), Vector3f(0f, -0.8f, 0.6f),
            Distance.meters(5.1f)
        ))
    }

    @Test
    fun testCreateBoundingBox() {
        val cylinder = CylinderShape(halfHeight = Distance.meters(30), radius = Distance.meters(10))
        val position = Position.meters(0, 100, 200)
        val boundingBox = cylinder.createBoundingBox(position)
        assertEquals(Position.meters(-10, 70, 190), boundingBox.min)
        assertEquals(Position.meters(10, 130, 210), boundingBox.max)
    }

    // This is one of the tests that was successfully generated by OpenAI :)
    @Test
    fun testFindRayIntersection_MissesCylinder() {
        val cylinder = CylinderShape(halfHeight = Distance.meters(30), radius = Distance.meters(10))
        val position = Position.meters(0, 100, 200)
        val rayStart = Position.meters(0, 200, 0)
        val unitDirection = Vector3f(0f, 1f, 0f)
        val rayLength = Distance.meters(300)
        val intersection = cylinder.findRayIntersection(position, rayStart, unitDirection, rayLength)
        assertNull(intersection)
    }
}
