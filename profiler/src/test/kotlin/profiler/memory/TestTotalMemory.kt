package profiler.memory

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TestTotalMemory {

    @Test
    fun testGetProcessMemoryUsage() {
        val processMemory = getProcessMemoryUsage()

        // I don't know exactly how much process memory this unit test will use, but it should be somewhere between
        // 1 MB and 1 GB
        assertTrue(processMemory > 1_000_000)
        assertTrue(processMemory < 1_000_000_000)
    }
}
