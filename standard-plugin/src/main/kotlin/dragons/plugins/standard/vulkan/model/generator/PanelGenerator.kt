package dragons.plugins.standard.vulkan.model.generator

import dragons.plugins.standard.vulkan.vertex.BasicVertex.Companion.MATERIAL_TERRAIN

fun generatePanelModel(textureIndex: Int, heightTextureIndex: Int) = ModelGenerator(
    numVertices = 4, numIndices = 6,
    fillVertexBuffer = { vertexBuffer ->
        for (vertex in vertexBuffer) {
            // The position differs per vertex, but the z-coordinate is always 0
            vertex.position.z = 0f
            vertex.normal.x = 0f
            vertex.normal.y = 0f
            vertex.normal.z = 1f
            // The color texture coordinates differ per vertex
            vertex.heightTextureCoordinates.x = 0f
            vertex.heightTextureCoordinates.y = 0f
            vertex.matrixIndex = 0
            vertex.materialIndex = MATERIAL_TERRAIN // TODO Create a dedicated material for UI
            vertex.deltaFactor.x = 1f
            vertex.deltaFactor.y = 1f
            vertex.colorTextureIndex = textureIndex
            vertex.heightTextureIndex = heightTextureIndex
        }

        vertexBuffer[0].position.x = -0.5f
        vertexBuffer[0].position.y = -0.5f
        vertexBuffer[0].colorTextureCoordinates.x = 0f
        vertexBuffer[0].colorTextureCoordinates.y = 0f

        vertexBuffer[1].position.x = 0.5f
        vertexBuffer[1].position.y = -0.5f
        vertexBuffer[1].colorTextureCoordinates.x = 1f
        vertexBuffer[1].colorTextureCoordinates.y = 0f

        vertexBuffer[2].position.x = 0.5f
        vertexBuffer[2].position.y = 0.5f
        vertexBuffer[2].colorTextureCoordinates.x = 1f
        vertexBuffer[2].colorTextureCoordinates.y = 1f

        vertexBuffer[3].position.x = -0.5f
        vertexBuffer[3].position.y = 0.5f
        vertexBuffer[3].colorTextureCoordinates.x = 0f
        vertexBuffer[3].colorTextureCoordinates.y = 1f
    },
    fillIndexBuffer = { indexBuffer ->
        indexBuffer.put(0, 0)
        indexBuffer.put(1, 1)
        indexBuffer.put(2, 2)
        indexBuffer.put(3, 2)
        indexBuffer.put(4, 3)
        indexBuffer.put(5, 0)
    }
)
