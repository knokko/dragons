package dragons.plugins.standard.vulkan.model.generator

import dragons.plugins.standard.vulkan.vertex.BasicVertex
import java.nio.IntBuffer

class ModelGenerator(
    /**
     * The number of vertices needed by the model
     */
    val numVertices: Int,
    /**
     * The number of indices needed by the model, should be a multiple of 3
     */
    val numIndices: Int,
    /**
     * Stores the attributes of the `numVertices` vertices of this model in the given vertex buffer. This should be
     * done by assigning the properties of the elements in the given vertex list.
     *
     * All `colorTextureIndex` and `heightTextureIndex` values are supposed to be assigned to an element of
     * `colorImageIndices` and `heightImageIndices` respectively.
     */
    val fillVertexBuffer: (List<BasicVertex>, colorImageIndices: List<Int>, heightImageIndices: List<Int>) -> Unit,
    /**
     * Puts the `numIndices` indices of this model generator into the given index buffer. This function does **not**
     * need to `flip` or `rewind` the buffer.
     */
    val fillIndexBuffer: (IntBuffer) -> Unit
) {
    companion object {
        fun combineWithSharedTextures(models: Collection<ModelGenerator>): ModelGenerator {
            val numVertices = models.sumOf { it.numVertices }
            val numIndices = models.sumOf { it.numIndices }

            val modelList = models.toList()
            val fillVertexBuffer = { vertexBuffer: List<BasicVertex>, colorImageIndices: List<Int>, heightImageIndices: List<Int> ->
                var nextVertexIndex = 0
                for (model in modelList) {
                    model.fillVertexBuffer(
                        vertexBuffer.slice(nextVertexIndex until nextVertexIndex + model.numVertices),
                        colorImageIndices, heightImageIndices
                    )
                    nextVertexIndex += model.numVertices
                }
            }

            val fillIndexBuffer = { indexBuffer: IntBuffer ->
                var vertexOffset = 0
                var nextIndexIndex = 0 // I kinda dislike this variable name
                for (model in modelList) {
                    model.fillIndexBuffer(indexBuffer.slice(nextIndexIndex, model.numIndices))

                    // Increase all indices written by model.fillIndexBuffer to ensure they match the right vertex
                    for (indexToIncrease in nextIndexIndex until nextIndexIndex + model.numIndices) {
                        val oldIndex = indexBuffer[indexToIncrease]
                        if (oldIndex < 0 || oldIndex >= model.numVertices) {
                            throw IllegalArgumentException("Index $oldIndex out of range for model with ${model.numVertices} vertices")
                        }
                        indexBuffer.put(indexToIncrease, oldIndex + vertexOffset)
                    }

                    vertexOffset += model.numVertices
                    nextIndexIndex += model.numIndices
                }
            }

            return ModelGenerator(
                numVertices = numVertices,
                numIndices = numIndices,
                fillVertexBuffer = fillVertexBuffer,
                fillIndexBuffer = fillIndexBuffer
            )
        }
    }
}

fun IntBuffer.putTriangle(i1: Int, i2: Int, i3: Int) {
    put(i1)
    put(i2)
    put(i3)
}

fun IntBuffer.putQuad(bottomLeft: Int, bottomRight: Int, topRight: Int, topLeft: Int) {
    putTriangle(bottomLeft, bottomRight, topRight)
    putTriangle(topRight, topLeft, bottomLeft)
}
