package dragons.plugins.standard.vulkan.model.generator

import dragons.plugins.standard.vulkan.vertex.BasicVertex
import dragons.util.Angle
import dragons.util.Distance
import org.joml.Math.PI

fun generateFlowerBushModel(flowerProperties: List<FlowerModelProperties>): ModelGenerator {
    val generators = mutableListOf<ModelGenerator>()
    for ((index, props) in flowerProperties.withIndex()) {
        createSingleFlower(index, props, generators)
    }
    return ModelGenerator.combine(generators)
}

private fun createSingleFlower(baseMatrixIndex: Int, props: FlowerModelProperties, generators: MutableCollection<ModelGenerator>) {
    generators.add(createFlowerStem(baseMatrixIndex, props))
    generators.add(props.topShape(baseMatrixIndex, props))
}

private fun createFlowerStem(matrixIndex: Int, props: FlowerModelProperties): ModelGenerator {
    return ModelGenerator(
        numVertices = props.numHorizontalStemParts * props.numVerticalStemParts,
        numIndices = 6 * (props.numHorizontalStemParts - 1) * (props.numVerticalStemParts - 1),
        fillVertexBuffer = { vertexBuffer ->
            val deltaFactorX = props.stemRadius.meters * PI.toFloat()
            val deltaFactorY = props.stemLength.meters

            for (horizontalIndex in 0 until props.numHorizontalStemParts) {
                val horizontalProgress = horizontalIndex.toFloat() / (props.numHorizontalStemParts - 1).toFloat()
                val horizontalAngle = Angle.degrees(360f * horizontalProgress)
                val normalX = horizontalAngle.sin
                val normalZ = horizontalAngle.cos
                val currentX = props.stemRadius * normalX
                val currentZ = props.stemRadius * normalZ

                for (verticalIndex in 0 until props.numVerticalStemParts) {
                    val verticalProgress = verticalIndex.toFloat() / (props.numVerticalStemParts - 1).toFloat()
                    val currentHeight = props.stemLength * verticalProgress
                    val currentVertex = vertexBuffer[horizontalIndex + props.numHorizontalStemParts * verticalIndex]

                    currentVertex.position.x = currentX.meters
                    currentVertex.position.y = currentHeight.meters
                    currentVertex.position.z = currentZ.meters

                    currentVertex.normal.x = normalX
                    currentVertex.normal.y = 0f
                    currentVertex.normal.z = normalZ

                    currentVertex.matrixIndex = matrixIndex
                    currentVertex.materialIndex = BasicVertex.MATERIAL_TERRAIN
                    currentVertex.deltaFactor.x = deltaFactorX
                    currentVertex.deltaFactor.y = deltaFactorY

                    currentVertex.colorTextureCoordinates.x = horizontalProgress
                    currentVertex.colorTextureCoordinates.y = verticalProgress
                    currentVertex.colorTextureIndex = props.stemColorTextureIndex

                    currentVertex.heightTextureCoordinates.x = horizontalProgress
                    currentVertex.heightTextureCoordinates.y = verticalProgress
                    currentVertex.heightTextureIndex = props.stemHeightTextureIndex
                }
            }
        },
        fillIndexBuffer = { indexBuffer ->
            for (horizontalIndex2 in 1 until props.numHorizontalStemParts) {
                val horizontalIndex1 = horizontalIndex2 - 1

                for (verticalIndex2 in 1 until props.numVerticalStemParts) {
                    val verticalIndex1 = verticalIndex2 - 1

                    val index11 = horizontalIndex1 + verticalIndex1 * props.numHorizontalStemParts
                    val index12 = horizontalIndex1 + verticalIndex2 * props.numHorizontalStemParts
                    val index21 = horizontalIndex2 + verticalIndex1 * props.numHorizontalStemParts
                    val index22 = horizontalIndex2 + verticalIndex2 * props.numHorizontalStemParts

                    indexBuffer.put(index11)
                    indexBuffer.put(index21)
                    indexBuffer.put(index22)

                    indexBuffer.put(index22)
                    indexBuffer.put(index12)
                    indexBuffer.put(index11)
                }
            }
        }
    )
}

