package dsl.pm2.interpreter

import dsl.pm2.interpreter.value.Pm2Value

class Pm2VariableScope {
    private val scopes = mutableListOf<MutableMap<String, Pair<Pm2Type, Pm2Value>>>()

    fun pushScope() {
        scopes.add(mutableMapOf())
    }

    fun popScope() {
        scopes.removeLast()
    }

    fun defineVariable(type: Pm2Type, name: String, initialValue: Pm2Value) {
        val lastScope = scopes.last()
        if (lastScope.containsKey(name)) throw Pm2RuntimeError("Duplicate variable $name")
        if (!type.acceptValue(initialValue)) throw Pm2RuntimeError("Type $type doesn't accept $initialValue")
        lastScope[name] = Pair(type, initialValue)
    }

    fun reassignVariable(name: String, newValue: Pm2Value) {
        for (scope in scopes.reversed()) {
            val maybeVariable = scope[name]
            if (maybeVariable != null) {
                if (!maybeVariable.first.acceptValue(newValue)) {
                    throw Pm2RuntimeError("$name has type ${maybeVariable.first}, which doesn't accept $newValue")
                }
                scope[name] = Pair(maybeVariable.first, newValue)
                return
            }
        }
        throw Pm2RuntimeError("Unknown variable $name")
    }

    fun getVariable(name: String): Pm2Value? {
        for (scope in scopes.reversed()) {
            val maybeVariable = scope[name]
            if (maybeVariable != null) return maybeVariable.second
        }
        return null
    }
}
