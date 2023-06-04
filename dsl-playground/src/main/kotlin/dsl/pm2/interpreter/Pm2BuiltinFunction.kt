package dsl.pm2.interpreter

import dsl.pm2.interpreter.value.Pm2NoneValue
import dsl.pm2.interpreter.value.Pm2Value

class Pm2BuiltinFunction(
    val parameterTypes: List<Pm2Type>,
    val returnType: Pm2Type?
) {

    fun invoke(valueStack: MutableList<Pm2Value>, implementation: (List<Pm2Value>) -> Pm2Value?) {
        val parameterValues = parameterTypes.indices.map { valueStack.removeLast() }.reversed()
        for ((index, type) in parameterTypes.withIndex()) {
            if (!type.acceptValue(parameterValues[index])) {
                throw Pm2RuntimeError("Parameter type $type doesn't accept parameter $index (${parameterValues[index]})")
            }
        }

        val result = implementation(parameterValues)
        if (returnType == null) {
            if (result != null) throw Pm2RuntimeError("Unexpected return value $result")
            valueStack.add(Pm2NoneValue())
        } else {
            if (result == null) throw Pm2RuntimeError("Expected a result of type $returnType")
            if (!returnType.acceptValue(result)) throw Pm2RuntimeError("Return type ($returnType) doesn't accept result $result")
            valueStack.add(result)
        }
    }

    companion object {
        val PRODUCE_TRIANGLE = Pm2BuiltinFunction(listOf(BuiltinTypes.VERTEX, BuiltinTypes.VERTEX, BuiltinTypes.VERTEX), null)
        val CONSTRUCT_POSITION = Pm2BuiltinFunction(listOf(BuiltinTypes.FLOAT, BuiltinTypes.FLOAT), BuiltinTypes.POSITION)
        val RGB = Pm2BuiltinFunction(listOf(BuiltinTypes.FLOAT, BuiltinTypes.FLOAT, BuiltinTypes.FLOAT), BuiltinTypes.COLOR)
        val FLOAT = Pm2BuiltinFunction(listOf(BuiltinTypes.INT), BuiltinTypes.FLOAT)
        val INT = Pm2BuiltinFunction(listOf(BuiltinTypes.FLOAT), BuiltinTypes.INT)
        val SIN = Pm2BuiltinFunction(listOf(BuiltinTypes.FLOAT), BuiltinTypes.FLOAT)
        val COS = Pm2BuiltinFunction(listOf(BuiltinTypes.FLOAT), BuiltinTypes.FLOAT)
        val TRANSLATE_MATRIX = Pm2BuiltinFunction(listOf(BuiltinTypes.MATRIX, BuiltinTypes.FLOAT, BuiltinTypes.FLOAT), null)
        val ROTATE_MATRIX = Pm2BuiltinFunction(listOf(BuiltinTypes.MATRIX, BuiltinTypes.FLOAT), null)
        val SCALE_MATRIX = Pm2BuiltinFunction(listOf(BuiltinTypes.MATRIX, BuiltinTypes.FLOAT, BuiltinTypes.FLOAT), null)

        val MAP = mutableMapOf(
            Pair("produceTriangle", PRODUCE_TRIANGLE),
            Pair("constructPosition", CONSTRUCT_POSITION),
            Pair("rgb", RGB),
            Pair("int", INT),
            Pair("float", FLOAT),
            Pair("sin", SIN),
            Pair("cos", COS),
            Pair("translate", TRANSLATE_MATRIX),
            Pair("rotate", ROTATE_MATRIX),
            Pair("scale", SCALE_MATRIX)
        )
    }
}
