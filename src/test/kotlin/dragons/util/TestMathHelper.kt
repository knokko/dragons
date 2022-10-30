package dragons.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestMathHelper {

    @Test
    fun testNextMultipleOf() {
        assertEquals(15, nextMultipleOf(5, 15))
        assertEquals(16, nextMultipleOf(4, 15))
        assertEquals(0, nextMultipleOf(3, 0))
        assertEquals(7, nextMultipleOf(7, 7))
        assertEquals(5, nextMultipleOf(5, 3))
    }

    @Test
    fun testNextPowerOf2() {
        assertEquals(1, nextPowerOf2(0))
        assertEquals(1, nextPowerOf2(1))
        assertEquals(2, nextPowerOf2(2))
        assertEquals(4, nextPowerOf2(3))
        assertEquals(4, nextPowerOf2(4))
        assertEquals(8, nextPowerOf2(5))
        assertEquals(8, nextPowerOf2(6))
        assertEquals(8, nextPowerOf2(7))
        assertEquals(8, nextPowerOf2(8))
        assertEquals(1024, nextPowerOf2(1024))
        val largePowerOf2 = 1024L * 1024L * 1024L * 1024L * 1024L
        assertEquals(largePowerOf2, nextPowerOf2(largePowerOf2 / 2 + 1))
    }
}
