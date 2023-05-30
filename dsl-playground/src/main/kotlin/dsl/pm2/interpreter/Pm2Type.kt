package dsl.pm2.interpreter

import dsl.pm2.interpreter.value.*
import kotlin.random.Random

class Pm2Type(
    val name: String,
    val createDefaultValue: (() -> Pm2Value)?,
    val acceptValue: (Pm2Value) -> Boolean
) {
    override fun toString() = name
}

object BuiltinTypes {

    val VOID = Pm2Type("void", createDefaultValue = null, acceptValue = { value -> value is Pm2NoneValue })

    val FLOAT = Pm2Type("float", createDefaultValue = { Pm2FloatValue(0f) }, acceptValue = { value -> value is Pm2FloatValue })

    val INT = Pm2Type("int", createDefaultValue = { Pm2IntValue(0) }, acceptValue = { value -> value is Pm2IntValue })

    val POSITION = Pm2Type("position", createDefaultValue = null, acceptValue = { value -> value is Pm2PositionValue })

    val COLOR = Pm2Type("color", createDefaultValue = { Pm2ColorValue(0f, 0f, 0f) }, acceptValue = { value -> value is Pm2ColorValue })

    val VERTEX = Pm2Type("Vertex", createDefaultValue = { Pm2VertexValue() }, acceptValue = { value -> value is Pm2VertexValue })

    val RANDOM = Pm2Type("Random", createDefaultValue = { Pm2RandomValue(Random.Default) }, acceptValue = { value -> value is Pm2RandomValue })
}
