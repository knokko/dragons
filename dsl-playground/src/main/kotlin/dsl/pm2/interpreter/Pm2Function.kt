package dsl.pm2.interpreter

class Pm2Function(
    val startInstruction: Int,
    val returnType: Pm2Type
)

class Pm2FunctionSignature(
    val name: String,
    val numParameters: Int
) {
    override fun toString() = "$name($numParameters parameters)"

    override fun equals(other: Any?) = other is Pm2FunctionSignature
            && other.name == name && other.numParameters == numParameters

    override fun hashCode() = name.hashCode() + 123 * numParameters
}
