package dragons.profiling

import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import java.lang.Thread.sleep
import java.lang.management.ManagementFactory
import kotlin.system.measureTimeMillis

class PerformanceProfiler(val storage: PerformanceStorage, private val sleepTime: Long) {

    private val threads = ManagementFactory.getThreadMXBean()
    private lateinit var ownThread: Thread
    private var shouldStop = false

    internal fun start() {
        
        ownThread = Thread {
            var sampleCounter = 0

            val spentTime = measureTimeMillis {
                while (!shouldStop) {
                    update()
                    sampleCounter += 1
                    sleep(sleepTime)
                }
            }

            val logger = getLogger(Logger.ROOT_LOGGER_NAME)
            logger.info("Took $sampleCounter performance samples in $spentTime ms with sleepTime $sleepTime")

        }
        ownThread.start()
    }

    private fun update() {
        val threadsToDump = threads.dumpAllThreads(false, false, 0).filter {
            it.threadState == Thread.State.RUNNABLE && it.threadId != Thread.currentThread().id
        }.map { it.threadId }.toLongArray()

        val dumps = threads.getThreadInfo(threadsToDump, false, false).filter { thread ->
            thread != null && thread.threadState == Thread.State.RUNNABLE && thread.stackTrace.any { it.className.contains("dragons") }
        }

        for (dump in dumps) {
            storage.insert(dump.threadId, dump.stackTrace)
        }
    }

    internal fun stop() {
        shouldStop = true
        ownThread.join()
    }
}
