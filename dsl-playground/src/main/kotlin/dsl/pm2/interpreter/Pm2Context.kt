package dsl.pm2.interpreter

import dsl.pm2.interpreter.value.Pm2Value

class Pm2Context {
    private val scopes = mutableListOf<Pm2Scope>()

    fun popScope() {
        this.scopes.removeLast()
    }

    fun pushScope() {
        this.scopes.add(Pm2Scope())
    }

    @Throws(IllegalArgumentException::class)
    fun defineFunction(name: String, function: Pm2Function) {
        if (this.getFunction(name) != null) throw IllegalArgumentException("Function $name already exists")
        this.scopes.last().functions[name] = function
    }

    fun getFunction(name: String): Pm2Function? {
        for (scope in this.scopes) {
            val maybeFunction = scope.functions[name]
            if (maybeFunction != null) return maybeFunction
        }

        return null
    }

    @Throws(IllegalArgumentException::class)
    fun defineType(name: String, type: Pm2Type) {
        if (getType(name) != null) throw IllegalArgumentException("Type $name is already defined")
        this.scopes.last().types[name] = type
    }

    fun getType(name: String): Pm2Type? {
        for (scope in this.scopes) {
            val maybeType = scope.types[name]
            if (maybeType != null) return maybeType
        }

        return null
    }

    @Throws(IllegalArgumentException::class)
    fun declareVariable(name: String, typeName: String) {
        if (this.getVariable(name) != null) throw IllegalArgumentException("Variable $name is already defined")
        val type = this.getType(typeName) ?: throw IllegalArgumentException("Unknown type $typeName")
        if (type.createDefaultValue == null) throw IllegalArgumentException("Type $typeName doesn't have a default value")
        this.scopes.last().variables[name] = Pair(type, type.createDefaultValue.invoke())
    }

    @Throws(IllegalArgumentException::class)
    fun declareVariable(name: String, typeName: String, initialValue: Pm2Value) {
        if (this.getVariable(name) != null) throw IllegalArgumentException("Variable $name is already defined")
        val type = this.getType(typeName) ?: throw IllegalArgumentException("Unknown type $typeName")
        if (!type.acceptValue(initialValue)) throw IllegalArgumentException("$initialValue is invalid for type $typeName")
        this.scopes.last().variables[name] = Pair(type, initialValue)
    }

    @Throws(IllegalArgumentException::class)
    fun reassignVariable(name: String, newValue: Pm2Value) {
        for (scope in this.scopes) {
            val maybeVariable = scope.variables[name]
            if (maybeVariable != null) {
                val type = maybeVariable.first
                if (!type.acceptValue(newValue)) {
                    throw IllegalArgumentException("Type $type doesn't accept value $newValue")
                }
                scope.variables[name] = Pair(type, newValue)
                return
            }
        }

        throw IllegalArgumentException("Unknown variable $name")
    }

    fun getVariable(name: String): Pair<Pm2Type, Pm2Value>? {
        for (scope in this.scopes) {
            val maybeVariable = scope.variables[name]
            if (maybeVariable != null) return maybeVariable
        }

        return null
    }
}

private class Pm2Scope {
    val types = mutableMapOf<String, Pm2Type>()
    val functions = mutableMapOf<String, Pm2Function>()
    val variables = mutableMapOf<String, Pair<Pm2Type, Pm2Value>>()
}
