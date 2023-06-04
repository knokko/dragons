package dsl.pm2.renderer

import dsl.pm2.interpreter.Pm2DynamicMatrix

class Pm2Mesh(
        internal val vertexAllocation: Long,
        internal val vertexBuffer: Long,
        internal val vertexOffset: Int,
        internal val numVertices: Int,
        internal val matrices: List<Pm2DynamicMatrix?>,
)
