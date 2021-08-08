package dragons.plugin.loading

import dragons.plugin.PluginURLHandler
import dragons.plugin.interfaces.PluginInterface
import java.lang.reflect.Modifier
import java.net.URL

class PluginClassLoader(
    private var classByteMap: Map<String, ByteArray>?,
    private val resourceMap: Map<String, ByteArray>
): ClassLoader() {

    constructor(content: JarContent) : this(content.classByteMap, content.resourceByteMap)

    private val classMap = mutableMapOf<String, Class<*>>()
    private val urlHandler = PluginURLHandler(resourceMap)

    val magicInstances: Collection<PluginInterface>

    private fun getOrCreateClass(name: String, knownBytes: ByteArray?): Class<*> {

        val existing = classMap[name]
        if (existing != null) return existing

        val classByteCode = if (knownBytes == null && classByteMap != null) {
            classByteMap!![name]
        } else {
            knownBytes
        }

        if (classByteCode != null) {
            val newClass = defineClass(name, classByteCode, 0, classByteCode.size)
            classMap[name] = newClass
            return newClass
        }

        throw ClassNotFoundException(name)
    }

    init {
        if (classByteMap == null) {
            throw IllegalArgumentException("classByteMap must not be null")
        }

        for ((name, bytes) in classByteMap!!) {
            getOrCreateClass(name, bytes)
        }

        // We don't need them anymore after class loading has finished, so allow the garbage collector to free them
        classByteMap = null

        val collectMagicClasses = mutableListOf<PluginInterface>()
        for (definedClass in ArrayList(classMap.values)) {
            resolveClass(definedClass)

            if (!definedClass.isInterface && isMagic(definedClass)) {
                if ((definedClass.modifiers and Modifier.FINAL) == 0) {
                    throw InvalidPluginException("All classes that implement PluginInterface must be final, but ${definedClass.name} isn't")
                }

                try {
                    collectMagicClasses.add(definedClass.getConstructor().newInstance() as PluginInterface)
                } catch (noSuitableConstructor: NoSuchMethodException) {
                    throw InvalidPluginException("All classes that implement PluginInterface must have a public constructor without parameters, but ${definedClass.name} doesn't")
                }
            }
        }

        magicInstances = List(collectMagicClasses.size) { index -> collectMagicClasses[index] }
    }

    private fun isMagic(candidate: Class<*>): Boolean {
        if (candidate == PluginInterface::class.java) {
            return true
        }

        for (child in candidate.interfaces) {
            if (isMagic(child)) {
                return true
            }
        }

        return false
    }

    override fun findClass(name: String): Class<*> {
        return getOrCreateClass(name, null)
    }

    override fun findResource(name: String?): URL {
        val resourceBytes = resourceMap[name]
        return if (resourceBytes != null) {
            URL("dragonsplugin", "pluginname", 0, name, urlHandler)
        } else {
            super.findResource(name)
        }
    }
}
