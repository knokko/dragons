package gruviks.space

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestCoordinate {

    @Test
    fun testFraction() {
        assertEquals(
            Coordinate.fraction(3, 10),
            Coordinate.fraction(1, 10) + Coordinate.fraction(2, 10)
        )
        assertEquals(
            Coordinate.fraction(4, 10),
            Coordinate.fraction(4, 1) / 10
        )
        assertEquals(
            Coordinate.fraction(80_000_000_000_000_000L, 10_000_000_000_000_000L),
            Coordinate.fraction(8, 1)
        )
    }

    @Test
    fun testPercentage() {
        assertEquals(
            Coordinate.fraction(1, 2),
            Coordinate.percentage(50)
        )
        assertEquals(
            Coordinate.fraction(2, 10),
            Coordinate.percentage(20)
        )
    }

    @Test
    fun testMultiplication() {
        assertEquals(
            Coordinate.fraction(3, 5),
            Coordinate.fraction(3, 10) * 2
        )
        assertEquals(
            Coordinate.fraction(3, -5),
            Coordinate.fraction(-3, 10) * 2
        )
        assertEquals(
            Coordinate.fraction(7, 50),
            Coordinate.fraction(7, -50) * -1
        )
    }

    @Test
    fun testAddition() {
        assertEquals(
            Coordinate.fraction(12, 5),
            Coordinate.fraction(3, 5) + Coordinate.fraction(9, 5)
        )
        assertEquals(
            Coordinate.fraction(-2, 4),
            Coordinate.fraction(1, 2) + Coordinate.fraction(-40, 40)
        )
        assertEquals(
            Coordinate.fraction(-14, 5),
            Coordinate.fraction(-7, 5) + Coordinate.fraction(70, -50)
        )
    }

    @Test
    fun testSubtraction() {
        assertEquals(
            Coordinate.fraction(4, 5),
            Coordinate.fraction(17, 17) - Coordinate.fraction(1, 5)
        )
        assertEquals(
            Coordinate.fraction(0, 13),
            Coordinate.fraction(-10, 2) - Coordinate.fraction(10, -2)
        )
    }

    @Test
    fun testDivision() {
        assertEquals(
            Coordinate.percentage(3),
            Coordinate.fraction(3, 10) / 10
        )
        assertEquals(
            Coordinate.fraction(-8, 25),
            Coordinate.fraction(8, 5) / -5
        )
        assertEquals(
            Coordinate.fraction(15, 20),
            Coordinate.fraction(-15, 10) / -2
        )
    }
}
