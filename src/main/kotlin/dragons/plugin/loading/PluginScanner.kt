package dragons.plugin.loading

import dragons.init.GameInitProperties
import dragons.init.trouble.SimpleStartupException
import dragons.init.trouble.StartupException
import dragons.plugin.PluginInfo
import dragons.plugin.PluginInstance
import kotlinx.coroutines.*
import java.io.File
import java.nio.file.Files
import java.util.jar.JarInputStream

@Throws(StartupException::class)
suspend fun scanDefaultPluginLocations(gameInitProps: GameInitProperties): Collection<Pair<JarContent, PluginInstance>> {
    val pluginsFolder = File("plug-ins/")
    if (!pluginsFolder.isDirectory && !pluginsFolder.mkdirs()) {
        throw SimpleStartupException("Can't create plug-ins directory", listOf(
            "The plug-ins directory for this game ($pluginsFolder) doesn't seem to exist yet, so this game tried to create it.",
            "But... this failed for some reason..."
        ))
    }

    if (pluginsFolder.list()!!.isEmpty() && !gameInitProps.isInDevelopment) {
        // TODO Copy the standard plug-in jar to the plug-ins folder
        // Or perhaps it's better to always enable the standard plug-in unless some config flag is set
        // If that flag is not set, copy it to a temp folder
    }

    val thirdPartyContent = scanDirectoryOfJars(pluginsFolder, gameInitProps)

    // TODO New development plug-ins should be added to this list manually
    val developmentProjects = listOf("standard-plugin", "debug-plugin")

    val developmentContent = developmentProjects.map { name ->
        val buildDirectories = listOf(
            File("$name/build/classes/kotlin/main"),
            File("$name/build/classes/java/main"),
            File("$name/build/resources/main")
        )
        val projectContent = scanDirectoriesOfClasses(buildDirectories)
        Pair(projectContent, PluginInstance(PluginInfo(
            name = name.replace("-plugin", "")),
            gameInitProps = gameInitProps
        ))
    }

    return thirdPartyContent + developmentContent
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

suspend fun scanDirectoryOfJars(rootDirectory: File, gameInitProps: GameInitProperties): Collection<Pair<JarContent, PluginInstance>> = withContext(Dispatchers.IO) {
    val jarList = mutableListOf<Pair<JarContent, PluginInstance>>()
    scanJarOrDirectory(this, rootDirectory, jarList, gameInitProps)
    return@withContext jarList
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
                val bytes = Files.readAllBytes(file.toPath())
                synchronized(classByteMap) {
                    classByteMap[className] = bytes
                }
            }
        } else {
            scope.launch {
                val bytes = Files.readAllBytes(file.toPath())
                synchronized(resourceByteMap) {
                    resourceByteMap[resourceName] = bytes
                }
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
    scope: CoroutineScope, file: File, jarList: MutableCollection<Pair<JarContent, PluginInstance>>,
    gameInitProps: GameInitProperties
) {
    if (file.isFile) {
        if (file.name.endsWith(".jar")) {
            scope.launch {
                val jarContent = scanJar(JarInputStream(Files.newInputStream(file.toPath())))
                // TODO Give plug-ins the opportunity to choose a different name than their file name
                val pluginInstance = PluginInstance(
                    info = PluginInfo(name = file.name.substring(0 until file.name.length - 4)),
                    gameInitProps = gameInitProps
                )
                synchronized(jarList) {
                    jarList.add(Pair(jarContent, pluginInstance))
                }
            }
        }
    } else if (file.isDirectory) {
        val children = file.listFiles()!!
        for (child in children) {
            scope.launch {
                scanJarOrDirectory(scope, child, jarList, gameInitProps)
            }
        }
    }
}

/**
 * Represents the content (class and resources) of 1 JAR file
 */
class JarContent(val classByteMap: Map<String, ByteArray>, val resourceByteMap: Map<String, ByteArray>)
