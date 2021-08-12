package dragons.plugin

import dragons.plugin.interfaces.general.PluginsLoadedListener
import dragons.plugin.loading.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import java.util.jar.JarInputStream

class TestPluginLoader {

    @Test
    fun testSimpleJarPluginLoader() {
        val simpleTestPluginContent =
            scanJar(JarInputStream(ClassLoader.getSystemResourceAsStream("dragons/plugins/test/simple.jar")))
        testSimplePluginLoader(listOf(Pair(simpleTestPluginContent, PluginInfo(name = "simple"))))
    }

    // NOTE: This test will fail before running ./gradlew shadowJar
    @Test
    fun testSimpleClassesPluginLoader() {
        val simpleTestPluginContent = runBlocking { scanDirectoriesOfClasses(listOf(
            File("test-plugins/simple/build/classes/java/main"),
            File("test-plugins/simple/build/classes/kotlin/main")
        )) }
        testSimplePluginLoader(listOf(Pair(simpleTestPluginContent, PluginInfo(name = "simple"))))
    }

    private fun testSimplePluginLoader(content: Collection<Pair<JarContent, PluginInfo>>) {
        val classLoader = PluginClassLoader(content)
        val pluginManager = PluginManager(classLoader.magicInstances)

        val loadListeners = pluginManager.getImplementations(PluginsLoadedListener::class.java)
        assertEquals(1, loadListeners.size)
        loadListeners.first().first.afterPluginsLoaded()
        pluginManager.getImplementations(PluginsLoadedListener::class.java).first().first.afterPluginsLoaded()

        assertEquals(2,
            Class.forName("dragons.plugins.test.simple.SimplePluginStore", true, classLoader)
                .getField("testCounter").get(null)
        )
    }

    @Test
    fun testTwinJarPluginLoader() {
        val twinContentA = scanJar(JarInputStream(ClassLoader.getSystemResourceAsStream("dragons/plugins/test/twinA.jar")))
        val twinContentB = scanJar(JarInputStream(ClassLoader.getSystemResourceAsStream("dragons/plugins/test/twinB.jar")))
        testTwinPluginLoader(twinContentA, twinContentB)
    }

    // NOTE: This test will fail before running ./gradlew shadowJar
    @Test
    fun testTwinClassesPluginLoader() {
        val twinPluginContentA = runBlocking { scanDirectoriesOfClasses(listOf(
            File("test-plugins/twin-a/build/classes/kotlin/main"),
            File("test-plugins/twin-a/build/resources/main"),
        )) }
        val twinPluginContentB = runBlocking { scanDirectoriesOfClasses(listOf(
            File("test-plugins/twin-b/build/classes/kotlin/main"),
            File("test-plugins/twin-b/build/classes/java/main"),
            File("test-plugins/twin-b/build/resources/main")
        )) }
        testTwinPluginLoader(twinPluginContentA, twinPluginContentB)
    }

    private fun testTwinPluginLoader(twinContentA: JarContent, twinContentB: JarContent) {
        val twinContent = listOf(
            Pair(twinContentA, PluginInfo(name = "twinA")),
            Pair(twinContentB, PluginInfo(name = "twinB"))
        )
        val classLoader = PluginClassLoader(twinContent)
        val pluginManager = PluginManager(classLoader.magicInstances)

        val loadListeners = pluginManager.getImplementations(PluginsLoadedListener::class.java)
        assertEquals(1, loadListeners.size)
        loadListeners.first().first.afterPluginsLoaded()
        pluginManager.getImplementations(PluginsLoadedListener::class.java).first().first.afterPluginsLoaded()

        assertEquals("abcdef",
            Class.forName("dragons.plugins.test.twin.TwinStore", true, classLoader)
                .getField("testString").get(null)
        )
    }
}
