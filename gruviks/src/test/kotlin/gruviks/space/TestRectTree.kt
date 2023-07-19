package gruviks.space

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.random.Random

class TestRectTree {

    @Test
    fun testSimple() {
        val tree = RectTree<String>(3)
        assertEquals(0, tree.size)
        assertEquals(1, tree.depth)
        assertTrue(tree.findBetween(RectRegion.percentage(10, 20, 30, 40)).isEmpty())

        tree.insert("test1", RectRegion.percentage(0, 0, 50, 50))
        assertEquals(1, tree.size)
        assertEquals(1, tree.depth)
        assertEquals(1, tree.findBetween(RectRegion.percentage(10, 20, 30, 40)).size)

        tree.insert("test2", RectRegion.percentage(50, 0, 100, 50))
        assertEquals(2, tree.size)
        assertEquals(1, tree.depth)
        assertEquals(1, tree.findBetween(RectRegion.percentage(10, 20, 30, 40)).size)
        assertEquals(2, tree.findBetween(RectRegion.percentage(10, 20, 60, 40)).size)

        tree.insert("test3", RectRegion.percentage(20, 50, 200, 80))
        assertEquals(3, tree.size)
        assertEquals(1, tree.depth)

        tree.insert("test4", RectRegion.percentage(30, 80, 50, 150))
        assertEquals(4, tree.size)
        assertEquals(2, tree.depth)

        assertEquals(1, tree.findBetween(RectRegion.percentage(10, 20, 30, 40)).size)
        assertEquals(2, tree.findBetween(RectRegion.percentage(10, 20, 60, 40)).size)
        assertEquals(1, tree.findBetween(RectRegion.percentage(30, 50, 200, 60)).size)
        assertEquals(1, tree.findBetween(RectRegion.percentage(30, 80, 50, 150)).size)
        assertEquals(4, tree.findBetween(RectRegion.percentage(0, 0, 100, 100)).size)

        tree.remove("test2", RectRegion.percentage(50, 0, 100, 50))
        assertEquals(3, tree.size)
        assertEquals(0, tree.findBetween(RectRegion.percentage(50, 0, 100, 50)).size)
    }

    @Test
    fun testOverlapCheck() {
        val tree = RectTree<String>(3)
        tree.insert("1", RectRegion.percentage(0, 0, 50, 50))
        assertThrows<IllegalArgumentException> { tree.insert("sdf", RectRegion.percentage(49, 40, 52, 70)) }
    }

    @Test
    fun randomBattleTest() {
        class NaiveRectTree<T> {

            val entries = mutableListOf<Pair<RectRegion, T>>()

            val size: Int
                get() = entries.size

            fun insert(element: T, region: RectRegion) {
                if (entries.any { it.first.overlaps(region) }) throw IllegalArgumentException("Overlap")
                entries.add(Pair(region, element))
            }

            fun findBetween(region: RectRegion) = entries.filter { it.first.overlaps(region) }
        }

        val naiveTree = NaiveRectTree<Int>()
        val complexTree = RectTree<Int>(11)

        val rng = Random(1234)
        for (counter in 0 until 10_000) {
            val minX = Coordinate.percentage(rng.nextInt(2000) - 1000)
            val minY = Coordinate.percentage(rng.nextInt(2000) - 1000)
            val rect = RectRegion(
                minX, minY,
                minX + Coordinate.percentage(1 + rng.nextInt(50)),
                minY + Coordinate.percentage(1 + rng.nextInt(50))
            )

            val correctOverlappingRegions = naiveTree.findBetween(rect)
            val numOverlappingRegions = correctOverlappingRegions.size
            val foundOverlappingRegions = complexTree.findBetween(rect)
            assertEquals(numOverlappingRegions, foundOverlappingRegions.size)

            if (numOverlappingRegions == 0) {
                naiveTree.insert(counter, rect)
                complexTree.insert(counter, rect)
            }

            assertEquals(naiveTree.size, complexTree.size)
        }

        assertTrue(naiveTree.size > 1000)
        println("final size is ${complexTree.size} and final depth is ${complexTree.depth}")

        var expectedSize = naiveTree.size
        for ((region, element) in naiveTree.entries) {
            assertEquals(listOf(region), complexTree.findBetween(region).map { it.first })
            complexTree.remove(element, region)
            assertEquals(0, complexTree.findBetween(region).size)
            expectedSize -= 1
            assertEquals(expectedSize, complexTree.size)
        }
        assertEquals(0, complexTree.size)
    }

    @Test
    fun testBalance() {
        val tree = RectTree<Int>(47)
        for (counter in 0 until 300_000) {
            val newRect = RectRegion.percentage(
                10 * counter, 10 * counter, 10 * (counter + 1), 10 * (counter + 1)
            )
            tree.insert(counter, newRect)
            for (innerCounter in 0 until 10) assertEquals(1, tree.findBetween(newRect).size)
            assertEquals(counter + 1, tree.size)
            assertTrue(tree.depth < 50)
        }
        println("final depth is ${tree.depth}")
    }
}
