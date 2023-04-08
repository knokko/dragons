package profiler.performance

import java.io.PrintWriter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class PerformanceNode {

    private val children = ConcurrentHashMap<StackTraceElement, PerformanceNode>()
    private val totalCount = AtomicLong(0L)

    fun insert(stackTrace: Array<StackTraceElement>, ownIndex: Int) {
        if (ownIndex + 1 >= stackTrace.size) throw IllegalArgumentException("stack trace is not long enough")

        this.totalCount.incrementAndGet()

        val child = children.getOrPut(stackTrace[ownIndex + 1]) { PerformanceNode() }
        if (ownIndex + 2 == stackTrace.size) child.totalCount.incrementAndGet()
        else child.insert(stackTrace, ownIndex + 1)
    }

    fun getTotalCount() = totalCount.get()

    fun getChildren() = children.map {
            entry -> Pair(entry.key, entry.value)
    }.sortedByDescending { it.second.totalCount.get() }

    fun print(output: PrintWriter, prefix: String, maxNumResults: Int, threshold: Double) {
        for ((index, entry) in getChildren().withIndex()) {
            if (index < maxNumResults) {
                val (key, child) = entry
                if (child.getTotalCount() >= threshold * this.getTotalCount()) {
                    output.println("$prefix${child.getTotalCount()}: ${key.className}.${key.methodName}(line ${key.lineNumber}):")
                    child.print(output, "$prefix ", maxNumResults, threshold)
                }
            }
        }
    }
}
