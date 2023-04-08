package profiler.memory

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TestMemorySnapshot {

    @Test
    fun testTake() {
        val memory = MemorySnapshot.take()
        val unknown = memory.unknownMemory

        // I don't know what the exact value should be, but I'm pretty sure it should be between 1 MB and 1 GB
        assertTrue(unknown!! > 1_000_000)
        assertTrue(unknown < 1_000_000_000)
    }
}
