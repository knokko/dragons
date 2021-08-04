package dragons.plugin.loading

import kotlinx.coroutines.*
import java.io.File
import java.nio.file.Files
import java.util.jar.JarInputStream

fun scanDevelopmentClasses() {

}

fun scanReleaseJars() {

}

fun scanJar(jarStream: JarInputStream): JarContent {
    var currentEntry = jarStream.nextJarEntry

    val classMap = mutableMapOf<String, ByteArray>()
    val resourceMap = mutableMapOf<String, ByteArray>()

    while (currentEntry != null) {
        if (currentEntry.size < 1_000_000_000) {
            // Skip META-INF
            if (!currentEntry.name.startsWith("META-INF")) {
                if (currentEntry.name.endsWith(".class")) {
                    classMap[classPathToName(currentEntry.name)] = jarStream.readAllBytes()
                } else if (!currentEntry.isDirectory) {
                    resourceMap[currentEntry.name] = jarStream.readAllBytes()
                }
            }
        } else {
            throw IllegalArgumentException("Skipping ridiculously big entry ${currentEntry.name}")
        }
        currentEntry = jarStream.nextJarEntry
    }
    jarStream.close()

    return JarContent(classMap, resourceMap)
}

fun classPathToName(classPath: String): String {
    return classPath.substring(0 until classPath.length - 6).replace('/', '.')
}

suspend fun scanDirectoryOfJars(rootDirectory: File): JarContent = withContext(Dispatchers.IO) {
    val classByteMap = mutableMapOf<String, ByteArray>()
    val resourceByteMap = mutableMapOf<String, ByteArray>()
    scanJarOrDirectory(this, rootDirectory, classByteMap, resourceByteMap)
    return@withContext JarContent(classByteMap, resourceByteMap)
}

suspend fun scanDirectoriesOfClasses(rootDirectories: Collection<File>): JarContent = withContext(Dispatchers.IO) {
    val classByteMap = mutableMapOf<String, ByteArray>()
    val resourceByteMap = mutableMapOf<String, ByteArray>()
    for (rootDirectory in rootDirectories) {
        if (!rootDirectory.isDirectory) {
            throw IllegalArgumentException("$rootDirectory is not a directory")
        }
        for (rootChild in rootDirectory.listFiles()!!) {
            scanClassOrResourceOrDirectory(this, rootChild, "", classByteMap, resourceByteMap)
        }
    }
    return@withContext JarContent(classByteMap, resourceByteMap)
}

suspend fun scanDirectoryOfClasses(rootDirectory: File): JarContent {
    return scanDirectoriesOfClasses(listOf(rootDirectory))
}

private fun scanClassOrResourceOrDirectory(
    scope: CoroutineScope, file: File, parentRelativePath: String,
    classByteMap: MutableMap<String, ByteArray>, resourceByteMap: MutableMap<String, ByteArray>
) {
    if (file.isFile) {
        val resourceName = "$parentRelativePath${file.name}"
        if (file.name.endsWith(".class")) {
            val className = classPathToName(resourceName)
            scope.launch {
                classByteMap[className] = Files.readAllBytes(file.toPath())
            }
        } else {
            scope.launch {
                resourceByteMap[resourceName] = Files.readAllBytes(file.toPath())
            }
        }
    } else if (file.isDirectory) {
        val newRelativePath = "$parentRelativePath${file.name}/"
        for (childFile in file.listFiles()!!) {
            scope.launch {
                scanClassOrResourceOrDirectory(scope, childFile, newRelativePath, classByteMap, resourceByteMap)
            }
        }
    }
}

private fun scanJarOrDirectory(
    scope: CoroutineScope, file: File, classByteMap: MutableMap<String, ByteArray>, resourceByteMap: MutableMap<String, ByteArray>
) {
    if (file.isFile) {
        if (file.name.endsWith(".jar")) {
            scope.launch {
                val jarContent = scanJar(JarInputStream(Files.newInputStream(file.toPath())))
                synchronized(classByteMap) {
                    classByteMap.putAll(jarContent.classByteMap)
                }
                synchronized(resourceByteMap) {
                    resourceByteMap.putAll(jarContent.resourceByteMap)
                }
            }
        }
    } else if (file.isDirectory) {
        val children = file.listFiles()!!
        for (child in children) {
            scope.launch {
                scanJarOrDirectory(scope, child, classByteMap, resourceByteMap)
            }
        }
    }
}

/**
 * Represents the content (class and resources) of 1 or more JAR files
 */
class JarContent(val classByteMap: Map<String, ByteArray>, val resourceByteMap: Map<String, ByteArray>)
