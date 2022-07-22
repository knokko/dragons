package gruviks.space

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestPoint {

    @Test
    fun testFraction() {
        val point = Point.fraction(2, 3, 10)
        assertEquals(Coordinate.fraction(2, 10), point.x)
        assertEquals(Coordinate.fraction(3, 10), point.y)
    }

    @Test
    fun testPercentage() {
        val point = Point.percentage(10, 80)
        assertEquals(Coordinate.percentage(10), point.x)
        assertEquals(Coordinate.percentage(80), point.y)
    }
}
