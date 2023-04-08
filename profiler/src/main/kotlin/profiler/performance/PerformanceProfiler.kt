package profiler.performance

import java.lang.Thread.sleep
import java.lang.management.ManagementFactory

class PerformanceProfiler(
    val storage: PerformanceStorage, private val sleepTime: Long,
    private val classNameFilter: (String) -> Boolean = { true }
) {

    private val threads = ManagementFactory.getThreadMXBean()
    private lateinit var ownThread: Thread
    private var shouldStop = false

    fun start() {
        ownThread = Thread {
            while (!shouldStop) {
                update()
                sleep(sleepTime)
            }
        }
        ownThread.start()
    }

    private fun update() {
        val threadsToDump = threads.dumpAllThreads(false, false, 0).filter {
            it.threadState == Thread.State.RUNNABLE && it.threadId != Thread.currentThread().id
        }.map { it.threadId }.toLongArray()

        val dumps = threads.getThreadInfo(threadsToDump, false, false).filter { thread ->
            thread != null && thread.threadState == Thread.State.RUNNABLE && thread.stackTrace.any { classNameFilter(it.className) }
        }

        for (dump in dumps) {
            storage.insert(dump.threadId, dump.stackTrace)
        }
    }

    fun stop() {
        shouldStop = true
        ownThread.join()
    }
}
