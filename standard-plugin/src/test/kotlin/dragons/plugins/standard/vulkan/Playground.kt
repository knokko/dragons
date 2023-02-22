package dragons.plugins.standard.vulkan

import dragons.profiling.PerformanceProfiler
import dragons.profiling.PerformanceStorage
import gruviks.space.RectRegion
import gruviks.space.RectTree
import org.junit.jupiter.api.Assertions
import java.io.File
import java.lang.System.currentTimeMillis

fun main() {
    val storage = PerformanceStorage()
    val profiler = PerformanceProfiler(storage, 1)
    profiler.start()

    val startTime = currentTimeMillis()

//    val tree = TreeMap<String, String>()
//    for (counter in 0 until 800_000) {
//        tree["Key$counter"] = "Value$counter"
//        for (innerCounter in 0 until 10) Assertions.assertEquals("Value$counter", tree["Key$counter"])
//    }
    val tree = RectTree<Int>()
    for (counter in 0 until 700_000) {
        val newRect = RectRegion.percentage(
            10 * counter, 10 * counter, 10 * (counter + 1), 10 * (counter + 1)
        )
        tree.insert(counter, newRect)
        for (counter in 0 until 10) Assertions.assertEquals(1, tree.findBetween(newRect).size)
        Assertions.assertEquals(counter + 1, tree.size)
        Assertions.assertTrue(tree.depth < 50)
    }
    val endTime = currentTimeMillis()
    println("Took ${endTime - startTime} ms")

    profiler.stop()
    storage.dump(File("performance.log"), threshold = 0.0000)
    println("final depth is ${tree.depth}")
}
