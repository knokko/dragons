package dsl.pm2.renderer

import dsl.pm2.interpreter.Pm2DynamicMatrix
import troll.buffer.VmaBuffer

class Pm2Mesh(
        internal val vertexBuffer: VmaBuffer,
        internal val vertexOffset: Int,
        internal val numVertices: Int,
        internal val matrices: List<Pm2DynamicMatrix?>,
)
