package dsl.pm2.renderer

import dsl.pm2.interpreter.Pm2Model
import org.lwjgl.system.MemoryUtil.memByteBuffer
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.vulkan.VK10.*
import troll.instance.TrollInstance

class Pm2Allocations internal constructor(
    private val troll: TrollInstance
) {

    fun allocateMesh(model: Pm2Model): Pm2Mesh {
        // TODO Buffer sub-allocation
        val vertexBuffer = troll.buffers.createMapped(
            model.vertices.size * STATIC_VERTEX_SIZE.toLong(),
            VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, "Pm2MeshVertexBuffer"
        )

        val byteBuffer = memByteBuffer(vertexBuffer.hostAddress, model.vertices.size * STATIC_VERTEX_SIZE)
        for (vertex in model.vertices) {
            byteBuffer.putFloat(vertex.x)
            byteBuffer.putFloat(vertex.y)
            byteBuffer.putFloat(vertex.color.redF)
            byteBuffer.putFloat(vertex.color.greenF)
            byteBuffer.putFloat(vertex.color.blueF)
            byteBuffer.putInt(vertex.matrixIndex)
        }

        return Pm2Mesh(
            vertexBuffer = vertexBuffer.buffer, vertexOffset = 0,
            numVertices = model.vertices.size, matrices = model.matrices,
            dynamicParameterTypes = model.dynamicParameters
        )
    }

    fun destroyMesh(mesh: Pm2Mesh) {
        vmaDestroyBuffer(troll.vmaAllocator(), mesh.vertexBuffer.vkBuffer, mesh.vertexBuffer.vmaAllocation)
    }

    fun destroy() {

    }
}
