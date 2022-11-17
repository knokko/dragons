package dragons.plugins.standard.vulkan.model.generator.dragon

import dragons.plugins.standard.vulkan.model.generator.ModelGenerator
import dragons.plugins.standard.vulkan.model.generator.putQuad
import dragons.plugins.standard.vulkan.model.generator.putTriangle
import dragons.plugins.standard.vulkan.vertex.BasicVertex
import dragons.space.Distance
import org.joml.Math.sqrt
import kotlin.math.absoluteValue

/*
 * Note: without looking at the corresponding sketches, the variable names used in this file won't make any sense.
 * You can find the sketches in project-folder/docs/model-sketches/dragon.
 */

fun createDragonWingGenerator(
    props: DragonWingProperties,
    matrixIndices: List<Int>,
    isRightWing: Boolean
): ModelGenerator {
    val leftGenerator = ModelGenerator.combineWithSharedTextures(listOf(
        generateInnerWing(props, matrixIndices),
        generateOuterWing(props, matrixIndices),
        generateWingSides(props, matrixIndices),
        generateNails(props, matrixIndices)
    ))
    if (isRightWing) {
        return ModelGenerator(
            numVertices = leftGenerator.numVertices,
            numIndices = leftGenerator.numIndices,
            fillVertexBuffer = { vertices, colorTextureIndices, heightTextureIndices ->
                leftGenerator.fillVertexBuffer(vertices, colorTextureIndices, heightTextureIndices)
                for (vertex in vertices) {
                    vertex.position.x *= -1f
                    vertex.normal.x *= -1f
                }
            },
            // TODO Perhaps invert winding order
            fillIndexBuffer = leftGenerator.fillIndexBuffer
        )
    } else {
        return leftGenerator
    }
}

private fun generateNails(props: DragonWingProperties, matrixIndices: List<Int>) = ModelGenerator.combineWithSharedTextures(listOf(
    generateNail(props, matrixIndices, 1),
    generateNail(props, matrixIndices, 0),
    generateNail(props, matrixIndices, -1)
))

private fun generateNail(props: DragonWingProperties, matrixIndices: List<Int>, relativeIndexY: Int): ModelGenerator {
    val indexBaseFrontDown = 0
    val indexBaseFrontUp = 1
    val indexBaseBackUp = 2
    val indexBaseBackDown = 3

    // TODO Use a more complicated shape
    val indexEndFront = 4
    val indexEndBack = 5

    return ModelGenerator(
        numVertices = 6,
        numIndices = 4 * 3,
        fillVertexBuffer = { vertices, colorTextureIndices, heightTextureIndices ->

            // First set the shared properties
            for (vertex in vertices) {
                vertex.run {
                    matrixIndex = matrixIndices[0]
                    // TODO Create separate nail material
                    materialIndex = BasicVertex.MATERIAL_METAL
                    deltaFactor.x = 1f
                    deltaFactor.y = 1f
                    colorTextureIndex = colorTextureIndices[DragonTextures.Wing.Nail.colorTexture]
                    heightTextureIndex = heightTextureIndices[DragonTextures.Wing.Nail.heightTexture]
                }
            }

            // The base vertices share the same X-position, X-normal and U-coordinate
            for (vertexIndex in arrayOf(indexBaseFrontDown, indexBaseFrontUp, indexBaseBackUp, indexBaseBackDown)) {
                vertices[vertexIndex].position.x = 0f
                vertices[vertexIndex].normal.x = 0f
                vertices[vertexIndex].colorTextureCoordinates.x = DragonTextures.Wing.Nail.minU
                vertices[vertexIndex].heightTextureCoordinates.x = DragonTextures.Wing.Nail.minU
            }

            // The end vertices also have the same X-position and U-coordinate
            for (vertexIndex in arrayOf(indexEndBack, indexEndFront)) {
                vertices[vertexIndex].position.x = props.nailLength.meters
                vertices[vertexIndex].colorTextureCoordinates.x = DragonTextures.Wing.Nail.maxU
                vertices[vertexIndex].heightTextureCoordinates.x = DragonTextures.Wing.Nail.maxU
            }

            // The down vertices share the same Y-position and V-coordinate
            for (vertexIndex in arrayOf(indexBaseFrontDown, indexBaseBackDown)) {
                vertices[vertexIndex].position.y = (props.nailWidth * (relativeIndexY - 0.5f)).meters
                vertices[vertexIndex].colorTextureCoordinates.y = DragonTextures.Wing.Nail.minV
                vertices[vertexIndex].heightTextureCoordinates.y = DragonTextures.Wing.Nail.minV
            }

            // The up vertices also share the same Y-position and V-coordinate
            for (vertexIndex in arrayOf(indexBaseFrontUp, indexBaseBackUp)) {
                vertices[vertexIndex].position.y = (props.nailWidth * (relativeIndexY + 0.5f)).meters
                vertices[vertexIndex].colorTextureCoordinates.y = DragonTextures.Wing.Nail.maxV
                vertices[vertexIndex].heightTextureCoordinates.y = DragonTextures.Wing.Nail.maxV
            }

            // The front vertices share Z-position and normal
            for (vertexIndex in arrayOf(indexBaseFrontDown, indexBaseFrontUp, indexEndFront)) {
                vertices[vertexIndex].run {
                    position.z = props.wingDepth.meters * 0.5f
                    normal.x = 0f
                    normal.y = 0f
                    normal.z = 1f
                }
            }

            val halfSq2 = 0.5f * sqrt(2f)

            // The back vertices are more complicated...
            vertices[indexBaseBackDown].run {
                position.z = -props.wingDepth.meters * 0.5f
                normal.y = -halfSq2
                normal.z = -halfSq2
                // TODO Finish this
            }

        },
        fillIndexBuffer = { indices ->
            indices.putTriangle(indexBaseFrontDown, indexEndFront, indexBaseFrontUp)
            indices.putTriangle(indexBaseFrontUp, indexEndBack, indexBaseBackUp)
            indices.putTriangle(indexBaseBackUp, indexEndBack, indexBaseBackDown)
            indices.putTriangle(indexBaseBackDown, indexEndBack, indexBaseFrontDown)
        }
    )
}

