package dragons.plugins.standard.vulkan.vertex

import org.lwjgl.system.MemoryUtil.*
import java.nio.ByteBuffer

@JvmInline
value class BasicVertex(val address: Long) {

    val position: Vec3f
    get() = Vec3f(address + OFFSET_BASE_POSITION)

    val normal: Vec3f
    get() = Vec3f(address + OFFSET_BASE_NORMAL)

    val colorTextureCoordinates: Vec2f
    get() = Vec2f(address + OFFSET_COLOR_TEXTURE_COORDINATES)

    val heightTextureCoordinates: Vec2f
    get() = Vec2f(address + OFFSET_HEIGHT_TEXTURE_COORDINATES)

    var matrixIndex: Int
    get() = memGetInt(address + OFFSET_MATRIX_INDEX)
    set(value) = memPutInt(address + OFFSET_MATRIX_INDEX, value)

    var materialIndex: Int
    get() = memGetInt(address + OFFSET_MATERIAL_INDEX)
    set(value) = memPutInt(address + OFFSET_MATERIAL_INDEX, value)

    /**
     * The distance (in meters) of 1 unit distance on the height texture. This is needed to determine the slope of
     * locations on the height map.
     */
    var deltaFactor: Float
    get() = memGetFloat(address + OFFSET_DELTA_FACTOR)
    set(value) = memPutFloat(address + OFFSET_DELTA_FACTOR, value)

    companion object {

        const val OFFSET_BASE_POSITION = 0
        const val OFFSET_BASE_NORMAL = OFFSET_BASE_POSITION + Vec3f.SIZE
        const val OFFSET_COLOR_TEXTURE_COORDINATES = OFFSET_BASE_NORMAL + Vec3f.SIZE
        const val OFFSET_HEIGHT_TEXTURE_COORDINATES = OFFSET_COLOR_TEXTURE_COORDINATES + Vec2f.SIZE
        const val OFFSET_MATRIX_INDEX = OFFSET_HEIGHT_TEXTURE_COORDINATES + Vec2f.SIZE
        const val OFFSET_MATERIAL_INDEX = OFFSET_MATRIX_INDEX + Int.SIZE_BYTES
        const val OFFSET_DELTA_FACTOR = OFFSET_MATERIAL_INDEX + Int.SIZE_BYTES
        const val OFFSET_COLOR_TEXTURE_INDEX = OFFSET_DELTA_FACTOR + Float.SIZE_BYTES
        const val OFFSET_HEIGHT_TEXTURE_INDEX = OFFSET_COLOR_TEXTURE_INDEX + Int.SIZE_BYTES

        const val SIZE = OFFSET_HEIGHT_TEXTURE_INDEX + Int.SIZE_BYTES

        const val MATERIAL_TERRAIN = 0
        const val MATERIAL_PLASTIC = 1
        const val MATERIAL_METAL = 2

        fun createArray(buffer: ByteBuffer, position: Int, length: Long): Array<BasicVertex> {
            if (position < 0) throw IllegalArgumentException("position ($position) < 0")
            val boundIndex = position + length * SIZE
            if (length > 0 && boundIndex > buffer.capacity()) {
                throw IllegalArgumentException(
                    "Creating this array would allow buffer overflow: boundIndex = $boundIndex, capacity = ${buffer.capacity()}"
                )
            }
            if (boundIndex < position) throw IllegalArgumentException("Size computation caused integer overflow")

            val startAddress = memAddress(buffer, position)
            return Array(length.toInt()) { index -> BasicVertex(startAddress + index * SIZE) }
        }
    }
}
