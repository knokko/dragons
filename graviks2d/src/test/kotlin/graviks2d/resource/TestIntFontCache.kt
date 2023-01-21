package graviks2d.resource

import graviks2d.resource.text.IntCache
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TestIntFontCache {

    @Test
    fun basicTest() {
        val cache = IntCache(size = 3, numSlotsPerIndex = 2, numLocks = 3)
        assertEquals(5, cache.getOrPut(0) { 5 })
        assertEquals(7, cache.getOrPut(1) { 7 })
        assertEquals(9, cache.getOrPut(2) { 9 })
        assertEquals(11, cache.getOrPut(5) { 11 })
        assertEquals(3, cache.getOrPut(900) { 3 })
    }

    @Test
    fun singleThreadedBattleTest() {
        for (size in 1 .. 5) {
            for (numSlotsPerIndex in 1 .. 5) {
                for (numLocks in 1 .. size) {
                    val cache = IntCache(size = size, numSlotsPerIndex = numSlotsPerIndex, numLocks = numLocks)

                    for (counter in 0 .. 3) {
                        for (key in 0 until 100) {
                            assertEquals(3 * key, cache.getOrPut(key) { 3 * key })
                        }
                    }
                }
            }
        }
    }

    @Test
    fun multiThreadedBattleTest() {
        for (size in 1 .. 5) {
            for (numSlotsPerIndex in 1 .. 5) {
                for (numLocks in 1 .. size) {
                    val cache = IntCache(size = size, numSlotsPerIndex = numSlotsPerIndex, numLocks = numLocks)

                    val numThreads = 20
                    val succeeded = BooleanArray(numThreads)
                    val threads = Array(numThreads) { index -> Thread {
                        for (counter in 0 .. 3) {
                            for (key in 0 until 100) {
                                assertEquals(3 * key, cache.getOrPut(key) { 3 * key })
                            }
                            succeeded[index] = false
                        }
                        succeeded[index] = true
                    }}

                    for (thread in threads) {
                        thread.start()
                    }

                    for ((index, thread) in threads.withIndex()) {
                        thread.join()
                        assertTrue(succeeded[index])
                    }
                }
            }
        }
    }
}