private fun generateWingSides(props: DragonWingProperties, matrixIndices: List<Int>): ModelGenerator {

    val indexInnerA4 = 0
    val indexInnerA0 = 1
    val indexOuterA0 = 2
    val indexOuterA4 = 3

    val indexInnerD4 = 4
    val indexInnerD1 = 5
    val indexOuterD1 = 6
    val indexOuterD4 = 7

    val indexInnerC2 = 8
    val indexInnerC0 = 9
    val indexOuterC0 = 10
    val indexOuterC2 = 11

    val indexInnerB1 = 12
    val indexInnerB0 = 13
    val indexOuterB0 = 14
    val indexOuterB1 = 15

    // TODO Add the sloped wing surfaces as well

    return ModelGenerator(
        numVertices = 4 * 4,
        numIndices = 4 * 6,
        fillVertexBuffer = { vertices, colorTextureIndices, heightTextureIndices ->

            val texture = DragonTextures.Wing.Side

            // To avoid code duplication, set all common vertex attributes first
            for (vertex in vertices) {
                // The z-coordinates will be controlled by the transformation matrix
                vertex.position.z = 0f
                vertex.heightTextureCoordinates.x = 0f
                vertex.heightTextureCoordinates.y = 0f
                // TODO Perhaps create a separate material for skin
                vertex.materialIndex = BasicVertex.MATERIAL_TERRAIN
                // TODO Perhaps use height textures on dragon models
                vertex.deltaFactor.x = 1f
                vertex.deltaFactor.y = 1f
                vertex.colorTextureIndex = colorTextureIndices[texture.colorTexture]
                vertex.heightTextureIndex = heightTextureIndices[texture.heightTexture]
            }

            // Set position.x
            for (index in arrayOf(
                indexInnerA0, indexInnerA4, indexInnerB0, indexInnerB1, indexInnerC0, indexInnerC2, indexInnerD1, indexInnerD4)
            ) {
                vertices[index].position.x = (props.wingDepth * 0.5f).meters
            }
            for (index in arrayOf(
                indexOuterA0, indexOuterA4, indexOuterB0, indexOuterB1, indexOuterC0, indexOuterC2, indexOuterD1, indexOuterD4
            )) {
                vertices[index].position.x = -(props.wingDepth * 0.5f).meters
            }

            // Set position.y and normal.xyz
            for (index in arrayOf(indexInnerA0, indexOuterA0, indexInnerA4, indexOuterA4)) {
                vertices[index].position.y = (props.nailWidth * 1.5f).meters
                vertices[index].normal.set(0f, 1f, 0f)
            }
            for (index in arrayOf(indexInnerB0, indexOuterB0, indexInnerB1, indexOuterB1)) {
                vertices[index].position.y = -(props.nailWidth * 1.5f).meters
                vertices[index].normal.set(0f, -1f, 0f)
            }
            for (index in arrayOf(indexInnerC0, indexOuterC0, indexInnerC2, indexOuterC2)) {
                vertices[index].position.y = (-props.nailWidth * 1.5f - props.wingLaneWidth).meters
                vertices[index].normal.set(0f, -1f, 0f)
            }
            for (index in arrayOf(indexInnerD1, indexOuterD1, indexInnerD4, indexOuterD4)) {
                vertices[index].position.y = (-props.nailWidth * 1.5f - props.wingLaneWidth * 2).meters
                vertices[index].normal.set(0f, -1f, 0f)
            }

            for (index in arrayOf(indexInnerA0, indexInnerB0, indexInnerC0, indexOuterA0, indexOuterB0, indexOuterC0)) {
                vertices[index].matrixIndex = matrixIndices[0]
            }
            for (index in arrayOf(indexInnerB1, indexInnerD1, indexOuterB1, indexOuterD1)) {
                vertices[index].matrixIndex = matrixIndices[1]
            }
            vertices[indexInnerC2].matrixIndex = matrixIndices[2]
            vertices[indexOuterC2].matrixIndex = matrixIndices[2]
            for (index in arrayOf(indexInnerA4, indexInnerD4, indexOuterA4, indexOuterD4)) {
                vertices[index].matrixIndex = 4
            }

            // For now, each side part will have the same texture coordinates
            for (index in arrayOf(indexInnerA4, indexOuterD4, indexOuterC2, indexOuterB1)) {
                vertices[index].colorTextureCoordinates.x = texture.minU
                vertices[index].colorTextureCoordinates.y = texture.minV
            }
            for (index in arrayOf(indexOuterA4, indexInnerD4, indexInnerC2, indexInnerB1)) {
                vertices[index].colorTextureCoordinates.x = texture.minU
                vertices[index].colorTextureCoordinates.y = texture.maxV
            }
            for (index in arrayOf(indexInnerA0, indexOuterD1, indexOuterC0, indexOuterB0)) {
                vertices[index].colorTextureCoordinates.x = texture.maxU
                vertices[index].colorTextureCoordinates.y = texture.minV
            }
            for (index in arrayOf(indexOuterA0, indexInnerD1, indexInnerC0, indexInnerB0)) {
                vertices[index].colorTextureCoordinates.x = texture.maxU
                vertices[index].colorTextureCoordinates.y = texture.maxV
            }
        },
        fillIndexBuffer = { indices ->
            indices.putQuad(indexInnerA4, indexInnerA0, indexOuterA0, indexOuterA4)
            indices.putQuad(indexOuterD4, indexOuterD1, indexInnerD1, indexInnerD4)
            indices.putQuad(indexOuterC2, indexOuterC0, indexInnerC0, indexInnerC2)
            indices.putQuad(indexOuterB1, indexOuterB0, indexInnerB0, indexInnerB1)
        }
    )
}

