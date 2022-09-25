package dragons.util

import org.lwjgl.system.Platform
import java.lang.Thread.sleep
import java.lang.management.ManagementFactory
import java.lang.management.MemoryUsage
import java.nio.file.Files
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.absolutePathString

private fun formatMemory(value: Long) = String.format("%.3f MB", value.toDouble() / (1024.0 * 1024.0))

private fun formatMemoryUsage(usage: MemoryUsage) = "${formatMemory(usage.used)} (${formatMemory(usage.committed)}) / ${formatMemory(usage.max)}"

private class StatisticsTracker {

    private var isStopping = false
    // Since fpsCounter will be set, get, and incremented from difference threads, it should be atomic
    val fpsCounter = AtomicInteger(0)
    lateinit var thread: Thread

    fun start() {
        this.thread = Thread {

            val memoryScriptFile = if (Platform.get() == Platform.WINDOWS) { Files.createTempFile("", ".bat") } else { null }
            val pid = ProcessHandle.current().pid()
            val memoryBean = ManagementFactory.getMemoryMXBean()

            while (!isStopping) {
                val totalMemory: String
                if (Platform.get() == Platform.WINDOWS) {
                    val memoryQueryCommand = "tasklist /FI \"PID eq $pid\""
                    Files.writeString(memoryScriptFile, memoryQueryCommand)

                    val memoryQueryBuilder = ProcessBuilder(memoryScriptFile!!.absolutePathString())
                    val memoryQueryProcess = memoryQueryBuilder.start()
                    totalMemory = if (memoryQueryProcess.waitFor() == 0) {

                        val scannedInput = mutableListOf<String>()
                        val scanner = Scanner(memoryQueryProcess.inputStream)
                        while (scanner.hasNextLine()) {
                            scannedInput.add(scanner.nextLine())
                        }
                        scanner.close()

                        if (scannedInput.size == 6) {
                            val relevantLine = scannedInput[5]
                            val lastSpaceIndex = relevantLine.lastIndexOf(' ')
                            if (lastSpaceIndex != -1) {
                                val memorySpaceIndex = relevantLine.substring(0 until lastSpaceIndex).lastIndexOf(' ')
                                if (memorySpaceIndex != -1) {
                                    val memoryUsage = relevantLine.substring(memorySpaceIndex + 1)
                                    println(memoryUsage)
                                    memoryUsage
                                } else {
                                    "Unexpected memory query format (3)"
                                }
                            } else {
                                "Unexpected memory query format (2)"
                            }

                        } else {
                            "Unexpected memory query format (1)"
                        }
                    } else "Memory query failed"
                } else {
                    // TODO UNIX support
                    totalMemory = "Unsupported OS"
                }

                val onHeapMemory = formatMemoryUsage(memoryBean.heapMemoryUsage)
                val managedOffHeapMemory = formatMemoryUsage(memoryBean.nonHeapMemoryUsage)

                fpsCounter.set(0)

                sleep(1000)

                currentStatistics = PerformanceStatistics(
                    onHeapMemory = onHeapMemory,
                    managedOffHeapMemory = managedOffHeapMemory,
                    totalMemory = totalMemory,
                    fps = fpsCounter.get()
                )
            }
        }

        this.thread.start()
    }

    fun stop() {
        isStopping = true
    }
}

private val STATISTICS_TRACKER = StatisticsTracker()
private var currentStatistics = PerformanceStatistics("", "", "", 0)

class PerformanceStatistics internal constructor(
    val onHeapMemory: String,
    val managedOffHeapMemory: String,
    val totalMemory: String,
    val fps: Int
) {
    companion object {

        fun start() {
            STATISTICS_TRACKER.start()
        }

        fun markFrame() {
            STATISTICS_TRACKER.fpsCounter.incrementAndGet()
        }

        fun get() = currentStatistics

        fun stop() {
            STATISTICS_TRACKER.stop()
        }
    }
}
