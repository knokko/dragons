package dragons.util

import java.io.PrintStream
import kotlin.collections.ArrayList

const val HISTORY_SIZE = 1000

private val standardOutputHistory = Array<String?>(HISTORY_SIZE) { null }
private var standardOutputHistoryIndex = 0

private var cachedResult: List<String>? = null

private var didInit = false

fun getStandardOutputHistory(maxLines: Int): List<String> {
    return synchronized(standardOutputHistory) {

        if (!didInit) {
            initHistoryLogger()
        }

        if (cachedResult == null) {
            val result = ArrayList<String>(maxLines)
            var readIndex = standardOutputHistoryIndex
            while (standardOutputHistory[readIndex] != null && result.size < maxLines && result.size < HISTORY_SIZE) {
                result.add(standardOutputHistory[readIndex]!!)
                readIndex -= 1
                if (readIndex < 0) {
                    readIndex = HISTORY_SIZE - 1
                }
            }
            cachedResult = result.reversed().toList()
        }

        cachedResult!!
    }
}

private fun initHistoryLogger() {
    System.setOut(StandardOutputLogger())
    didInit = true
}

class StandardOutputLogger: PrintStream(System.out) {
    override fun print(x: String) {
        super.print(x)

        synchronized(standardOutputHistory) {

            fun append(value: String) {
                if (standardOutputHistory[standardOutputHistoryIndex] == null) {
                    standardOutputHistory[standardOutputHistoryIndex] = ""
                }
                standardOutputHistory[standardOutputHistoryIndex] += value
            }

            var nextIndex = 0
            while (true) {
                val indexSeparator = x.indexOf(System.lineSeparator(), nextIndex)
                if (indexSeparator == -1) break

                append(x.substring(nextIndex until indexSeparator))
                nextIndex = indexSeparator + System.lineSeparator().length
                standardOutputHistoryIndex += 1
                if (standardOutputHistoryIndex >= HISTORY_SIZE) {
                    standardOutputHistoryIndex = 0
                }
                standardOutputHistory[standardOutputHistoryIndex] = ""
            }

            append(x.substring(nextIndex))

            cachedResult = null
        }
    }

    private fun startNewLine() {
        synchronized(standardOutputHistory) {
            if (standardOutputHistory[standardOutputHistoryIndex] == null) {
                standardOutputHistory[standardOutputHistoryIndex] = ""
            }

            standardOutputHistoryIndex += 1
            if (standardOutputHistoryIndex >= HISTORY_SIZE) {
                standardOutputHistoryIndex = 0
            }

            standardOutputHistory[standardOutputHistoryIndex] = ""

            cachedResult = null
        }
    }

    override fun println(x: Int) {
        super.println(x)
        startNewLine()
    }

    override fun println(x: Any?) {
        super.println(x)
        startNewLine()
    }

    override fun println(x: Boolean) {
        super.println(x)
        startNewLine()
    }

    override fun println(x: Char) {
        super.println(x)
        startNewLine()
    }

    override fun println(x: CharArray) {
        super.println(x)
        startNewLine()
    }

    override fun println(x: Double) {
        super.println(x)
        startNewLine()
    }

    override fun println(x: Float) {
        super.println(x)
        startNewLine()
    }

    override fun println(x: Long) {
        super.println(x)
        startNewLine()
    }

    override fun println() {
        super.println()
        startNewLine()
    }

    override fun println(x: String?) {
        super.println(x)
        startNewLine()
    }
}
