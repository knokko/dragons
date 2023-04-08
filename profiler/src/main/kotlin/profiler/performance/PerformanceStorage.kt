package profiler.performance

import java.io.File
import java.io.PrintWriter
import java.util.concurrent.ConcurrentHashMap

class PerformanceStorage {

    private val threadTrees = ConcurrentHashMap<Long, PerformanceNode>()

    fun insert(threadID: Long, stackTrace: Array<StackTraceElement>) {
        threadTrees.getOrPut(threadID) { PerformanceNode() }.insert(stackTrace.reversedArray(), -1)
    }

    fun getAll() = threadTrees.map { entry -> Pair(entry.key, entry.value) }.sortedByDescending { it.second.getTotalCount() }

    fun dump(file: File, maxNumResults: Int = 5, threshold: Double = 0.1) {
        val writer = PrintWriter(file)
        for ((threadID, tree) in getAll()) {
            writer.println("Thread $threadID:")
            tree.print(writer, "  ", maxNumResults, threshold)
            writer.println()
        }
        writer.flush()
        writer.close()
    }
}
