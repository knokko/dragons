package dragons.plugin.loading

import dragons.init.trouble.SimpleStartupException
import dragons.init.trouble.StartupException
import kotlinx.coroutines.*
import java.io.File
import java.nio.file.Files
import java.util.jar.JarInputStream

@Throws(StartupException::class)
suspend fun scanDefaultPluginLocations(): JarContent {
    val pluginsFolder = File("plug-ins/")
    if (!pluginsFolder.isDirectory && !pluginsFolder.mkdirs()) {
        throw SimpleStartupException("Can't create plug-ins directory", listOf(
            "The plug-ins directory for this game ($pluginsFolder) doesn't seem to exist yet, so this game tried to create it.",
            "But... this failed for some reason..."
        ))
    }

    if (pluginsFolder.list()!!.isEmpty()) {
        // TODO Copy the standard plug-in jar to the plug-ins folder
        // But don't do that in development!
    }

    val thirdPartyContent = scanDirectoryOfJars(pluginsFolder)

    // TODO New plug-ins should be added to this list manually
    val developmentProjects = listOf("standard-plugin")
    val developmentBuildFolders = developmentProjects.flatMap { project -> listOf(
        File("$project/build/classes/kotlin/main"),
        File("$project/build/classes/java/main"),
        File("$project/build/resources/main")
    ) }

    val developmentContent = scanDirectoriesOfClasses(developmentBuildFolders)

    return JarContent.merge(thirdPartyContent, developmentContent)
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
        if (rootDirectory.exists()) {
            if (!rootDirectory.isDirectory) {
                throw IllegalArgumentException("$rootDirectory is not a directory")
            }
            for (rootChild in rootDirectory.listFiles()!!) {
                scanClassOrResourceOrDirectory(this, rootChild, "", classByteMap, resourceByteMap)
            }
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
class JarContent(val classByteMap: Map<String, ByteArray>, val resourceByteMap: Map<String, ByteArray>) {
    companion object {
        fun merge(vararg contentList: JarContent): JarContent {
            val classByteMap = mutableMapOf<String, ByteArray>()
            val resourceByteMap = mutableMapOf<String, ByteArray>()

            for (content in contentList) {
                classByteMap.putAll(content.classByteMap)
                resourceByteMap.putAll(content.resourceByteMap)
            }

            return JarContent(classByteMap, resourceByteMap)
        }
    }
}
