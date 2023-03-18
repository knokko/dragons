package gruviks.glfw

import gruviks.component.RectangularDrawnRegion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.lwjgl.system.MemoryStack.stackPush
import kotlin.math.absoluteValue

class TestGruviksGLFW {

    @Test
    fun testAllocatePresentRegions() {
        fun assertEquals(expected: Int, actual: Int, margin: Int) {
            assertEquals(expected.toFloat(), actual.toFloat(), margin.toFloat() + 0.1f)
        }

        stackPush().use { stack ->
            val regionsToPresent = listOf(
                    RectangularDrawnRegion(-0.1f, -0.2f, 0.3f, 0.4f),
                    RectangularDrawnRegion(0.7f, 0.9f, 1.1f, 1f),
                    RectangularDrawnRegion(0.4f, 0.2f, 0.9f, 0.5f)
            )
            val result = allocatePresentRegions(stack, regionsToPresent, 500, 200)
            assertEquals(0, result.position())
            assertEquals(3, result.limit())
            result[0].run {
                assertEquals(0, offset().x())
                assertEquals(120, offset().y(), 2)
                assertEquals(150, extent().width(), 2)
                assertEquals(80, extent().height(), 2)
                assertEquals(200, offset().y() + extent().height())
            }
            result[1].run {
                assertEquals(350, offset().x(), 2)
                assertEquals(0, offset().y())
                assertEquals(150, extent().width(), 2)
                assertEquals(20, extent().height(), 2)
            }
            result[2].run {
                assertEquals(200, offset().x(), 2)
                assertEquals(100, offset().y(), 2)
                assertEquals(250, extent().width(), 2)
                assertEquals(60, extent().height(), 2)
            }
        }
    }
}
