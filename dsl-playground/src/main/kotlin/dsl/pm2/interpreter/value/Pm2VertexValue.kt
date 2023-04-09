package dsl.pm2.interpreter.value

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
        return "Vertex($positionString)"
    }
}
