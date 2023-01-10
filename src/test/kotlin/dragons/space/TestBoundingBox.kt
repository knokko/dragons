package dragons.space

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TestBoundingBox {

    @Test
    fun testConstructor() {
        val box = BoundingBox(Position.meters(3, -2, 1), Position.meters(-5, 6, -7))
        assertEquals(Position.meters(-5, -2, -7), box.min)
        assertEquals(Position.meters(3, 6, 1), box.max)
    }

    @Test
    fun testContains() {
        val box = BoundingBox(Position.meters(10, -20, 30), Position.meters(5, -10, 60))
        assertTrue(box.contains(Position.meters(10, -20, 30)))
        assertTrue(box.contains(Position.meters(5, -11, 60)))
        assertFalse(box.contains(Position.meters(5, -9, 60)))

        assertTrue(box.contains(Position.meters(8, -13, 50)))
        assertTrue(box.contains(Position.meters(6, -17, 35)))

        assertFalse(box.contains(Position.meters(0, -15, 40)))
        assertFalse(box.contains(Position.meters(6, -25, 10)))
        assertFalse(box.contains(Position.meters(100, 100, 100)))
        assertFalse(box.contains(Position.meters(-100, -100, -100)))
    }

    @Test
    fun testIntersects() {
        val box = BoundingBox(Position.meters(10, -20, 30), Position.meters(5, -10, 60))
        assertFalse(box.intersects(BoundingBox(Position.meters(100, 100, 100), Position.meters(200, 200, 200))))
        assertFalse(box.intersects(BoundingBox(Position.meters(100, 100, 100), Position.meters(100, 100, 100))))
        assertFalse(box.intersects(BoundingBox(Position.meters(-100, -100, -100), Position.meters(-200, -200, -200))))
        assertFalse(box.intersects(BoundingBox(Position.meters(-100, -100, -100), Position.meters(-100, -100, -100))))

        assertTrue(box.intersects(BoundingBox(Position.meters(0, -100, 0), Position.meters(100, 0, 100))))
        assertTrue(box.intersects(BoundingBox(Position.meters(6, -18, 55), Position.meters(8, -12, 40))))

        assertFalse(box.intersects(BoundingBox(Position.meters(2, 0, 20), Position.meters(30, 25, 50))))
        assertTrue(box.intersects(BoundingBox(Position.meters(2, -10, 20), Position.meters(30, 25, 50))))

        assertTrue(box.intersects(BoundingBox(Position.meters(8, -15, 40), Position.meters(8, -15, 40))))
        assertFalse(box.intersects(BoundingBox(Position.meters(8, -25, 40), Position.meters(8, -25, 40))))

        assertFalse(box.intersects(BoundingBox(Position.meters(-5, -20, 30), Position.meters(4, -10, 60))))
        assertTrue(box.intersects(BoundingBox(Position.meters(-5, -20, 30), Position.meters(5, -10, 60))))
        assertTrue(box.intersects(BoundingBox(Position.meters(-5, -20, 30), Position.meters(8, -10, 60))))
        assertTrue(box.intersects(BoundingBox(Position.meters(-5, -20, 30), Position.meters(10, -10, 60))))
        assertTrue(box.intersects(BoundingBox(Position.meters(-5, -20, 30), Position.meters(100, -10, 60))))
    }
}
