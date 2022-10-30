package dragons.plugins.standard.vulkan.render.entity

import dragons.plugins.standard.vulkan.model.generator.ModelGenerator
import dragons.plugins.standard.vulkan.vertex.BasicVertex
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TestEntityMeshTracker {

    @Test
    fun testEverything() {

        fun createMesh(numVertices: Int, numIndices: Int) = EntityMesh(
            generator = ModelGenerator(
                numVertices = numVertices, numIndices = numIndices,
                fillVertexBuffer = { _, _, _ -> throw UnsupportedOperationException("The mesh tracker shouldn't call this method") },
                fillIndexBuffer = { throw UnsupportedOperationException("The mesh tracker shouldn't call this method") }
            ),
            colorImages = listOf(DummyEntityColorImage(100, 200)),
            heightImages = listOf(DummyEntityHeightImage(200, 50)),
            numTransformationMatrices = 3
        )

        // Minimum size mesh with 4 vertices and 6 indices
        val mesh1 = createMesh(4, 6)

        // Larger mesh with 20 vertices and 80 indices
        val mesh2 = createMesh(20, 80)

        // Just as big as mesh2
        val mesh3 = createMesh(20, 80)

        // Maximum mesh that fits
        val mesh4 = createMesh(50, 200)

        // Space for 50 vertices and 200 indices
        val meshTracker = EntityMeshTracker(50 * BasicVertex.SIZE + 200 * 4)

        // Frame 1
        meshTracker.startFrame()
        meshTracker.useMesh(mesh1)
        meshTracker.endFrame()

        assertFalse(meshTracker.getLocation(mesh1).hasBeenFilled)
        checkOverlap(meshTracker, listOf(mesh1))
        meshTracker.markFilled(mesh1)
        assertTrue(meshTracker.getLocation(mesh1).hasBeenFilled)

        // Frame 2
        meshTracker.startFrame()
        for (counter in 0 until 5) {
            meshTracker.useMesh(mesh1)
        }
        meshTracker.useMesh(mesh2)
        meshTracker.endFrame()

        assertTrue(meshTracker.getLocation(mesh1).hasBeenFilled)
        assertFalse(meshTracker.getLocation(mesh2).hasBeenFilled)
        meshTracker.markFilled(mesh2)
        assertTrue(meshTracker.getLocation(mesh1).hasBeenFilled)
        assertTrue(meshTracker.getLocation(mesh2).hasBeenFilled)
        checkOverlap(meshTracker, listOf(mesh1, mesh2))

        // Frame 3
        meshTracker.startFrame()
        meshTracker.useMesh(mesh1)
        meshTracker.useMesh(mesh3)
        meshTracker.endFrame()

        assertTrue(meshTracker.getLocation(mesh1).hasBeenFilled)
        assertFalse(meshTracker.getLocation(mesh3).hasBeenFilled)
        checkOverlap(meshTracker, listOf(mesh1, mesh3))

        // Frame 4
        meshTracker.startFrame()
        meshTracker.useMesh(mesh4)
        meshTracker.endFrame()

        checkOverlap(meshTracker, listOf(mesh4))

        // Frame 5
        meshTracker.startFrame()
        meshTracker.useMesh(mesh3)
        meshTracker.endFrame()

        assertFalse(meshTracker.getLocation(mesh3).hasBeenFilled)
        checkOverlap(meshTracker, listOf(mesh3))

        meshTracker.destroy()
    }

    private fun checkOverlap(tracker: EntityMeshTracker, meshes: List<EntityMesh>) {
        val usedBytes = Array(tracker.size) { false }

        for (mesh in meshes) {
            val location = tracker.getLocation(mesh)
            for (byteIndex in location.vertexOffset until location.vertexOffset + mesh.generator.numVertices * BasicVertex.SIZE) {
                assertFalse(usedBytes[byteIndex])
                usedBytes[byteIndex] = true
            }
            for (byteIndex in location.indexOffset until location.indexOffset + mesh.generator.numIndices * 4) {
                assertFalse(usedBytes[byteIndex])
                usedBytes[byteIndex] = true
            }
        }
    }
}