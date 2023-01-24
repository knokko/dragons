package dragons.plugins.standard.vulkan.model.generator

import dragons.plugins.standard.vulkan.vertex.BasicVertex.Companion.MATERIAL_TERRAIN
import dragons.geometry.Angle
import dragons.geometry.Distance
import org.joml.Math.*
import org.joml.Vector3f

fun generateFlowerBushModel(flowerProperties: List<FlowerModelProperties>): ModelGenerator {
    val generators = mutableListOf<ModelGenerator>()
    for ((index, props) in flowerProperties.withIndex()) {
        createSingleFlower(index, props, generators)
    }
    return ModelGenerator.combineWithSharedTextures(generators)
}

private fun createSingleFlower(baseMatrixIndex: Int, props: FlowerModelProperties, generators: MutableCollection<ModelGenerator>) {
    generators.add(createFlowerStem(baseMatrixIndex, props))
    generators.add(props.topShape(baseMatrixIndex, props))
}

private fun createFlowerStem(matrixIndex: Int, props: FlowerModelProperties): ModelGenerator {
    return ModelGenerator(
        numVertices = props.numHorizontalStemParts * props.numVerticalStemParts,
        numIndices = 6 * (props.numHorizontalStemParts - 1) * (props.numVerticalStemParts - 1),
        fillVertexBuffer = { vertexBuffer, colorImageIndices, heightImageIndices ->

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

                    currentVertex.position.x = currentX.meters.toFloat()
                    currentVertex.position.y = currentHeight.meters.toFloat()
                    currentVertex.position.z = currentZ.meters.toFloat()

                    currentVertex.normal.x = normalX
                    currentVertex.normal.y = 0f
                    currentVertex.normal.z = normalZ

                    currentVertex.matrixIndex = matrixIndex
                    currentVertex.materialIndex = MATERIAL_TERRAIN
                    currentVertex.deltaFactor.x = deltaFactorX.toFloat()
                    currentVertex.deltaFactor.y = deltaFactorY.toFloat()

                    currentVertex.colorTextureCoordinates.x = horizontalProgress
                    currentVertex.colorTextureCoordinates.y = verticalProgress
                    currentVertex.colorTextureIndex = colorImageIndices[0]

                    currentVertex.heightTextureCoordinates.x = horizontalProgress
                    currentVertex.heightTextureCoordinates.y = verticalProgress
                    currentVertex.heightTextureIndex = heightImageIndices[0]
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
    val stemLength: Distance, val stemRadius: Distance,
    val numHorizontalStemParts: Int, val numVerticalStemParts: Int,
    val topShape: FlowerTopShape
)

typealias FlowerTopShape = (matrixIndex: Int, props: FlowerModelProperties) -> ModelGenerator

class LeafRing(val angle: Angle, val numLeafs: Int)

fun leafRingTopShape(
    innerRadius: Distance,
    numInnerRingVertices: Int,
    rings: Collection<LeafRing>,
    leafLength: Distance,
    numVerticesPerLeafHalf: Int,
    leafWidthFunction: (Float) -> Distance,
    leafDepthFunction: (Float) -> Distance
) = { matrixIndex: Int, props: FlowerModelProperties ->

    val totalNumLeafs = rings.sumOf { it.numLeafs }

    // The number of vertices on 1 side (top or bottom) of the leaf.
    val numVerticesPerLeafSide = 2 * numVerticesPerLeafHalf

    // The number of vertices per leaf is simply twice the number of vertices per leaf side
    val numVerticesPerLeaf = 2 * numVerticesPerLeafSide

    // 1 quad between each consecutive inner vertex
    val numIndicesPerLeafSide = 6 * (numVerticesPerLeafHalf - 1)

    // The number of indices to 'glue' the top side to the bottom side. It needs 1 quad for each consecutive inner vertex.
    val numSideGlueIndices = 6 * (numVerticesPerLeafSide - 1)

    // We also need 1 quad to connect the bottom end to the top end
    val numEndGlueIndices = 6

    val numIndicesPerLeaf = 2 * numIndicesPerLeafSide + 2 * numSideGlueIndices + numEndGlueIndices

    ModelGenerator(
        numVertices = 2 * (1 + numInnerRingVertices) + totalNumLeafs * numVerticesPerLeaf,
        numIndices = (3 + 3 + 6) * numInnerRingVertices + totalNumLeafs * numIndicesPerLeaf,
        fillVertexBuffer = { vertexBuffer, colorImageIndices, heightImageIndices ->

            if (colorImageIndices.size != 2 || heightImageIndices.size != 2) {
                throw IllegalArgumentException("There must be exactly 2 color image indices and 2 height image indices")
            }

            val innerDepth = leafDepthFunction(0f)
            for ((indexOffset, scaleY) in arrayOf(Pair(0, 1f), Pair(1 + numInnerRingVertices, -1f))) {

                // The center vertex
                vertexBuffer[indexOffset].run {
                    position.x = 0f
                    position.y = (props.stemLength + innerDepth * scaleY).meters.toFloat()
                    position.z = 0f

                    normal.x = 0f
                    normal.y = scaleY
                    normal.z = 0f

                    colorTextureCoordinates.x = 0.5f
                    colorTextureCoordinates.y = 0.5f
                }

                // The inner ring vertices
                for (rawIndex in 0 until numInnerRingVertices) {
                    val angle = Angle.degrees(360f * rawIndex.toFloat() / numInnerRingVertices.toFloat())
                    vertexBuffer[indexOffset + rawIndex + 1].run {
                        position.x = angle.sin * innerRadius.meters.toFloat()
                        position.y = (props.stemLength + innerDepth * scaleY).meters.toFloat()
                        position.z = angle.cos * innerRadius.meters.toFloat()

                        normal.x = 0f
                        normal.y = scaleY
                        normal.z = 0f

                        colorTextureCoordinates.x = 0.5f + angle.sin * 0.25f
                        colorTextureCoordinates.y = 0.5f + angle.cos * 0.25f
                    }
                }
            }

            // The leaf vertices
            var firstRingVertexIndex = 2 * (1 + numInnerRingVertices)

            for (ring in rings) {

                val verticalFactor = ring.angle.sin
                val horizontalFactor = ring.angle.cos

                for (leafIndex in 0 until ring.numLeafs) {
                    val firstLeafVertexIndex = firstRingVertexIndex + leafIndex * numVerticesPerLeaf

                    val horizontalAngle = Angle.degrees(360f * leafIndex.toFloat() / ring.numLeafs.toFloat())

                    val baseX = horizontalAngle.sin * innerRadius.meters.toFloat()
                    val baseY = props.stemLength.meters.toFloat()
                    val baseZ = horizontalAngle.cos * innerRadius.meters.toFloat()
                    val base = Vector3f(baseX, baseY, baseZ)

                    val flatLengthDirectionX = horizontalAngle.sin
                    val flatLengthDirectionZ = horizontalAngle.cos

                    val flatWidthDirectionX = flatLengthDirectionZ
                    val flatWidthDirectionZ = -flatLengthDirectionX

                    val lengthDirectionX = horizontalFactor * flatLengthDirectionX
                    val lengthDirectionY = verticalFactor
                    val lengthDirectionZ = horizontalFactor * flatLengthDirectionZ
                    val lengthDirection = Vector3f(lengthDirectionX, lengthDirectionY, lengthDirectionZ)

                    val widthDirectionX = flatWidthDirectionX
                    val widthDirectionY = 0f
                    val widthDirectionZ = flatWidthDirectionZ
                    val widthDirection = Vector3f(widthDirectionX, widthDirectionY, widthDirectionZ)

                    val depthDirectionX = -verticalFactor * flatLengthDirectionX
                    val depthDirectionY = horizontalFactor
                    val depthDirectionZ = -verticalFactor * flatLengthDirectionZ
                    val depthDirection = Vector3f(depthDirectionX, depthDirectionY, depthDirectionZ)

                    fun addVertex(internalIndex: Int, lengthFactor: Float, positiveWidth: Boolean, positiveDepth: Boolean) {
                        val positiveWidthFactor = if (positiveWidth) 1f else -1f
                        val halfWidth = leafWidthFunction(lengthFactor).meters.toFloat() * positiveWidthFactor

                        val positiveDepthFactor = if (positiveDepth) 1f else -1f
                        val halfDepth = leafDepthFunction(lengthFactor).meters.toFloat() * positiveDepthFactor

                        val length = lengthFactor * leafLength.meters.toFloat()

                        vertexBuffer[firstLeafVertexIndex + internalIndex].run {
                            val positionVector = Vector3f(base)
                            positionVector.add(lengthDirection.mul(length, Vector3f()))
                            positionVector.add(widthDirection.mul(halfWidth, Vector3f()))
                            positionVector.add(depthDirection.mul(halfDepth, Vector3f()))

                            position.x = positionVector.x
                            position.y = positionVector.y
                            position.z = positionVector.z

                            normal.x = depthDirection.x * positiveDepthFactor
                            normal.y = depthDirection.y * positiveDepthFactor
                            normal.z = depthDirection.z * positiveDepthFactor

                            colorTextureCoordinates.x = 0.5f + horizontalAngle.sin * (0.25f + lengthFactor * 0.25f)
                            colorTextureCoordinates.y = 0.5f + horizontalAngle.cos * (0.25f + lengthFactor * 0.25f)
                        }
                    }

                    for ((sideIndex, positiveDepth) in arrayOf(true, false).withIndex()) {
                        val internalIndexOffset = sideIndex * numVerticesPerLeafSide

                        // Left vertices
                        for (rawIndex in 0 until numVerticesPerLeafHalf) {
                            addVertex(
                                internalIndexOffset + rawIndex,
                                rawIndex.toFloat() / (numVerticesPerLeafHalf - 1).toFloat(),
                                positiveWidth = false, positiveDepth
                            )
                        }

                        // Right vertices
                        for (rawIndex in 0 until numVerticesPerLeafHalf) {
                            addVertex(
                                internalIndexOffset + numVerticesPerLeafHalf + rawIndex,
                                rawIndex.toFloat() / (numVerticesPerLeafHalf - 1).toFloat(),
                                positiveWidth = true, positiveDepth
                            )
                        }
                    }
                }

                firstRingVertexIndex += ring.numLeafs * numVerticesPerLeaf
            }

            // Some properties are shared between all vertices
            val deltaFactor = (innerRadius + leafLength).meters

            for (vertex in vertexBuffer) {
                vertex.matrixIndex = matrixIndex
                vertex.materialIndex = MATERIAL_TERRAIN
                vertex.deltaFactor.x = deltaFactor.toFloat()
                vertex.deltaFactor.y = deltaFactor.toFloat()
                vertex.heightTextureCoordinates.x = vertex.colorTextureCoordinates.x
                vertex.heightTextureCoordinates.y = vertex.colorTextureCoordinates.y
                vertex.colorTextureIndex = colorImageIndices[1]
                vertex.heightTextureIndex = heightImageIndices[1]
            }
        }, fillIndexBuffer = { indexBuffer ->

            // The top center
            var indexCenter = 0
            for (innerIndex in 1 until numInnerRingVertices) {
                indexBuffer.put(indexCenter)
                indexBuffer.put(indexCenter + innerIndex + 1)
                indexBuffer.put(indexCenter + innerIndex)
            }
            indexBuffer.put(indexCenter)
            indexBuffer.put(indexCenter + 1)
            indexBuffer.put(indexCenter + numInnerRingVertices)

            // The bottom center
            indexCenter = 1 + numInnerRingVertices
            for (innerIndex in 1 until numInnerRingVertices) {
                indexBuffer.put(indexCenter)
                indexBuffer.put(indexCenter + innerIndex)
                indexBuffer.put(indexCenter + innerIndex + 1)
            }
            indexBuffer.put(indexCenter)
            indexBuffer.put(indexCenter + numInnerRingVertices)
            indexBuffer.put(indexCenter + 1)

            // The center side
            for (innerIndex in 1 until numInnerRingVertices) {
                val indexTopRight = innerIndex
                val indexTopLeft = innerIndex + 1
                val indexBottomRight = indexTopRight + 1 + numInnerRingVertices
                val indexBottomLeft = indexBottomRight + 1

                indexBuffer.put(indexBottomLeft)
                indexBuffer.put(indexBottomRight)
                indexBuffer.put(indexTopRight)

                indexBuffer.put(indexTopRight)
                indexBuffer.put(indexTopLeft)
                indexBuffer.put(indexBottomLeft)
            }

            // The leaves...
            var firstRingVertexIndex = 2 * (1 + numInnerRingVertices)
            for (ring in rings) {

                for (leafCounter in 0 until ring.numLeafs) {

                    // Connect the subsequent top vertices
                    for (rawLeafIndex in 0 until numVerticesPerLeafHalf - 1) {
                        val topLeftBack = firstRingVertexIndex + numVerticesPerLeafHalf + rawLeafIndex
                        val topRightBack = firstRingVertexIndex + rawLeafIndex
                        val topLeftFront = topLeftBack + 1
                        val topRightFront = topRightBack + 1

                        indexBuffer.put(topRightBack)
                        indexBuffer.put(topRightFront)
                        indexBuffer.put(topLeftFront)

                        indexBuffer.put(topLeftFront)
                        indexBuffer.put(topLeftBack)
                        indexBuffer.put(topRightBack)
                    }

                    // Connect the bottom side to the top side
                    for (rawLeafIndex in 0 until numVerticesPerLeafHalf - 1) {
                        val topBack = firstRingVertexIndex + rawLeafIndex
                        val topFront = topBack + 1
                        val bottomBack = topBack + numVerticesPerLeafSide
                        val bottomFront = bottomBack + 1

                        indexBuffer.put(bottomBack)
                        indexBuffer.put(bottomFront)
                        indexBuffer.put(topFront)

                        indexBuffer.put(topFront)
                        indexBuffer.put(topBack)
                        indexBuffer.put(bottomBack)
                    }
                    for (rawLeafIndex in 0 until numVerticesPerLeafHalf - 1) {
                        val topBack = numVerticesPerLeafHalf + firstRingVertexIndex + rawLeafIndex
                        val topFront = topBack + 1
                        val bottomBack = topBack + numVerticesPerLeafSide
                        val bottomFront = bottomBack + 1

                        indexBuffer.put(bottomFront)
                        indexBuffer.put(bottomBack)
                        indexBuffer.put(topBack)

                        indexBuffer.put(topBack)
                        indexBuffer.put(topFront)
                        indexBuffer.put(bottomFront)
                    }

                    // Connect the top end vertices to the bottom end vertices
                    val indexTopRight = firstRingVertexIndex + numVerticesPerLeafHalf - 1
                    val indexTopLeft = indexTopRight + numVerticesPerLeafHalf
                    val indexBottomRight = indexTopRight + numVerticesPerLeafSide
                    val indexBottomLeft = indexBottomRight + numVerticesPerLeafHalf

                    indexBuffer.put(indexBottomRight)
                    indexBuffer.put(indexBottomLeft)
                    indexBuffer.put(indexTopLeft)

                    indexBuffer.put(indexTopLeft)
                    indexBuffer.put(indexTopRight)
                    indexBuffer.put(indexBottomRight)

                    firstRingVertexIndex += numVerticesPerLeafSide

                    // Connect the subsequent bottom vertices
                    for (rawLeafIndex in 0 until numVerticesPerLeafHalf - 1) {
                        val bottomLeftBack = firstRingVertexIndex + numVerticesPerLeafHalf + rawLeafIndex
                        val bottomRightBack = firstRingVertexIndex + rawLeafIndex
                        val bottomLeftFront = bottomLeftBack + 1
                        val bottomRightFront = bottomRightBack + 1

                        indexBuffer.put(bottomLeftBack)
                        indexBuffer.put(bottomLeftFront)
                        indexBuffer.put(bottomRightFront)

                        indexBuffer.put(bottomRightFront)
                        indexBuffer.put(bottomRightBack)
                        indexBuffer.put(bottomLeftBack)
                    }

                    firstRingVertexIndex += numVerticesPerLeafSide
                }
            }
        }
    )
}

fun circleTopShape(
    numRingVertices: Int,
    radius: Distance
) = { matrixIndex: Int, props: FlowerModelProperties ->
    ModelGenerator(
        numVertices = 1 + numRingVertices,
        numIndices = 3 * numRingVertices,
        fillVertexBuffer = { vertexBuffer, colorImageIndices, heightImageIndices ->

            if (colorImageIndices.size != 2 || heightImageIndices.size != 2) {
                throw IllegalArgumentException("There must be exactly 2 color image indices and 2 height image indices")
            }

            // Some values are shared between all vertices
            for (vertex in vertexBuffer) {
                vertex.position.y = props.stemLength.meters.toFloat()

                vertex.normal.x = 0f
                vertex.normal.y = 1f
                vertex.normal.z = 0f

                vertex.matrixIndex = matrixIndex
                vertex.materialIndex = MATERIAL_TERRAIN

                vertex.deltaFactor.x = 2f * radius.meters.toFloat()
                vertex.deltaFactor.y = 2f * radius.meters.toFloat()

                vertex.colorTextureIndex = colorImageIndices[1]
                vertex.heightTextureIndex = heightImageIndices[1]
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
                    position.x = angle.sin * radius.meters.toFloat()
                    position.z = angle.cos * radius.meters.toFloat()

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
    val MODEL_PROPS1 = FlowerModelProperties(
        stemLength = Distance.meters(0.5f), stemRadius = Distance.milliMeters(30),
        numHorizontalStemParts = 6, numVerticalStemParts = 4,
        topShape = circleTopShape(15, Distance.Companion.milliMeters(100))
    )

    val MODEL_PROPS2 = FlowerModelProperties(
        stemLength = Distance.meters(0.3f), stemRadius = Distance.milliMeters(20),
        numHorizontalStemParts = 6, numVerticalStemParts = 4,
        topShape = leafRingTopShape(
            innerRadius = Distance.milliMeters(40),
            numInnerRingVertices = 15,
            rings = listOf(
                LeafRing(angle = Angle.degrees(30f), numLeafs = 15),
                LeafRing(angle = Angle.degrees(-20f), numLeafs = 10)
            ),
            leafLength = Distance.milliMeters(50),
            numVerticesPerLeafHalf = 6,
            leafWidthFunction = { lengthFactor -> Distance.milliMeters(6f + 10f * sin(lengthFactor * PI.toFloat())) },
            leafDepthFunction = { Distance.milliMeters(1) }
        )
    )

    const val BUSH_SIZE1 = 8
}
