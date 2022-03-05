package graviks2d.context

import graviks2d.core.GraviksInstance
import graviks2d.util.Color

class GraviksContext(
    val instance: GraviksInstance,
    val targetImageView: Long,
    val width: Int,
    val height: Int,
    val depthMode: DepthMode,

    /**
     * The initial maximum number of vertices that fit in the vertex buffer. Adding
     * more vertices will cause a reallocation.
     */
    private var vertexBufferSize: Int = 5000,
    /**
     * The size of the operation buffer, in **bytes**
     */
    private var operationBufferSize: Int = 25000
) {

    private var vertexBuffer = instance.memory.createVertexBuffer(vertexBufferSize)

    private var currentDepth = -1
    private var maxDepth = -1

    init {
        // TODO Determine background color
        clear()

        if (depthMode == DepthMode.AutomaticSlow) {
            throw UnsupportedOperationException("AutomaticSlow still needs to be implemented")
        }
    }

    fun fillRect(minX: Float, minY: Float, maxX: Float, maxY: Float, color: Color) {

    }

    fun setDepth(newDepth: Int) {
        if (depthMode != DepthMode.Manual) {
            throw UnsupportedOperationException("setDepth is only allowed when depthMode is Manual")
        }
        currentDepth = newDepth
        if (currentDepth > maxDepth) {
            maxDepth = currentDepth
        }
    }

    fun flush() {

    }

    fun clear() {
        // TODO Actually clear the target image view
        currentDepth = 1
        maxDepth = currentDepth
    }

    fun destroy() {
        vertexBuffer.destroy(instance.memory.vmaAllocator)
    }
}
