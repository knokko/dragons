package dragons.vulkan.util

import org.lwjgl.system.Struct
import org.lwjgl.vulkan.VkBaseOutStructure

fun combineNextChains(a: VkBaseOutStructure?, b: VkBaseOutStructure?): VkBaseOutStructure? {
    return if (a != null) {
        if (b != null) {
            endOfNextChain(a).pNext(b)
        }
        a
    } else {
        b
    }
}

fun endOfNextChain(chain: VkBaseOutStructure): VkBaseOutStructure {
    var end = chain
    while (end.pNext() != null) {
        end = end.pNext()!!
    }
    return end
}

fun findInNextChain(chain: Struct?, sType: Int): VkBaseOutStructure? {
    if (chain == null) {
        return null
    }
    var currentStruct: VkBaseOutStructure? = VkBaseOutStructure.create(chain.address())
    while (currentStruct != null) {
        if (currentStruct.sType() == sType) {
            return currentStruct
        }
        currentStruct = currentStruct.pNext()
    }
    return null
}
