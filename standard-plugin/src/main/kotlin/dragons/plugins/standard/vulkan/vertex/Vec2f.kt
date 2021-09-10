package dragons.plugins.standard.vulkan.vertex

import org.lwjgl.system.MemoryUtil.memGetFloat
import org.lwjgl.system.MemoryUtil.memPutFloat

@JvmInline
value class Vec2f(val address: Long) {

    var x: Float
    get() = memGetFloat(address + OFFSET_X)
    set(value) = memPutFloat(address + OFFSET_X, value)

    var y: Float
    get() = memGetFloat(address + OFFSET_Y)
    set(value) = memPutFloat(address + OFFSET_Y, value)

    companion object {
        // 2 floats and 1 float consists of 4 bytes
        const val SIZE = 8

        const val OFFSET_X = 0
        const val OFFSET_Y = 4
    }
}
