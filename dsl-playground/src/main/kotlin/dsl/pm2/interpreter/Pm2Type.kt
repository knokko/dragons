package dsl.pm2.interpreter

import dsl.pm2.interpreter.value.*

class Pm2Type(
    val createDefaultValue: (() -> Pm2Value)?,
    val acceptValue: (Pm2Value) -> Boolean
)

object BuiltinTypes {

    val FLOAT = Pm2Type(createDefaultValue = { Pm2FloatValue(0f) }, acceptValue = { value -> value is Pm2FloatValue })

    val INT = Pm2Type(createDefaultValue = { Pm2IntValue(0) }, acceptValue = { value -> value is Pm2IntValue })

    val POSITION = Pm2Type(createDefaultValue = null, acceptValue = { value -> value is Pm2PositionValue })

    val VERTEX = Pm2Type(createDefaultValue = { Pm2VertexValue() }, acceptValue = { value -> value is Pm2VertexValue })
}
