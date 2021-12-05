package dragons.plugins.standard.vulkan.model.generator

import dragons.plugins.standard.vulkan.vertex.BasicVertex
import java.nio.IntBuffer

class ModelGenerator(
    val numVertices: Int,
    val numIndices: Int,
    val fillVertexBuffer: (Array<BasicVertex>) -> Unit,
    val fillIndexBuffer: (IntBuffer) -> Unit
) {
    companion object {
        fun combine(models: Collection<ModelGenerator>): ModelGenerator {
            val numVertices = models.sumOf { it.numVertices }
            val numIndices = models.sumOf { it.numIndices }

            val modelList = models.toList()
            val fillVertexBuffer = { vertexBuffer: Array<BasicVertex> ->
                var nextVertexIndex = 0
                for (model in modelList) {
                    model.fillVertexBuffer(vertexBuffer.sliceArray(nextVertexIndex until nextVertexIndex + model.numVertices))
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
                        indexBuffer.put(indexToIncrease, indexBuffer[indexToIncrease] + vertexOffset)
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
