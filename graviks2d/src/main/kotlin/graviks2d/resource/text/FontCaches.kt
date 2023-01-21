package graviks2d.resource.text

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.random.Random

internal class IntCache(private val size: Int, private val numSlotsPerIndex: Int, private val numLocks: Int) {

    init {
        if (numLocks > size) throw IllegalArgumentException("numLocks ($numLocks) must not be larger than size ($size)")
        if (numLocks <= 0) throw IllegalArgumentException("numLocks($numLocks) must be positive")
        if (numSlotsPerIndex <= 0) throw IllegalArgumentException("numSlotsPerIndex ($numSlotsPerIndex) must be positive")
    }

    private val array = IntArray(size * 2 * numSlotsPerIndex) { -1 }
    private val locks = Array(numLocks) { ReentrantReadWriteLock(true) }
    private val rng = Random.Default

    fun getOrPut(key: Int, computeValue: () -> Int): Int {
        if (key < 0) throw IllegalArgumentException("key ($key) must be non-negative")

        val rawBaseArrayIndex = key % size
        val baseArrayIndex = rawBaseArrayIndex * 2 * numSlotsPerIndex
        val lock = locks[rawBaseArrayIndex % numLocks]

        // First check if the key is already stored. If so, return early
        lock.read {
            for (slotIndex in 0 until numSlotsPerIndex) {
                val arrayIndex = baseArrayIndex + 2 * slotIndex
                if (array[arrayIndex] == key) {
                    return array[arrayIndex + 1]
                }
            }
        }

        val value = computeValue()
        var writeSlotIndex = rng.nextInt(numSlotsPerIndex)

        // If the key is not yet stored, acquire the *write lock* to insert it
        lock.write {

            for (slotIndex in 0 until numSlotsPerIndex) {
                val arrayIndex = baseArrayIndex + 2 * slotIndex

                // Maybe, we are lucky and the value was inserted before we acquired the *write lock*
                if (array[arrayIndex] == key) {
                    return array[arrayIndex + 1]
                }

                // If there is an unused slot, use that instead of the randomly generated write slot
                if (array[arrayIndex] == -1) {
                    writeSlotIndex = slotIndex
                    break
                }
            }

            val writeArrayIndex = baseArrayIndex + 2 * writeSlotIndex
            array[writeArrayIndex] = key
            array[writeArrayIndex + 1] = value
        }

        return value
    }
}

internal class IntPairCache(private val size: Int, private val numSlotsPerIndex: Int, private val numLocks: Int) {

    init {
        if (numLocks > size) throw IllegalArgumentException("numLocks ($numLocks) must not be larger than size ($size)")
        if (numLocks <= 0) throw IllegalArgumentException("numLocks($numLocks) must be positive")
        if (numSlotsPerIndex <= 0) throw IllegalArgumentException("numSlotsPerIndex ($numSlotsPerIndex) must be positive")
    }

    private val array = IntArray(size * 3 * numSlotsPerIndex) { -1 }
    private val locks = Array(numLocks) { ReentrantReadWriteLock(true) }
    private val rng = Random.Default

    fun getOrPut(leftKey: Int, rightKey: Int, computeValue: () -> Int): Int {
        if (leftKey < 0 || rightKey < 0) throw IllegalArgumentException("keys ($leftKey, $rightKey) must be non-negative")

        val rawBaseArrayIndex = (leftKey + 37 * rightKey) % size
        val baseArrayIndex = rawBaseArrayIndex * 3 * numSlotsPerIndex
        val lock = locks[rawBaseArrayIndex % numLocks]

        // First check if the key is already stored. If so, return early
        lock.read {
            for (slotIndex in 0 until numSlotsPerIndex) {
                val arrayIndex = baseArrayIndex + 3 * slotIndex
                if (array[arrayIndex] == leftKey && array[arrayIndex + 1] == rightKey) return array[arrayIndex + 2]
            }
        }

        val value = computeValue()
        var writeSlotIndex = rng.nextInt(numSlotsPerIndex)

        // If the key is not yet stored, acquire the *write lock* to insert it
        lock.write {

            for (slotIndex in 0 until numSlotsPerIndex) {
                val arrayIndex = baseArrayIndex + 3 * slotIndex

                // Maybe, we are lucky and the value was inserted before we acquired the *write lock*
                if (array[arrayIndex] == leftKey && array[arrayIndex + 1] == rightKey) return array[arrayIndex + 2]

                // If there is an unused slot, use that instead of the randomly generated write slot
                if (array[arrayIndex] == -1) {
                    writeSlotIndex = slotIndex
                    break
                }
            }

            val writeArrayIndex = baseArrayIndex + 3 * writeSlotIndex
            array[writeArrayIndex] = leftKey
            array[writeArrayIndex + 1] = rightKey
            array[writeArrayIndex + 2] = value
        }

        return value
    }
}

internal class GlyphCache(private val size: Int, private val numSlotsPerIndex: Int, private val numLocks: Int) {

    init {
        if (numLocks > size) throw IllegalArgumentException("numLocks ($numLocks) must not be larger than size ($size)")
        if (numLocks <= 0) throw IllegalArgumentException("numLocks($numLocks) must be positive")
        if (numSlotsPerIndex <= 0) throw IllegalArgumentException("numSlotsPerIndex ($numSlotsPerIndex) must be positive")
    }

    private val array = Array<Pair<Int, GlyphShape>?>(size * numSlotsPerIndex) { null }
    private val locks = Array(numLocks) { ReentrantReadWriteLock(true) }
    private val rng = Random.Default

    fun borrow(key: Int, createGlyphShape: () -> GlyphShape, useGlyphShape: (GlyphShape) -> Unit) {
        if (key < 0) throw IllegalArgumentException("key ($key) must be non-negative")

        val rawBaseArrayIndex = key % size
        val baseArrayIndex = numSlotsPerIndex * rawBaseArrayIndex
        val lock = locks[rawBaseArrayIndex % numLocks]

        // First check if the key is already stored. If so, return early
        lock.read {
            for (slotIndex in 0 until numSlotsPerIndex) {
                val entry = array[baseArrayIndex + slotIndex]
                if (entry != null && entry.first == key) {
                    useGlyphShape(entry.second)
                    return
                }
            }
        }

        var writeSlotIndex = rng.nextInt(numSlotsPerIndex)

        // If the key is not yet stored, acquire the *write lock* to insert it
        lock.write {

            for (slotIndex in 0 until numSlotsPerIndex) {
                val arrayIndex = baseArrayIndex + slotIndex
                val entry = array[arrayIndex]

                // Maybe, we are lucky and the value was inserted before we acquired the *write lock*
                if (entry != null && entry.first == key) {
                    useGlyphShape(entry.second)
                    return
                }

                // If there is an unused slot, use that instead of the randomly generated write slot
                if (entry == null) {
                    writeSlotIndex = slotIndex
                    break
                }
            }

            val newGlyph = createGlyphShape()
            useGlyphShape(newGlyph)

            val writeArrayIndex = baseArrayIndex + writeSlotIndex

            val entryToRemove = array[writeArrayIndex]
            entryToRemove?.second?.destroy()

            array[writeArrayIndex] = Pair(key, newGlyph)
        }
    }

    fun destroy() {
        for (lock in locks) {
            lock.writeLock().lock()
        }

        for ((index, entry) in array.withIndex()) {
            if (entry != null) {
                entry.second.destroy()
                array[index] = null
            }
        }

        for (lock in locks) {
            lock.writeLock().unlock()
        }
    }
}
