package dsl.pm2.interpreter.value

import dsl.pm2.interpreter.Pm2RuntimeError
import dsl.pm2.interpreter.Pm2Vertex
import graviks2d.util.Color

class Pm2VertexValue : Pm2Value() {

    private lateinit var position: Pm2PositionValue
    private var color = Pm2ColorValue(0f, 0f, 0f)
    private var matrix = Pm2MatrixIndexValue(0)

    override fun getProperty(propertyName: String) = when(propertyName) {
        "position" -> this.position
        "color" -> this.color
        "matrix" -> this.matrix
        else -> throw Pm2RuntimeError("Unexpected property: vertex.$propertyName")
    }

    override fun setProperty(propertyName: String, newValue: Pm2Value) {
        when(propertyName) {
            "position" -> this.position = newValue.castTo()
            "color" -> this.color = newValue.castTo()
            "matrix" -> this.matrix = newValue.castTo()
            else -> throw Pm2RuntimeError("Unexpected property: vertex.$propertyName")
        }
    }

    override fun toString(): String {
        val positionString = if (this::position.isInitialized) this.position.toString() else "undefined"
        return "Vertex($positionString, $color, matrix=${matrix.index})"
    }

    fun toVertex() = Pm2Vertex(
        x = position.getProperty("x").floatValue(),
        y = position.getProperty("y").floatValue(),
        color = Color.rgbFloat(
            color.getProperty("red").floatValue(),
            color.getProperty("green").floatValue(),
            color.getProperty("blue").floatValue()
        ),
        matrixIndex = matrix.index
    )

    override fun copy(): Pm2Value {
        val copy = Pm2VertexValue()
        if (this::position.isInitialized) copy.position = this.position
        copy.color = this.color
        copy.matrix = this.matrix
        return copy
    }
}
