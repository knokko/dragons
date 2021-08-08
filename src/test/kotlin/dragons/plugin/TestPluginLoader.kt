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
        testSimplePluginLoader(simpleTestPluginContent)
    }

    // NOTE: This test will fail before running ./gradlew shadowJar
    @Test
    fun testSimpleClassesPluginLoader() {
        val simpleTestPluginContent = runBlocking { scanDirectoriesOfClasses(listOf(
            File("test-plugins/simple/build/classes/java/main"),
            File("test-plugins/simple/build/classes/kotlin/main")
        )) }
        testSimplePluginLoader(simpleTestPluginContent)
    }

    private fun testSimplePluginLoader(content: JarContent) {
        val classLoader = PluginClassLoader(content)
        val pluginManager = PluginManager(classLoader.magicInstances)

        val loadListeners = pluginManager.getImplementations(PluginsLoadedListener::class.java)
        assertEquals(1, loadListeners.size)
        loadListeners.first().afterPluginsLoaded()
        pluginManager.getImplementations(PluginsLoadedListener::class.java).first().afterPluginsLoaded()

        assertEquals(2,
            Class.forName("dragons.plugins.test.simple.SimplePluginStore", true, classLoader)
                .getField("testCounter").get(null)
        )
    }

    @Test
    fun testTwinJarPluginLoader() {
        val twinContentA = scanJar(JarInputStream(ClassLoader.getSystemResourceAsStream("dragons/plugins/test/twinA.jar")))
        val twinContentB = scanJar(JarInputStream(ClassLoader.getSystemResourceAsStream("dragons/plugins/test/twinB.jar")))

        val twinContent = JarContent.merge(twinContentA, twinContentB)
        testTwinPluginLoader(twinContent)
    }

    // NOTE: This test will fail before running ./gradlew shadowJar
    @Test
    fun testTwinClassesPluginLoader() {
        val twinPluginContent = runBlocking { scanDirectoriesOfClasses(listOf(
            File("test-plugins/twin-a/build/classes/kotlin/main"),
            File("test-plugins/twin-a/build/resources/main"),
            File("test-plugins/twin-b/build/classes/kotlin/main"),
            File("test-plugins/twin-b/build/classes/java/main"),
            File("test-plugins/twin-b/build/resources/main")
        )) }
        testTwinPluginLoader(twinPluginContent)
    }

    private fun testTwinPluginLoader(twinContent: JarContent) {
        val classLoader = PluginClassLoader(twinContent)
        val pluginManager = PluginManager(classLoader.magicInstances)

        val loadListeners = pluginManager.getImplementations(PluginsLoadedListener::class.java)
        assertEquals(1, loadListeners.size)
        loadListeners.first().afterPluginsLoaded()
        pluginManager.getImplementations(PluginsLoadedListener::class.java).first().afterPluginsLoaded()

        assertEquals("abcdef",
            Class.forName("dragons.plugins.test.twin.TwinStore", true, classLoader)
                .getField("testString").get(null)
        )
    }
}
