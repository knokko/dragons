package gruviks.component

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TestRenderResult {

    @Test
    fun testWithinBounds() {
        val region = RectangularDrawnRegion(2f, 0f, 3f, 1.5f)
        assertTrue(region.isWithinBounds(2.1f, 0.1f))
        assertTrue(region.isWithinBounds(2.9f, 1.4f))
        assertFalse(region.isWithinBounds(1.9f, 0.1f))
        assertFalse(region.isWithinBounds(3.1f, 0.1f))
        assertFalse(region.isWithinBounds(2.1f, -0.1f))
        assertFalse(region.isWithinBounds(2.1f, 1.6f))
        assertFalse(region.isWithinBounds(0f, -1f))
    }

    @Test
    fun testRectangularDrawnRegion() {
        val region = RectangularDrawnRegion(0.2f, -0.5f, 1.4f, 0.1f)
        assertEquals(0.2f, region.minX)
        assertEquals(-0.5f, region.minY)
        assertEquals(1.4f, region.maxX)
        assertEquals(0.1f, region.maxY)

        assertTrue(region.isInside(0.3f, -0.2f))
        assertFalse(region.isInside(0.1f, 0f))
        assertFalse(region.isInside(1f, 0.2f))
        assertFalse(region.isInside(0f, -1f))
    }

    @Test
    fun testRoundedRectangularDrawnRegion() {
        val region = RoundedRectangularDrawnRegion(5f, 1f, 8f, 2f, 0.2f, 0.5f)
        assertEquals(5f, region.minX)
        assertEquals(1f, region.minY)
        assertEquals(8f, region.maxX)
        assertEquals(2f, region.maxY)
        assertEquals(0.2f, region.radiusX)
        assertEquals(0.5f, region.radiusY)

        // Top-left corner
        assertTrue(region.isInside(5.2f, 1.9f))
        assertFalse(region.isInside(5.05f, 1.9f))
        assertTrue(region.isWithinBounds(5.05f, 1.9f))

        // Bottom-left corner
        assertTrue(region.isInside(5.2f, 1.1f))
        assertFalse(region.isInside(5.05f, 1.1f))
        assertTrue(region.isWithinBounds(5.1f, 1.1f))

        // Top-right corner
        assertTrue(region.isInside(7.8f, 1.9f))
        assertFalse(region.isInside(7.95f, 1.9f))
        assertTrue(region.isWithinBounds(7.95f, 1.9f))

        // Bottom-right corner
        assertTrue(region.isInside(7.8f, 1.1f))
        assertFalse(region.isInside(7.95f, 1.1f))
        assertTrue(region.isWithinBounds(7.95f, 1.1f))

        // Top edge
        assertTrue(region.isInside(6f, 1.9f))
        assertFalse(region.isInside(6f, 2.1f))
        assertFalse(region.isWithinBounds(6f, 2.1f))

        // Bottom edge
        assertTrue(region.isInside(6f, 1.1f))
        assertFalse(region.isInside(6f, 0.9f))
        assertFalse(region.isWithinBounds(6f, 0.9f))

        // Left edge
        assertTrue(region.isInside(5.1f, 1.5f))
        assertFalse(region.isInside(4.9f, 1.5f))
        assertFalse(region.isWithinBounds(4.9f, 1.5f))

        // Right edge
        assertTrue(region.isInside(7.9f, 1.5f))
        assertFalse(region.isInside(8.1f, 1.5f))
        assertFalse(region.isWithinBounds(8.1f, 1.5f))
    }

    @Test
    fun testCompositeDrawnRegion() {
        val region = CompositeDrawnRegion(listOf(
            RectangularDrawnRegion(-3f, -2f, -1f, -1f),
            RectangularDrawnRegion(1f, 0f, 3f, 1f)
        ))
        assertEquals(-3f, region.minX)
        assertEquals(-2f, region.minY)
        assertEquals(3f, region.maxX)
        assertEquals(1f, region.maxY)

        assertTrue(region.isInside(-2f, -1.5f))
        assertFalse(region.isInside(0f, 0f))
        assertTrue(region.isWithinBounds(0f, 0f))
        assertTrue(region.isInside(2f, 0.5f))

        assertFalse(region.isInside(-4f, -1.5f))
        assertFalse(region.isWithinBounds(-4f, -1.5f))
        assertFalse(region.isInside(4f, 0.5f))
        assertFalse(region.isWithinBounds(4f, 0.5f))
    }

    @Test
    fun testTransformedDrawnRegion() {
        val margin = 0.001f
        val region = TransformedDrawnRegion(
            RectangularDrawnRegion(0.1f, 0.5f, 0.9f, 1f),
            0.5f, 1.5f, 1f, 2f
        )

        assertEquals(0.55f, region.minX, margin)
        assertEquals(1.75f, region.minY, margin)
        assertEquals(0.95f, region.maxX, margin)
        assertEquals(2f, region.maxY, margin)

        assertTrue(region.isInside(0.56f, 1.76f))
        assertFalse(region.isInside(0.54f, 1.76f))
        assertFalse(region.isInside(0.56f, 1.74f))

        assertTrue(region.isInside(0.94f, 1.99f))
        assertFalse(region.isInside(0.96f, 1.99f))
        assertFalse(region.isInside(0.94f, 2.01f))
    }
}
