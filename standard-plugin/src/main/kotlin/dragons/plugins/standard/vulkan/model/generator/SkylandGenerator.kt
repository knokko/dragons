package dragons.plugins.standard.vulkan.model.generator

import dragons.plugins.standard.vulkan.vertex.BasicVertex
import org.joml.Math.*
import java.nio.IntBuffer

fun generateSkylandModel(
    /**
     * The function that determines the shape of the skyland. The skyland model will have a center at (x, z) = (0, 0).
     * The result of this function is the distance between the center and the edge of the skyland if you move in the
     * direction indicated by the *angle*. The *angle* is the (only) parameter of the function.
     *
     * ## Angle interpretation
     *  - The angle at (x, z) = (0, 1) is 0 (and 360)
     *  - The angle at (x, z) = (1, 1) is 45
     *  - The angle at (x, z) = (1, 0) is 90
     *  - The angle at (x, z) = (1, -1) is 135
     *  - The angle at (x, z) = (0, -1) is 180
     *  - The angle at (x, z) = (-1, -1) is 225
     *  - The angle at (x, z) = (-1, 0) is 270
     *  - The angle at (x, z) = (-1, 1) is 315
     *
     *  The angles listed above simply demonstrate how the angle system works and is **not** exhaustive at all: the
     *  function should also be able to handle all the angles *between* the demonstrated angles above.
     *
     *  ## Example usage
     *   - If the radius function would always return 5, the generated skyland model will be a (nearly) perfect circle
     *  with radius 5.
     *   - If the radius function would return `5 + 5 * abs(sin(toRadians(angle)))`, the generated skyland model will be
     *   a (nearly) perfect ellipse that has a length of 10 on the x-axis and a length of 5 on the z-axis.
     *
     *   ## Recommendations
     *    - Ensure that `radiusFunction(0) == radiusFunction(360)` to avoid a weird gap at the edge on the positive Z
     *    edge of the skyland.
     *    - Ensure that the difference between `radiusFunction(a)` and `radiusFunction(b)` is small when the difference
     *    between `a` and `b` is small. This avoids sharp edges on the edge of the skyland.
     */
    radiusFunction: (Float) -> Float,
    colorTextureIndex: Int, heightTextureIndex: Int,
): ModelGenerator {

    // TODO Use more vertices for bigger skylands
    val numVerticesPerCircle = 30
    val numVerticesPerAngle = 5

    val numVertices = 1 + numVerticesPerAngle * numVerticesPerCircle
    val numIndices = 6 * (numVerticesPerAngle - 1) * numVerticesPerCircle + 3 * numVerticesPerCircle

    fun determineVertexIndex(radiusIndex: Int, angleIndex: Int) = 1 + radiusIndex * numVerticesPerCircle + angleIndex

    val fillVertexBuffer = { vertices: Array<BasicVertex> ->

        // Since this model is simple and flat, all vertices have these values in common
        for (vertex in vertices) {
            // position.x and position.z will be filled in per vertex
            vertex.position.y = 0f
            vertex.normal.x = 0f
            vertex.normal.y = 1f
            vertex.normal.z = 0f
            // colorTextureCoordinates and heightTextureCoordinates will be filled in per vertex
            vertex.matrixIndex = 0
            vertex.materialIndex = BasicVertex.MATERIAL_TERRAIN
            vertex.colorTextureIndex = colorTextureIndex
            vertex.heightTextureIndex = heightTextureIndex
        }

        // The center is a special vertex
        run {
            val center = vertices[0]
            center.position.x = 0f
            center.position.z = 0f

            center.colorTextureCoordinates.x = 0.5f
            center.colorTextureCoordinates.y = 0.5f
            center.heightTextureCoordinates.x = 0.5f
            center.heightTextureCoordinates.y = 0.5f
        }

        // We need the largest radius to compute the delta factor
        var largestRadius = 0f

        // Fill the remaining vertices
        for (angleIndex in 0 until numVerticesPerCircle) {

            val angle = 360f * angleIndex.toFloat() / numVerticesPerCircle.toFloat()
            val radius = radiusFunction(angle)
            if (radius > largestRadius) {
                largestRadius = radius
            }

            for (radiusIndex in 0 until numVerticesPerAngle) {

                val distanceFactor = (radiusIndex + 1).toFloat() / numVerticesPerAngle.toFloat()
                val distance = radius * distanceFactor

                val directionX = sin(toRadians(angle))
                val directionZ = cos(toRadians(angle))

                val vertex = vertices[determineVertexIndex(radiusIndex, angleIndex)]
                vertex.colorTextureCoordinates.x = 0.5f + 0.5f * directionX * distanceFactor
                vertex.colorTextureCoordinates.y = 0.5f + 0.5f * directionZ * distanceFactor
                vertex.heightTextureCoordinates.x = 0.5f + 0.5f * directionX * distanceFactor
                vertex.heightTextureCoordinates.y = 0.5f + 0.5f * directionZ * distanceFactor
                vertex.position.x = distance * directionX
                vertex.position.z = distance * directionZ
            }
        }

        // Now that the largest radius is known, we can set the right delta factors
        for (vertex in vertices) {
            vertex.deltaFactor.x = 2f * largestRadius
            vertex.deltaFactor.y = 2f * largestRadius
        }
    }

    val fillIndexBuffer = { indices: IntBuffer ->

        // Connect the center vertex with the inner 'circle' of vertices
        for (angleIndex in 1 .. numVerticesPerCircle) {

            val centerIndex = 0
            val firstIndex = determineVertexIndex(0, angleIndex - 1)
            val secondIndex = determineVertexIndex(0, angleIndex)

            indices.put(centerIndex)
            indices.put(firstIndex)
            indices.put(secondIndex)
        }

        // Connect the rings of vertices with each other
        for (angleIndex in 1 until numVerticesPerCircle) {
            for (radiusIndex in 1 until numVerticesPerAngle) {

                val innerFirst = determineVertexIndex(radiusIndex - 1, angleIndex - 1)
                val innerSecond = determineVertexIndex(radiusIndex - 1, angleIndex)
                val outerFirst = determineVertexIndex(radiusIndex, angleIndex - 1)
                val outerSecond = determineVertexIndex(radiusIndex, angleIndex)

                indices.put(innerFirst)
                indices.put(outerFirst)
                indices.put(outerSecond)

                indices.put(outerSecond)
                indices.put(innerSecond)
                indices.put(innerFirst)
            }
        }

        // Connect the vertices of the last angle with those of the first angle
        for (radiusIndex in 1 until numVerticesPerAngle) {
            val innerFirst = determineVertexIndex(radiusIndex - 1, numVerticesPerCircle - 1)
            val innerSecond = determineVertexIndex(radiusIndex - 1, 0)
            val outerFirst = determineVertexIndex(radiusIndex, numVerticesPerCircle - 1)
            val outerSecond = determineVertexIndex(radiusIndex, 0)

            indices.put(innerFirst)
            indices.put(outerFirst)
            indices.put(outerSecond)

            indices.put(outerSecond)
            indices.put(innerSecond)
            indices.put(innerFirst)
        }
    }

    return ModelGenerator(
        numVertices = numVertices,
        numIndices = numIndices,
        fillVertexBuffer = fillVertexBuffer,
        fillIndexBuffer = fillIndexBuffer
    )
}
