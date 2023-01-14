package dragons.space.shape

import dragons.space.Angle
import dragons.space.Distance
import dragons.space.Position
import org.joml.Math.sqrt
import org.joml.Vector3f
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.absoluteValue

class TestVerticalPlaneShape {

    @Test
    fun testCreateBoundingBox() {
        fun checkBoundingBox(
            planeShape: VerticalPlaneShape, x: Int, y: Int, z: Int,
            minX: Float, minY: Float, minZ: Float, maxX: Float, maxY: Float, maxZ: Float
        ) {
            val box = planeShape.createBoundingBox(Position.meters(x, y, z))
            assertTrue(Position.meters(minX, minY, minZ).distanceTo(box.min).milliMeters < 1f)
            assertTrue(Position.meters(maxX, maxY, maxZ).distanceTo(box.max).milliMeters < 1f)
        }

        // Positive X plane
        checkBoundingBox(
            VerticalPlaneShape(Distance.meters(40), Distance.meters(30), Angle.degrees(0)),
            100, 100, 100, 60f, 70f, 100f, 140f, 130f, 100f
        )

        // Positive Z plane
        checkBoundingBox(
            VerticalPlaneShape(Distance.meters(10), Distance.meters(20), Angle.degrees(90)),
            100, 200, 300, 100f, 180f, 290f, 100f, 220f, 310f
        )

        // Negative X plane
        checkBoundingBox(
            VerticalPlaneShape(Distance.meters(20), Distance.meters(20), Angle.degrees(180)),
            100, 200, 300, 80f, 180f, 300f, 120f, 220f, 300f
        )

        // Negative Z plane
        checkBoundingBox(
            VerticalPlaneShape(Distance.meters(50), Distance.meters(30), Angle.degrees(270)),
            100, 100, 100, 100f, 70f, 50f, 100f, 130f, 150f
        )

        // Diagonal plane
        checkBoundingBox(
            VerticalPlaneShape(Distance.meters(10), Distance.meters(5), Angle.degrees(45)),
            -100, -100, -100, -100f - 5f * sqrt(2f), -105f, -100f - 5f * sqrt(2f),
            -100f + 5f * sqrt(2f), -95f, -100f + 5f * sqrt(2f)
        )
    }

    @Test
    fun testFindRayIntersectionParallel() {
        val shapeX = VerticalPlaneShape(Distance.meters(30), Distance.meters(50), Angle.degrees(0))
        val shapeZ = VerticalPlaneShape(Distance.meters(30), Distance.meters(50), Angle.degrees(270))
        val shapeD = VerticalPlaneShape(Distance.meters(30), Distance.meters(50), Angle.degrees(45))

        val position = Position.meters(130, 140, 150)

        for (z in arrayOf(50, 140, 150, 160, 1000)) {
            assertNull(shapeX.findRayIntersection(
                position, Position.meters(0, 0, z), Vector3f(1f, 0f, 0f), Distance.kiloMeters(1)
            ))
            assertNull(shapeX.findRayIntersection(
                position, Position.meters(0, 0, z), Vector3f(0.5f * sqrt(2f), 0.5f * sqrt(2f), 0f),
                Distance.kiloMeters(1)
            ))
            assertNull(shapeX.findRayIntersection(
                position, Position.meters(0, 300, z), Vector3f(1f, 0f, 0f), Distance.kiloMeters(1)
            ))
        }

        for (x in arrayOf(50, 120, 130, 140, 1000)) {
            assertNull(shapeZ.findRayIntersection(
                position, Position.meters(x, 0, 0), Vector3f(0f, 0f, 1f), Distance.kiloMeters(1)
            ))
            assertNull(shapeZ.findRayIntersection(
                position, Position.meters(x, 0, 0), Vector3f(0f, 0.5f * sqrt(2f), 0.5f * sqrt(2f)),
                Distance.kiloMeters(1)
            ))
            assertNull(shapeZ.findRayIntersection(
                position, Position.meters(x, 300, 0), Vector3f(0f, 0f, 1f), Distance.kiloMeters(1)
            ))
        }

        for (offset in arrayOf(-100, -10, 0, 10, 100)) {
            assertNull(shapeD.findRayIntersection(
                position, Position.meters(offset, 0, offset), Vector3f(1f, 0f, 1f).normalize(), Distance.kiloMeters(1)
            ))
            assertNull(shapeD.findRayIntersection(
                position, Position.meters(offset, 0, offset), Vector3f(1f).normalize(), Distance.kiloMeters(1)
            ))
            assertNull(shapeD.findRayIntersection(
                position, Position.meters(offset, 300, offset), Vector3f(1f, 0f, 1f).normalize(), Distance.kiloMeters(1)
            ))
        }
    }