private fun generateOuterWing(props: DragonWingProperties, matrixIndices: List<Int>): ModelGenerator {
    // Since the outer wing is almost the same as the inner wing, we just generate the inner wing and adapt it
    val innerGenerator = generateInnerWing(props, matrixIndices)
    return ModelGenerator(
        numVertices = innerGenerator.numVertices,
        numIndices = innerGenerator.numIndices,
        fillVertexBuffer = { vertices, colorImageIndices, heightImageIndices ->

            val outerX = (-props.wingDepth * 0.5f).meters
            val outer = DragonTextures.Wing.Outer
            val inner = DragonTextures.Wing.Inner
            val offsetU1 = outer.minU - inner.minU
            val offsetV1 = outer.minV - inner.minV
            val offsetU2 = outer.maxU - inner.maxU
            val offsetV2 = outer.maxV - inner.maxV
            assert((offsetU1 - offsetU2).absoluteValue < 0.001f)
            assert((offsetV1 - offsetV2).absoluteValue < 0.001f)

            innerGenerator.fillVertexBuffer(vertices, colorImageIndices, heightImageIndices)

            for (vertex in vertices) {
                vertex.position.x = outerX
                vertex.normal.x = -1f
                // TODO Perhaps add height textures to dragon wing
                vertex.colorTextureCoordinates.x += offsetU1
                vertex.colorTextureCoordinates.y += offsetV1
                vertex.colorTextureIndex = matrixIndices[outer.colorTexture]
                vertex.heightTextureIndex = matrixIndices[outer.heightTexture]
            }
        },
        fillIndexBuffer = { indices ->
            innerGenerator.fillIndexBuffer(indices)

            // Reverse winding order
            var index = 0
            while (index < innerGenerator.numIndices) {
                val oldIndex = indices[index]
                indices.put(index, indices[index + 2])
                indices.put(index + 2, oldIndex)
                index += 3
            }
        }
    )
}

