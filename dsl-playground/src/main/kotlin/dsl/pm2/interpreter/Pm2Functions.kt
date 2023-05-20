package dsl.pm2.interpreter

class Pm2Functions {

    private val scopes = mutableListOf<MutableMap<Pm2FunctionSignature, Pm2Function>>()

    fun pushScope() {
        scopes.add(mutableMapOf())
    }

    fun popScope() {
        scopes.removeLast()
    }

    fun defineUserFunction(signature: Pm2FunctionSignature, function: Pm2Function) {
        val lastScope = scopes.last()
        if (lastScope.containsKey(signature)) {
            throw Pm2CompileError("Duplicate function $signature")
        }

        lastScope[signature] = function
    }

    fun getUserFunction(signature: Pm2FunctionSignature): Pm2Function {
        for (scope in scopes.reversed()) {
            val maybeFunction = scope[signature]
            if (maybeFunction != null) return maybeFunction
        }
        throw Pm2CompileError("Unknown function $signature")
    }
}