    @Test
    fun testFindRayIntersectionMiss() {
        val shape = VerticalPlaneShape(Distance.meters(20), Distance.meters(30), Angle.degrees(300))
        val position = Position.meters(100, 200, 300)

        // Complete miss
        assertNull(shape.findRayIntersection(
            position, Position.meters(0, 0, 0), Vector3f(1f, 0f, 0f), Distance.kiloMeters(1)
        ))

        // Just below
        assertNull(shape.findRayIntersection(
            position, Position.meters(0, 165, 300), Vector3f(1f, 0f, 0f), Distance.kiloMeters(1)
        ))

        // Just above
        assertNull(shape.findRayIntersection(
            position, Position.meters(0, 235, 300), Vector3f(1f, 0f, 0f), Distance.kiloMeters(1)
        ))

        // Z-coordinate slightly too low
        assertNull(shape.findRayIntersection(
            position, Position.meters(0, 190, 282), Vector3f(1f, 0f, 0f), Distance.kiloMeters(1)
        ))

        // Z-coordinate slightly too high
        assertNull(shape.findRayIntersection(
            position, Position.meters(0, 190, 318), Vector3f(1f, 0f, 0f), Distance.kiloMeters(1)
        ))

        // Ray is slightly too short
        assertNull(shape.findRayIntersection(
            position, Position.meters(0, 200, 300), Vector3f(1f, 0f, 0f), Distance.meters(99)
        ))

        // Plane is behind the ray
        assertNull(shape.findRayIntersection(
            position, Position.meters(130, 200, 300), Vector3f(1f, 0f, 0f), Distance.meters(100)
        ))
    }

    private fun assertHit(expected: Float, actual: Distance?) {
        assertTrue((Distance.meters(expected) - actual!!).milliMeters.absoluteValue < 1f)
    }

    @Test
    fun testFindRayIntersectionHit() {
        val shape = VerticalPlaneShape(Distance.meters(20), Distance.meters(30), Angle.degrees(300))
        val position = Position.meters(100, 200, 300)

        // Almost too low
        assertHit(100f, shape.findRayIntersection(
            position, Position.meters(0, 175, 300), Vector3f(1f, 0f, 0f), Distance.kiloMeters(1)
        ))

        // Almost above
        assertHit(100f, shape.findRayIntersection(
            position, Position.meters(0, 225, 300), Vector3f(1f, 0f, 0f), Distance.kiloMeters(1)
        ))

        // Z-coordinate almost too low
        assertHit(109.81495f, shape.findRayIntersection(
            position, Position.meters(0, 190, 283), Vector3f(1f, 0f, 0f), Distance.kiloMeters(1)
        ))

        // Z-coordinate almost too high
        assertHit(90.185045f, shape.findRayIntersection(
            position, Position.meters(0, 190, 317), Vector3f(1f, 0f, 0f), Distance.kiloMeters(1)
        ))

        // Ray is almost too short
        assertHit(100f, shape.findRayIntersection(
            position, Position.meters(0, 200, 300), Vector3f(1f, 0f, 0f), Distance.meters(101)
        ))

        // Complex ray
        assertHit(352.56326f, shape.findRayIntersection(
            position, Position.meters(10, 10, 10), Vector3f(1f, 2f, 3f).normalize(), Distance.kiloMeters(1)
        ))
    }

    // The first implementation worked, except when the angle was exactly 0 degrees
    @Test
    fun testIntersectionWithZeroDegreeAngle() {
        val plane = VerticalPlaneShape(Distance.meters(50), Distance.meters(50), Angle.degrees(0))
        val position = Position.meters(0, 0, 100)

        for (x in arrayOf(-45, -20, -5, 0, 5, 20, 45)) {
            for (y in arrayOf(-45, -20, -5, 0, 5, 20, 45)) {
                assertHit(100f, plane.findRayIntersection(
                    position, Position.meters(x, y, 0), Vector3f(0f, 0f, 1f), Distance.meters(101)
                ))
            }
        }
    }
}