class FlowerModelProperties(
    val stemLength: Distance, val stemRadius: Distance, val numHorizontalStemParts: Int, val numVerticalStemParts: Int,
    val topShape: FlowerTopShape, val stemColorTextureIndex: Int, val stemHeightTextureIndex: Int,
    val topColorTextureIndex: Int, val topHeightTextureIndex: Int
)

typealias FlowerTopShape = (matrixIndex: Int, props: FlowerModelProperties) -> ModelGenerator

fun circleTopShape(
    numRingVertices: Int,
    radius: Distance
) = { matrixIndex: Int, props: FlowerModelProperties ->
    ModelGenerator(
        numVertices = 1 + numRingVertices,
        numIndices = 3 * numRingVertices,
        fillVertexBuffer = { vertexBuffer ->

            // Some values are shared between all vertices
            for (vertex in vertexBuffer) {
                vertex.position.y = props.stemLength.meters

                vertex.normal.x = 0f
                vertex.normal.y = 1f
                vertex.normal.z = 0f

                vertex.matrixIndex = matrixIndex
                vertex.materialIndex = BasicVertex.MATERIAL_TERRAIN

                vertex.deltaFactor.x = 2f * radius.meters
                vertex.deltaFactor.y = 2f * radius.meters

                vertex.colorTextureIndex = props.topColorTextureIndex
                vertex.heightTextureIndex = props.topHeightTextureIndex
            }

            // The first vertex is special
            vertexBuffer[0].run {
                position.x = 0f
                position.z = 0f

                colorTextureCoordinates.x = 0.5f
                colorTextureCoordinates.y = 0.5f

                heightTextureCoordinates.x = 0.5f
                heightTextureCoordinates.y = 0.5f
            }

            for (ringIndex in 0 until numRingVertices) {
                val angle = Angle.degrees(360f * ringIndex.toFloat() / numRingVertices.toFloat())
                val textureX = 0.5f + angle.sin * 0.5f
                val textureZ = 0.5f + angle.cos * 0.5f

                vertexBuffer[1 + ringIndex].run {
                    position.x = angle.sin * radius.meters
                    position.z = angle.cos * radius.meters

                    colorTextureCoordinates.x = textureX
                    colorTextureCoordinates.y = textureZ

                    heightTextureCoordinates.x = textureX
                    heightTextureCoordinates.y = textureZ
                }
            }
        }, fillIndexBuffer = { indexBuffer ->
            // Note: the center vertex occupies the first element in the vertex buffer
            for (ringIndex1 in 1 until numRingVertices) {
                val ringIndex2 = ringIndex1 + 1

                indexBuffer.put(0)
                indexBuffer.put(ringIndex2)
                indexBuffer.put(ringIndex1)
            }
            indexBuffer.put(0)
            indexBuffer.put(1)
            indexBuffer.put(numRingVertices)
        }
    )
}

object FlowerGenerators {
    fun modelProps1(
        stemColorTextureIndex: Int, stemHeightTextureIndex: Int,
        topColorTextureIndex: Int, topHeightTextureIndex: Int
    ) = FlowerModelProperties(
        stemLength = Distance.meters(0.5f), stemRadius = Distance.milliMeters(30),
        numHorizontalStemParts = 6, numVerticalStemParts = 4,
        topShape = circleTopShape(15, Distance.Companion.milliMeters(100)),
        stemColorTextureIndex = stemColorTextureIndex, stemHeightTextureIndex = stemHeightTextureIndex,
        topColorTextureIndex = topColorTextureIndex, topHeightTextureIndex = topHeightTextureIndex
    )

    val BUSH_SIZE1 = 8
}
