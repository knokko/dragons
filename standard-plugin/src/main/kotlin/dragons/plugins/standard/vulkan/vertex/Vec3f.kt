package dragons.plugins.standard.vulkan.vertex

import org.lwjgl.system.MemoryUtil.memGetFloat
import org.lwjgl.system.MemoryUtil.memPutFloat

@JvmInline
value class Vec3f(val address: Long) {

    var x: Float
    get() = memGetFloat(address + OFFSET_X)
    set(value) = memPutFloat(address + OFFSET_X, value)

    var y: Float
    get() = memGetFloat(address + OFFSET_Y)
    set(value) = memPutFloat(address + OFFSET_Y, value)

    var z: Float
    get() = memGetFloat(address + OFFSET_Z)
    set(value) = memPutFloat(address + OFFSET_Z, value)

    companion object {
        // 3 floats and each float is 4 bytes big
        const val SIZE = 3 * 4

        const val OFFSET_X = 0
        const val OFFSET_Y = 4
        const val OFFSET_Z = 8
    }
}