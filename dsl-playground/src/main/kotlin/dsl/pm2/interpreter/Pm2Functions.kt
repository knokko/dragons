package dsl.pm2.interpreter

class Pm2Functions {

    private val scopes = mutableListOf<MutableMap<String, Pm2Function>>()

    fun pushScope() {
        scopes.add(mutableMapOf())
    }

    fun popScope() {
        scopes.removeLast()
    }

    fun defineUserFunction(name: String, function: Pm2Function) {
        val lastScope = scopes.last()
        if (lastScope.containsKey(name)) {
            // TODO Handle this properly
            throw IllegalArgumentException("Duplicate function $name")
        }

        lastScope[name] = function
    }

    fun getUserFunction(name: String): Pm2Function? {
        for (scope in scopes.reversed()) {
            val maybeFunction = scope[name]
            if (maybeFunction != null) return maybeFunction
        }
        return null
    }
}
