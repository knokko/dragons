package profiler.memory

import java.lang.Long.parseLong
import java.nio.file.Files
import java.util.*
import kotlin.io.path.absolutePathString

internal val isWindows = System.getProperty("os.name").lowercase().contains("win")

private val memoryScriptFile = if (isWindows) {
    Files.createTempFile("", ".bat")
} else { null }

private val pid = ProcessHandle.current().pid()

@Throws(UnsupportedOperationException::class)
fun getProcessMemoryUsage(): Long {
    if (isWindows) {
        val memoryQueryCommand = "tasklist /FI \"PID eq $pid\""
        Files.writeString(memoryScriptFile, memoryQueryCommand)

        val memoryQueryBuilder = ProcessBuilder(memoryScriptFile!!.absolutePathString())
        val memoryQueryProcess = memoryQueryBuilder.start()
        val exitCode = memoryQueryProcess.waitFor()
        if (exitCode == 0) {

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

                        if (!memoryUsage.endsWith(" K")) {
                            throw UnsupportedOperationException("Expected $memoryUsage to end with \" K\"")
                        }

                        val rawMemoryUsage = memoryUsage.substring(0 until memoryUsage.length - 2)
                            .replace(",", "").replace(".", "")

                        return 1024 * parseLong(rawMemoryUsage)
                    } else throw UnsupportedOperationException("Unexpected memory query format (3): $relevantLine")
                } else throw UnsupportedOperationException("Unexpected memory query format (2): $relevantLine")
            } else throw UnsupportedOperationException("Unexpected memory query format (1): $scannedInput")
        } else throw UnsupportedOperationException("Memory query failed with exit code $exitCode")
    } else {
        val memoryUsageProcess = Runtime.getRuntime().exec("ps -q $pid -eo size")
        val exitCode = memoryUsageProcess.waitFor()
        if (exitCode == 0) {
            val scannedInput = mutableListOf<String>()
            val scanner = Scanner(memoryUsageProcess.inputStream)
            while (scanner.hasNextLine()) {
                scannedInput.add(scanner.nextLine())
            }
            scanner.close()

            if (scannedInput.size != 3) throw UnsupportedOperationException("Unexpected memory query format (4)")
            if (!scannedInput[0].contains("SIZE")) throw UnsupportedOperationException("Unexpected memory query format (5)")
            try {
                val dummyUsage = parseLong(scannedInput[2].trim())
                if (dummyUsage != 0L) throw UnsupportedOperationException("Unexpected memory query format (6)")
                return 1024 * parseLong(scannedInput[1].trim())
            } catch (invalid: NumberFormatException) {
                throw UnsupportedOperationException("Unexpected memory query format (7)")
            }
        } else throw UnsupportedOperationException("Memory query failed with exit code $exitCode")
    }
}