private fun generateInnerWing(props: DragonWingProperties, matrixIndices: List<Int>): ModelGenerator {
    val indexA0 = 0
    val indexB0 = 1
    val indexC0 = 2

    val indexA1 = 3
    val indexB1 = 4
    val indexC1 = 5
    val indexD1 = 6

    val indexA2 = 7
    val indexC2 = 8
    val indexD2 = 9

    val indexA3 = 10
    val indexD3 = 11

    val indexA4 = 12
    val indexD4 = 13

    val x = props.wingDepth * 0.5f

    return ModelGenerator(
        numVertices = 14,
        numIndices = 4 * 6 + 2 * 3,
        fillVertexBuffer = { vertices, colorTextureIndices, heightTextureIndices ->

            val texture = DragonTextures.Wing.Inner
            val du = texture.maxU - texture.minU
            val dv = texture.maxV - texture.minV

            // To avoid code duplication, set all common vertex attributes first
            for (vertex in vertices) {
                vertex.position.x = x.meters
                // The z-coordinates will be controlled by the transformation matrix
                vertex.position.z = 0f
                vertex.normal.x = 1f
                vertex.normal.y = 0f
                vertex.normal.z = 0f
                vertex.heightTextureCoordinates.x = 0f
                vertex.heightTextureCoordinates.y = 0f
                // TODO Perhaps create a separate material for skin
                vertex.materialIndex = BasicVertex.MATERIAL_TERRAIN
                // TODO Perhaps use height textures on dragon models
                vertex.deltaFactor.x = 1f
                vertex.deltaFactor.y = 1f
                vertex.colorTextureIndex = colorTextureIndices[texture.colorTexture]
                vertex.heightTextureIndex = heightTextureIndices[texture.heightTexture]
            }

            val totalLength = props.baseWingLength + props.wingTopLength * 2 + props.nailLength
            val totalWidth = props.wingLaneWidth * 2 + props.nailWidth * 3

            // The remaining attributes are position.y, colorTextureCoordinates, and matrixIndex

            // The y/v-coordinates of all vertices in the same row are identical
            fun populateRowVertices(indices: Array<Int>, y: Distance, distanceV: Distance) {
                for (index in indices) {
                    vertices[index].run {
                        position.y = y.meters
                        colorTextureCoordinates.y = texture.minV + dv * (distanceV / totalWidth)
                    }
                }
            }

            populateRowVertices(arrayOf(indexA0, indexA1, indexA2, indexA3, indexA4), props.nailWidth * 1.5f, totalWidth)
            populateRowVertices(arrayOf(indexB0, indexB1), -props.nailWidth * 1.5f, props.wingLaneWidth * 2)
            populateRowVertices(arrayOf(indexC0, indexC1, indexC2), -props.nailWidth * 1.5f - props.wingLaneWidth, props.wingLaneWidth)
            populateRowVertices(arrayOf(indexD1, indexD2, indexD3, indexD4), -props.nailWidth * 1.5f - props.wingLaneWidth * 2, Distance.meters(0))

            // The x/u-coordinates and matrix indices of all vertices in the same column are identical
            fun populateColumnVertices(indices: Array<Int>, distanceU: Distance, indirectMatrixIndex: Int) {
                for (index in indices) {
                    vertices[index].run {
                        colorTextureCoordinates.x = texture.minU + du * (distanceU / totalLength)
                        matrixIndex = matrixIndices[indirectMatrixIndex]
                    }
                }
            }

            populateColumnVertices(arrayOf(indexA0, indexB0, indexC0), props.baseWingLength + props.wingTopLength * 2, 0)
            populateColumnVertices(arrayOf(indexA1, indexB1, indexC1, indexD1), props.baseWingLength + props.wingTopLength, 1)
            populateColumnVertices(arrayOf(indexA2, indexC2, indexD2), props.baseWingLength, 2)
            populateColumnVertices(arrayOf(indexA3, indexD3), props.baseWingLength / 2, 3)
            populateColumnVertices(arrayOf(indexA4, indexD4), Distance.meters(0), 4)
        },
        fillIndexBuffer = { indices ->
            indices.putQuad(indexB1, indexB0, indexA0, indexA1)
            indices.putQuad(indexC2, indexC1, indexA1, indexA2)
            indices.putTriangle(indexC1, indexC0, indexB1)
            indices.putQuad(indexD3, indexD2, indexA2, indexA3)
            indices.putTriangle(indexD2, indexD1, indexC2)
            indices.putQuad(indexD4, indexD3, indexA3, indexA4)
        }
    )
}

class DragonWingProperties(
    val baseWingLength: Distance,
    val wingLaneWidth: Distance,
    val wingTopLength: Distance,
    val wingDepth: Distance,
    val nailLength: Distance,
    val nailWidth: Distance
)
