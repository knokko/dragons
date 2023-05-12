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
                throw IllegalArgumentException("Parameter type $type doesn't accept parameter $index (${parameterValues[index]})")
            }
        }

        val result = implementation(parameterValues)
        if (returnType == null) {
            if (result != null) throw RuntimeException("Unexpected return value $result")
            valueStack.add(Pm2NoneValue())
        } else {
            if (result == null) throw RuntimeException("Expected a result of type $returnType")
            if (!returnType.acceptValue(result)) throw RuntimeException("Return type ($returnType) doesn't accept result $result")
            valueStack.add(result)
        }
    }

    companion object {
        val PRODUCE_TRIANGLE = Pm2BuiltinFunction(listOf(BuiltinTypes.VERTEX, BuiltinTypes.VERTEX, BuiltinTypes.VERTEX), null)
        val CONSTRUCT_POSITION = Pm2BuiltinFunction(listOf(BuiltinTypes.FLOAT, BuiltinTypes.FLOAT), BuiltinTypes.POSITION)
        val FLOAT = Pm2BuiltinFunction(listOf(BuiltinTypes.INT), BuiltinTypes.FLOAT)
        val INT = Pm2BuiltinFunction(listOf(BuiltinTypes.FLOAT), BuiltinTypes.INT)
        val SIN = Pm2BuiltinFunction(listOf(BuiltinTypes.FLOAT), BuiltinTypes.FLOAT)
        val COS = Pm2BuiltinFunction(listOf(BuiltinTypes.FLOAT), BuiltinTypes.FLOAT)

        val MAP = mutableMapOf(
            Pair("produceTriangle", PRODUCE_TRIANGLE),
            Pair("constructPosition", CONSTRUCT_POSITION),
            Pair("int", INT),
            Pair("float", FLOAT),
            Pair("sin", SIN),
            Pair("cos", COS)
        )
    }
}