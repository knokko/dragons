package dsl.pm2.interpreter

class Pm2Parameter(
    val type: Pm2ParameterType,
    val name: String
) {
}

enum class Pm2ParameterType {
    Static,
    Dynamic
}
