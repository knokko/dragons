package graviks2d.context

import graviks2d.core.GraviksInstance
import graviks2d.util.Color
import org.lwjgl.vulkan.VK10.*

class GraviksContext(
    val instance: GraviksInstance,
    val width: Int,
    val height: Int,
    val depthMode: DepthMode,

    initialBackgroundColor: Color,

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
    private var maxDepth = -1


    private val commands: ContextCommands

    init {
        if (depthMode == DepthMode.AutomaticSlow) {
            throw UnsupportedOperationException("AutomaticSlow still needs to be implemented")
        }

        this.commands = ContextCommands(this)
        clear(initialBackgroundColor)
    }

    fun fillRect(minX: Float, minY: Float, maxX: Float, maxY: Float, color: Color) {

    }

    fun setDepth(newDepth: Int) {
        if (depthMode != DepthMode.Manual) {
            throw UnsupportedOperationException("setDepth is only allowed when depthMode is Manual")
        }
        currentDepth = newDepth
        if (currentDepth > maxDepth) {
            throw IllegalArgumentException("Can't set depth to $newDepth because maxDepth is $maxDepth. Call setMaxDepth first")
        }
    }

    fun setMaxDepth(newMaxDepth: Int) {
        if (depthMode != DepthMode.Manual) {
            throw UnsupportedOperationException("setDepth is only allowed when depthMode is Manual")
        }
        this.maxDepth = newMaxDepth
    }

    fun flush() {
        commands.draw(ehm, maxDepth)
        // TODO What to do with the depth?
    }

    fun clear(clearColor: Color) {
        commands.clearDepthImage()
        // TODO Clear the color target image
        currentDepth = 1
        maxDepth = currentDepth
    }

    fun copyColorImageTo(destImage: Long?, destBuffer: Long?) {
        flush()
        commands.copyColorImageTo(destImage = destImage, destBuffer = destBuffer)
    }

    fun destroy() {
        commands.destroy()
        targetImages.destroy()
        buffers.destroy()
    }
}
