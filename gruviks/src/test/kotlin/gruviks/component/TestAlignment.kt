package gruviks.component

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestAlignment {

    private fun assertFloatPairEqual(expected: Pair<Float, Float>, actual: Pair<Float, Float>) {
        assertEquals(expected.first, actual.first, 0.001f)
        assertEquals(expected.second, actual.second, 0.001f)
    }

    @Test
    fun testComputeBoundsX() {
        assertFloatPairEqual(Pair(2f, 2.8f), computeBoundsX(2f, 3f, 0.8f, HorizontalComponentAlignment.Left))
        assertFloatPairEqual(Pair(-4.8f, -4f), computeBoundsX(-5f, -4f, 0.8f, HorizontalComponentAlignment.Right))
        assertFloatPairEqual(Pair(-0.4f, 1.4f), computeBoundsX(-1f, 2f, 1.8f, HorizontalComponentAlignment.Middle))
    }

    @Test
    fun testComputeBoundsY() {
        assertFloatPairEqual(Pair(0.5f, 1.2f), computeBoundsY(0.5f, 2.7f, 0.7f, VerticalComponentAlignment.Bottom))
        assertFloatPairEqual(Pair(-1.23f, 0f), computeBoundsY(-4.3f, 0f, 1.23f, VerticalComponentAlignment.Top))
        assertFloatPairEqual(Pair(0.3f, 0.7f), computeBoundsY(0f, 1f, 0.4f, VerticalComponentAlignment.Middle))
    }
}
