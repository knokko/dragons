package profiler.memory

import java.lang.management.ManagementFactory
import java.lang.management.MemoryUsage

private val memoryBean = ManagementFactory.getMemoryMXBean()

class MemorySnapshot(
    val heapMemory: MemoryUsage,
    val nonHeapMemory: MemoryUsage,
    val processMemory: Long?,
    val processMemoryFailedException: UnsupportedOperationException?
) {

    init {
        if ((processMemory == null) == (processMemoryFailedException == null)) {
            throw IllegalArgumentException("Exactly 1 of processMemory and processMemoryFailedException must be null")
        }
    }

    val unknownMemory: Long?
    get() = if (processMemory == null) null
            // On Windows, `getProcessMemoryUsage` will count the *used* (non-)heap memory
            else if (isWindows) processMemory - heapMemory.used - nonHeapMemory.used
            // On Linux, `getProcessMemoryUsage` will count the *committed* (non-)heap memory
            else processMemory - heapMemory.committed - nonHeapMemory.committed

    companion object {
        fun take(): MemorySnapshot {
            var processUsage: Long? = null
            var processMemoryFailedException: UnsupportedOperationException? = null
            try {
                processUsage = getProcessMemoryUsage()
            } catch (failed: UnsupportedOperationException) {
                processMemoryFailedException = failed
            }
            return MemorySnapshot(
                heapMemory = memoryBean.heapMemoryUsage, nonHeapMemory = memoryBean.nonHeapMemoryUsage,
                processMemory = processUsage, processMemoryFailedException = processMemoryFailedException
            )
        }
    }

    private fun format(value: Long) = String.format("%.3f MB", value.toDouble() / (1024.0 * 1024.0))

    private fun formatUsage(usage: MemoryUsage) = "(used: ${format(usage.used)}, committed: ${format(usage.committed)}, max: ${format(usage.max)})"

    override fun toString() = "MemorySnapshot(heap: ${formatUsage(heapMemory)}, non-heap: ${formatUsage(nonHeapMemory)}, process: ${if (processMemory != null) format(processMemory) else "unknown"}"

    fun debugDump() {
        println("Heap memory: ${formatUsage(heapMemory)}")
        println("Non-heap memory: ${formatUsage(nonHeapMemory)}")
        if (processMemory != null) {
            println("Process memory: ${format(processMemory)}")
            println("Unknown memory: ${format(unknownMemory!!)}")
        } else {
            println("Failed to get process memory: ${processMemoryFailedException!!.message}")
        }
    }
}
