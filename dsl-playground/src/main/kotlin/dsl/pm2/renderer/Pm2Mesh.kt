package dsl.pm2.renderer

class Pm2Mesh(
    internal val vmaAllocation: Long,
    internal val vkBuffer: Long,
    internal val vertexOffset: Int,
    internal val numVertices: Int
) {
}
