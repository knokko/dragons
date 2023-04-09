package dsl.pm2.interpreter

import dsl.pm2.interpreter.value.Pm2Value
import dsl.pm2.interpreter.value.Pm2VertexValue

class Pm2Function(
    val parameterTypes: List<Pm2Type>,
    val returnType: Pm2Type?,
    private val invoke: (parameters: List<Pm2Value>) -> Pm2Value?
) {
    fun invokeChecked(parameters: List<Pm2Value>): Pm2Value? {
        if (parameters.size != parameterTypes.size) {
            throw IllegalArgumentException("Wrong number of parameters: expected ${parameterTypes.size}; got ${parameters.size}")
        }
        for (index in parameters.indices) {
            if (!parameterTypes[index].acceptValue(parameters[index])) {
                throw IllegalArgumentException("Type $index (${parameterTypes[index]}) doesn't accept value ${parameters[index]}")
            }
        }

        val result = invoke(parameters)
        if (returnType == null && result != null) throw IllegalArgumentException("Function shouldn't have a result")
        if (returnType != null && result == null) throw IllegalArgumentException("Function should have a result")
        if (returnType != null && result != null && !returnType.acceptValue(result)) {
            throw IllegalArgumentException("Return type $returnType doesn't accept result $result")
        }
        return result
    }
}

object BuiltinFunctions {
    fun produceTriangle(produce: (vertex1: Pm2VertexValue, vertex2: Pm2VertexValue, vertex3: Pm2VertexValue) -> Unit) = Pm2Function(
        parameterTypes = listOf(BuiltinTypes.VERTEX, BuiltinTypes.VERTEX, BuiltinTypes.VERTEX), returnType = null, invoke = { vertices ->
            produce(vertices[0] as Pm2VertexValue, vertices[1] as Pm2VertexValue, vertices[2] as Pm2VertexValue)
            null
        }
    )
}
