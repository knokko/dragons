package gruviks.space

import org.junit.jupiter.api.Assertions.assertEquals
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
}
