package dragons.plugins.standard.vulkan.model.generator

import dragons.plugins.standard.vulkan.vertex.BasicVertex
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.lwjgl.system.MemoryStack.stackPush

class TestModelGenerator {

    @Test
    fun testCombineWithSharedTextures() {
        val generator1 = ModelGenerator(
            numVertices = 4,
            numIndices = 6,
            fillVertexBuffer = { vertexBuffer, _, _->
                vertexBuffer[0].matrixIndex = 1
                vertexBuffer[1].matrixIndex = 2
                vertexBuffer[2].matrixIndex = 3
                vertexBuffer[3].matrixIndex = 4
            },
            fillIndexBuffer = { indexBuffer ->
                indexBuffer.put(0)
                indexBuffer.put(1)
                indexBuffer.put(2)
                indexBuffer.put(2)
                indexBuffer.put(3)
                indexBuffer.put(0)
            }
        )

        val generator2 = ModelGenerator(
            numVertices = 3,
            numIndices = 3,
            fillVertexBuffer = { vertexBuffer, _, _ ->
                vertexBuffer[0].matrixIndex = 5
                vertexBuffer[1].matrixIndex = 6
                vertexBuffer[2].matrixIndex = 7
            },
            fillIndexBuffer = { indexBuffer ->
                indexBuffer.put(1)
                indexBuffer.put(2)
                indexBuffer.put(0)
            }
        )

        val combinedGenerator = ModelGenerator.combineWithSharedTextures(listOf(generator1, generator2, generator1, generator2))
        assertEquals(14, combinedGenerator.numVertices)
        assertEquals(18, combinedGenerator.numIndices)

        stackPush().use { stack ->
            val vertexBuffer = BasicVertex.createArray(stack.calloc(14 * BasicVertex.SIZE), 0, 14)
            combinedGenerator.fillVertexBuffer(vertexBuffer, listOf(0), listOf(0))

            var expectedMatrixIndices = arrayOf(
                1, 2, 3, 4, 5, 6, 7
            )
            expectedMatrixIndices += expectedMatrixIndices
            for ((index, expectedMatrixIndex) in expectedMatrixIndices.withIndex()) {
                assertEquals(expectedMatrixIndex, vertexBuffer[index].matrixIndex)
            }

            val indexBuffer = stack.callocInt(18)
            combinedGenerator.fillIndexBuffer(indexBuffer)
            val expectedIndices = arrayOf(
                0, 1, 2, 2, 3, 0,
                5, 6, 4,
                7, 8, 9, 9, 10, 7,
                12, 13, 11
            )
            for (expectedIndex in expectedIndices) {
                assertEquals(expectedIndex, indexBuffer.get())
            }
        }
    }
}
