package dsl.pm2.interpreter

class Pm2Types {

    private val scopes = mutableListOf<MutableMap<String, Pm2Type>>()

    fun pushScope() {
        scopes.add(mutableMapOf())
    }

    fun popScope() {
        scopes.removeLast()
    }

    fun defineType(name: String, type: Pm2Type) {
        val lastScope = scopes.last()
        if (lastScope.containsKey(name)) {
            throw Pm2CompileError("Duplicate type $name")
        }

        lastScope[name] = type
    }

    fun getType(name: String): Pm2Type {
        for (scope in scopes.reversed()) {
            val maybeType = scope[name]
            if (maybeType != null) return maybeType
        }
        throw Pm2CompileError("Unknown type $name")
    }
}
