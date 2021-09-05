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
}
