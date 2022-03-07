package graviks2d.context

import graviks2d.core.GraviksInstance
import graviks2d.util.Color
import java.lang.IllegalStateException

class GraviksContext(
    val instance: GraviksInstance,
    val width: Int,
    val height: Int,
    val depthPolicy: DepthPolicy,
    val translucentPolicy: TranslucentPolicy,

    initialBackgroundColor: Color,

    val maxDepth: Int = 1000,

    /**
     * The maximum number of **vertices** that can fit in the vertex buffer
     */
    vertexBufferSize: Int = 5000,

    /**
     * The maximum number of **int**s that can fit in the operation buffer
     */
    operationBufferSize: Int = 25000
) {

    internal val targetImages = ContextTargetImages(this)
    internal val buffers = ContextBuffers(this, vertexBufferSize, operationBufferSize)

    private var currentDepth = -1
    private var manualMaxDepth = -1

    private var currentVertexIndex = -1
    private var currentOperationIndex = -1

    private val commands: ContextCommands
    internal val descriptors = ContextDescriptors(this.instance, this.buffers.operationVkBuffer)

    init {
        if (depthPolicy == DepthPolicy.QuickCheck || depthPolicy == DepthPolicy.PreciseCheck) {
            throw UnsupportedOperationException("This DepthPolicy is not yet implemented")
        }

        if (translucentPolicy != TranslucentPolicy.Manual) {
            throw UnsupportedOperationException("This TranslucentPolicy is not yet implemented")
        }

        this.commands = ContextCommands(this)
        clear(initialBackgroundColor)
    }

    fun fillRect(minX: Float, minY: Float, maxX: Float, maxY: Float, color: Color) {

        // TODO This could use some abstraction

        // TODO Check that vertexIndex, depth, and operatinIndex are not too large
        val vertexIndex = this.currentVertexIndex
        this.currentVertexIndex += 6

        val depth = this.currentDepth
        if (this.depthPolicy != DepthPolicy.Manual) {
            if (this.depthPolicy == DepthPolicy.AlwaysIncrement) {
                this.currentDepth += 1
            } else {
                throw UnsupportedOperationException("Depth policy $depthPolicy is not yet supported")
            }
        }

        val operationIndex = this.currentOperationIndex
        this.currentOperationIndex += 2

        this.buffers.vertexCpuBuffer.run {
            this[vertexIndex].x = minX
            this[vertexIndex].y = minY

            this[vertexIndex + 1].x = maxX
            this[vertexIndex + 1].y = minY

            this[vertexIndex + 2].x = maxX
            this[vertexIndex + 2].y = maxY

            this[vertexIndex + 3].x = maxX
            this[vertexIndex + 3].y = maxY

            this[vertexIndex + 4].x = minX
            this[vertexIndex + 4].y = maxY

            this[vertexIndex + 5].x = minX
            this[vertexIndex + 5].y = minY

            for (index in 0 until 6) {
                this[vertexIndex + index].depth = depth
                this[vertexIndex + index].operationIndex = operationIndex
            }
        }

        this.buffers.operationCpuBuffer.run {
            this.put(operationIndex, 1)
            this.put(operationIndex + 1, color.rawValue)
        }
    }

    fun setDepth(newDepth: Int) {
        if (depthPolicy != DepthPolicy.Manual) {
            throw UnsupportedOperationException("setDepth is only allowed when depthMode is Manual")
        }
        if (newDepth > maxDepth) {
            throw IllegalArgumentException("Can't set depth to $newDepth because the absolute maxDepth is $maxDepth.")
        }
        if (newDepth > manualMaxDepth) {
            throw IllegalArgumentException("Can't set depth to $newDepth because the manual maxDepth is $manualMaxDepth. Call setManualMaxDepth first")
        }
        currentDepth = newDepth
    }

    fun setManualMaxDepth(newMaxDepth: Int) {
        if (depthPolicy != DepthPolicy.Manual) {
            throw UnsupportedOperationException("setManualMaxDepth is only allowed when depthMode is Manual")
        }
        if (newMaxDepth < 1) {
            throw IllegalArgumentException("new max depth must be positive, but it is $newMaxDepth")
        }
        this.manualMaxDepth = newMaxDepth
    }

    fun flush() {
        val drawMaxDepth = if (depthPolicy == DepthPolicy.Manual) {
            if (manualMaxDepth == -1) {
                throw IllegalStateException("You need to call setManualMaxDepth before flushing")
            }
            manualMaxDepth
        } else {
            currentDepth
        }

        if (currentVertexIndex < 0) {
            throw IllegalStateException("Current vertex index ($currentVertexIndex) must be non-negative")
        }
        if (currentOperationIndex < 0) {
            throw IllegalStateException("Current operation index ($currentOperationIndex) must be non-negative")
        }

        // If currentVertexIndex is 0, we can skip the actual drawing
        if (currentVertexIndex > 0) {
            commands.draw(currentVertexIndex, drawMaxDepth)
        }

        commands.clearDepthImage()
        if (depthPolicy != DepthPolicy.Manual) {
            currentDepth = 1
        }
        currentVertexIndex = 0
        currentOperationIndex = 0
    }

    fun clear(clearColor: Color) {
        commands.clearDepthImage()
        currentDepth = 1

        // TODO This could use some more abstraction
        this.buffers.vertexCpuBuffer.run {
            this[0].x = 0f
            this[0].y = 0f
            this[1].x = 1f
            this[1].y = 0f
            this[2].x = 1f
            this[2].y = 1f
            this[3].x = 1f
            this[3].y = 1f
            this[4].x = 0f
            this[4].y = 1f
            this[5].x = 0f
            this[5].y = 0f

            for (vertexIndex in 0 until 4) {
                this[vertexIndex].depth = 0
                this[vertexIndex].operationIndex = 0
            }
        }
        this.buffers.operationCpuBuffer.run {
            this.put(0, 1) // 1 is the op code for fill rect
            this.put(1, clearColor.rawValue)
        }
        currentVertexIndex = 6 // The first 6 vertices are used for the color clear
        currentOperationIndex = 2 // The first 2 operation values are used for the color clear
    }

    fun copyColorImageTo(destImage: Long?, destBuffer: Long?) {
        flush()
        commands.copyColorImageTo(destImage = destImage, destBuffer = destBuffer)
    }

    fun destroy() {
        commands.destroy()
        targetImages.destroy()
        descriptors.destroy()
        buffers.destroy()
    }
}
