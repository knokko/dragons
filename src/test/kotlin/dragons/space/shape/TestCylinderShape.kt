package dragons.space.shape

import dragons.space.Distance
import dragons.space.Position
import org.joml.Math.sqrt
import org.joml.Vector3f
import org.junit.jupiter.api.Assertions.assertNull
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
            position, Position.meters(0, 0, 0), Vector3f(0f, 1f, 0f), Distance.meters(51)
        ))

        // Vertical line that enters at the bottom and exits at the top
        assertHit(50f, cylinder.findRayIntersection(
            position, Position.meters(0, 0, 0), Vector3f(0f, 1f, 0f), Distance.meters(151)
        ))

        // Vertical line that almost misses
        assertHit(30f, cylinder.findRayIntersection(
            position, Position.meters(7, 20, -107), Vector3f(0f, 1f, 0f), Distance.meters(500)
        ))

        // Diagonal line that enters at the bottom and exits at the side
        assertHit(sqrt(2f), cylinder.findRayIntersection(
            position, position + Position.meters(8, -1, 0),
            Vector3f(0.5f * sqrt(2f), 0.5f * sqrt(2f), 0f), Distance.meters(100)
        ))
    }
}
