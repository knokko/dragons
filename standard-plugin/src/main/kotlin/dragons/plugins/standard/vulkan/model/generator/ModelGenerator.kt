package dragons.plugins.standard.vulkan.model.generator

import dragons.plugins.standard.vulkan.vertex.BasicVertex
import java.nio.IntBuffer

class ModelGenerator(
    val numVertices: Int,
    val numIndices: Int,
    val fillVertexBuffer: (Array<BasicVertex>) -> Unit,
    val fillIndexBuffer: (IntBuffer) -> Unit
) {
}