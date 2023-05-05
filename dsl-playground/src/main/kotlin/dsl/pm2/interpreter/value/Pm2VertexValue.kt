package dsl.pm2.interpreter.value

import dsl.pm2.interpreter.Pm2Vertex

class Pm2VertexValue : Pm2Value() {

    private lateinit var position: Pm2PositionValue

    override fun getProperty(propertyName: String) = when(propertyName) {
        "position" -> this.position
        else -> throw UnsupportedOperationException("Unexpected property: vertex.$propertyName")
    }

    override fun setProperty(propertyName: String, newValue: Pm2Value) {
        when(propertyName) {
            "position" -> this.position = newValue as Pm2PositionValue
            else -> throw UnsupportedOperationException("Unexpected property: vertex.$propertyName")
        }
    }

    override fun toString(): String {
        val positionString = if (this::position.isInitialized) this.position.toString() else "undefined"
        return "Vertex($positionString, identity=${System.identityHashCode(this)})"
    }

    fun toVertex() = Pm2Vertex(
        x = position.getProperty("x").floatValue(),
        y = position.getProperty("y").floatValue()
    )
}
