package dsl.pm2.renderer

import com.github.knokko.boiler.buffer.VmaBuffer
import dsl.pm2.interpreter.Pm2DynamicMatrix
import dsl.pm2.interpreter.Pm2Type

class Pm2Mesh(
        internal val vertexBuffer: VmaBuffer,
        internal val vertexOffset: Int,
        internal val numVertices: Int,
        internal val matrices: List<Pm2DynamicMatrix?>,
        internal val dynamicParameterTypes: Map<String, Pm2Type>
)
