package dragons.plugin

import dragons.plugin.interfaces.PluginInterface
import kotlin.reflect.KClass

class PluginManager(val magicInstances: Collection<PluginInterface>) {

    private val magicInterfaceMap = mutableMapOf<Class<*>, Collection<*>>()

    /**
     * Gets a list of all plugin classes that implement the given 'magic' interface. That interface must extend
     * PluginInterface!
     */
    @Synchronized
    fun <T: PluginInterface> getImplementations(magicInterface: Class<T>): Collection<T> {
        return magicInterfaceMap.getOrPut(magicInterface) {
            if (!implementsOrExtendsInterface(magicInterface, PluginInterface::class.java)) {
                throw IllegalArgumentException("$magicInterface doesn't extend PluginInterface")
            }

            val untypedResult = magicInstances.filter { candidateInstance ->
                implementsOrExtendsInterface(candidateInstance::class.java, magicInterface)
            }
            val result = ArrayList<T>(untypedResult.size)
            for (element in untypedResult) {
                result.add(element as T)
            }
            result
        } as Collection<T>
    }

    fun <T: PluginInterface> getImplementations(magicInterface: KClass<T>): Collection<T> {
        return getImplementations(magicInterface.java)
    }
}

private fun implementsOrExtendsInterface(candidate: Class<*>, desiredInterface: Class<*>): Boolean {
    return candidate.interfaces.any {
            candidateInterface -> candidateInterface == desiredInterface || implementsOrExtendsInterface(candidateInterface, desiredInterface)
    }
}
