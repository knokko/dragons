package gruviks.space

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TestRectRegion {

    @Test
    fun testFraction() {
        val region = RectRegion.fraction(-1, 0, 1, 2, 4)
        assertEquals(Coordinate.fraction(-1, 4), region.minX)
        assertEquals(Coordinate.fraction(0, 4), region.minY)
        assertEquals(Coordinate.fraction(1, 4), region.boundX)
        assertEquals(Coordinate.fraction(2, 4), region.boundY)
    }

    @Test
    fun testPercentage() {
        val region = RectRegion.percentage(15, 0, 30, 20)
        assertEquals(Coordinate.percentage(15), region.minX)
        assertEquals(Coordinate.percentage(0), region.minY)
        assertEquals(Coordinate.percentage(30), region.boundX)
        assertEquals(Coordinate.percentage(20), region.boundY)
    }

    @Test
    fun testInvalidConstruction() {
        assertThrows<IllegalArgumentException> { RectRegion.fraction(1, 1, -1, 2, 5) }
        assertThrows<IllegalArgumentException> { RectRegion.percentage(20, 50, 40, 40) }
    }

    @Test
    fun testOverlaps() {
        assertFalse(RectRegion.percentage(20, 20, 80, 80)
            .overlaps(RectRegion.percentage(100, 20, 120, 80)))
        assertFalse(RectRegion.percentage(20, 20, 80, 80)
            .overlaps(RectRegion.percentage(20, 100, 70, 200)))

        assertTrue(RectRegion.percentage(100, 80, 200, 150)
            .overlaps(RectRegion.percentage(70, 100, 120, 300)))
    }

    @Test
    fun testTransform() {
        val margin = 0.001f
        val region = RectRegion.percentage(20, 50, 60, 80)

        run {
            val (x, y) = region.transform(Point.percentage(25, 100))
            assertEquals(0.3f, x, margin)
            assertEquals(0.8f, y, margin)
        }

        run {
            val transformed = region.transform(RectRegion.percentage(-100, 10, 50, 30))
            assertEquals(-0.2f, transformed.minX, margin)
            assertEquals(0.53f, transformed.minY, margin)
            assertEquals(0.4f, transformed.maxX, margin)
            assertEquals(0.59f, transformed.maxY, margin)
        }
    }

    @Test
    fun testTransformBack() {
        val margin = 0.001f
        val region = RectRegion.percentage(50, 0, 100, 200)

        run {
            val (x, y) = region.transformBack(Point.percentage(70, 150))
            assertEquals(0.4f, x, margin)
            assertEquals(0.75f, y, margin)
        }

        run {
            val transformed = region.transformBack(RectRegion.percentage(20, 80, 150, 180))
            assertEquals(-0.6f, transformed.minX, margin)
            assertEquals(0.4f, transformed.minY, margin)
            assertEquals(2f, transformed.maxX, margin)
            assertEquals(0.9f, transformed.maxY, margin)
        }
    }
}
