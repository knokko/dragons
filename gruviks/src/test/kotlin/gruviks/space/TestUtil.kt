package gruviks.space

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TestUtil {

    @Test
    fun testWouldMultiplicationOverflow() {
        assertFalse(wouldMultiplicationOverflow(10, 3))

        // 1 Billion
        val b = 1_000_000_000L

        assertFalse(wouldMultiplicationOverflow(b, b))
        assertFalse(wouldMultiplicationOverflow(-b, b))
        assertFalse(wouldMultiplicationOverflow(-b, -b))

        assertTrue(wouldMultiplicationOverflow(10 * b, b))
        assertTrue(wouldMultiplicationOverflow(10 * b, -b))
        assertTrue(wouldMultiplicationOverflow(-10 * b, -b))

        assertTrue(wouldMultiplicationOverflow(10 * b, 10 * b))
        assertTrue(wouldMultiplicationOverflow(-10 * b, 10 * b))
        assertTrue(wouldMultiplicationOverflow(-10 * b, -10 * b))

        assertFalse(wouldMultiplicationOverflow(Long.MIN_VALUE, 1))
        assertTrue(wouldMultiplicationOverflow(Long.MIN_VALUE, -1))
        assertTrue(wouldMultiplicationOverflow(Long.MAX_VALUE, 2))
        assertTrue(wouldMultiplicationOverflow(Long.MIN_VALUE, 2))

        assertTrue(wouldMultiplicationOverflow(Long.MAX_VALUE, Long.MAX_VALUE))
        assertTrue(wouldMultiplicationOverflow(Long.MIN_VALUE, Long.MIN_VALUE))
    }

    @Test
    fun testDivideRounded() {
        assertEquals(0, divideRounded(0, 1))
        assertEquals(0, divideRounded(1, 3))
        assertEquals(1, divideRounded(2, 3))
        assertEquals(1, divideRounded(3, 3))
        assertEquals(0, divideRounded(-1, 3))
        assertEquals(-1, divideRounded(2, -3))
        assertEquals(-1, divideRounded(-3, 3))
        assertEquals(5, divideRounded(16, 3))
        assertEquals(5, divideRounded(-14, -3))
    }
}
