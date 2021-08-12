package dragons.plugin

import dragons.plugin.interfaces.PluginInterface
import kotlin.reflect.KClass

class PluginManager(val magicInstances: Collection<Pair<PluginInterface, PluginInfo>>) {

    private val magicInterfaceMap = mutableMapOf<Class<*>, Collection<*>>()

    /**
     * Gets a list of all plugin classes that implement the given 'magic' interface. That interface must extend
     * PluginInterface!
     */
    @Synchronized
    fun <T: PluginInterface> getImplementations(magicInterface: Class<T>): Collection<Pair<T, PluginInfo>> {
        return magicInterfaceMap.getOrPut(magicInterface) {
            if (!implementsOrExtendsInterface(magicInterface, PluginInterface::class.java)) {
                throw IllegalArgumentException("$magicInterface doesn't extend PluginInterface")
            }

            val untypedResult = magicInstances.filter { candidateInstance ->
                implementsOrExtendsInterface(candidateInstance.first::class.java, magicInterface)
            }
            val result = ArrayList<Pair<T, PluginInfo>>(untypedResult.size)
            for (element in untypedResult) {
                result.add(element as Pair<T, PluginInfo>)
            }
            result
        } as Collection<Pair<T, PluginInfo>>
    }

    fun <T: PluginInterface> getImplementations(magicInterface: KClass<T>): Collection<Pair<T, PluginInfo>> {
        return getImplementations(magicInterface.java)
    }
}

private fun implementsOrExtendsInterface(candidate: Class<*>, desiredInterface: Class<*>): Boolean {
    return candidate.interfaces.any {
            candidateInterface -> candidateInterface == desiredInterface || implementsOrExtendsInterface(candidateInterface, desiredInterface)
    }
}
