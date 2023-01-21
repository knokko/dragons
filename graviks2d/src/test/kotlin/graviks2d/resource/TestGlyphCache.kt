package graviks2d.resource

import graviks2d.resource.text.GlyphCache
import graviks2d.resource.text.GlyphShape
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.lwjgl.stb.STBTTFontinfo

class TestGlyphCache {

    private val dummyFont = STBTTFontinfo.create()

    private fun createGlyph(advanceWidth: Int): () -> GlyphShape = { GlyphShape(dummyFont, null, advanceWidth) }

    @Test
    fun basicTest() {
        val cache = GlyphCache(size = 3, numSlotsPerIndex = 2, numLocks = 3)
        cache.borrow(0, createGlyph(5)) { assertEquals(5, it.advanceWidth) }
        cache.borrow(1, createGlyph(7)) { assertEquals(7, it.advanceWidth) }
        cache.borrow(2, createGlyph(9)) { assertEquals(9, it.advanceWidth) }
        cache.borrow(5, createGlyph(11)) { assertEquals(11, it.advanceWidth) }
        cache.borrow(900, createGlyph(3)) { assertEquals(3, it.advanceWidth) }
        cache.destroy()
    }

    @Test
    fun singleThreadedBattleTest() {
        for (size in 1 .. 5) {
            for (numSlotsPerIndex in 1 .. 5) {
                for (numLocks in 1 .. size) {
                    val cache = GlyphCache(size = size, numSlotsPerIndex = numSlotsPerIndex, numLocks = numLocks)

                    for (counter in 0 .. 3) {
                        for (key in 0 until 100) {
                            cache.borrow(key, createGlyph(3 * key)) {
                                assertEquals(3 * key, it.advanceWidth)
                            }
                        }
                    }

                    cache.destroy()
                }
            }
        }
    }

    @Test
    fun multiThreadedBattleTest() {
        for (size in 1 .. 5) {
            for (numSlotsPerIndex in 1 .. 5) {
                for (numLocks in 1 .. size) {
                    val cache = GlyphCache(size = size, numSlotsPerIndex = numSlotsPerIndex, numLocks = numLocks)

                    val numThreads = 20
                    val succeeded = BooleanArray(numThreads)
                    val threads = Array(numThreads) { index -> Thread {
                        for (counter in 0 .. 3) {
                            for (key in 0 until 100) {
                                cache.borrow(key, createGlyph(3 * key)) {
                                    assertEquals(3 * key, it.advanceWidth)
                                }
                            }
                            succeeded[index] = false
                        }
                        cache.destroy()
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
