package dsl.pm2.interpreter.value

import dsl.pm2.interpreter.Pm2RuntimeError

abstract class Pm2Value {

    @kotlin.jvm.Throws(Pm2RuntimeError::class)
    inline fun <reified T> castTo(): T {
        if (this is T) {
            return this
        } else {
            throw Pm2RuntimeError("Expected ${T::class.simpleName}, but found $this")
        }
    }

    @Throws(Pm2RuntimeError::class)
    open fun setProperty(propertyName: String, newValue: Pm2Value) {
        throw Pm2RuntimeError("$this doesn't have properties")
    }

    @Throws(Pm2RuntimeError::class)
    open fun getProperty(propertyName: String): Pm2Value {
        throw Pm2RuntimeError("$this doesn't have properties")
    }

    @Throws(Pm2RuntimeError::class)
    open fun floatValue(): Float {
        throw Pm2RuntimeError("$this is not of type float")
    }

    @Throws(Pm2RuntimeError::class)
    open fun intValue(): Int {
        throw Pm2RuntimeError("$this is not of type int")
    }

    @Throws(Pm2RuntimeError::class)
    open fun booleanValue(): Boolean {
        throw Pm2RuntimeError("$this is not of type boolean")
    }

    @Throws(Pm2RuntimeError::class)
    open operator fun div(right: Pm2Value): Pm2Value {
        throw Pm2RuntimeError("No semantics for ${this::class.simpleName} / ${right::class.simpleName}")
    }

    @Throws(Pm2RuntimeError::class)
    open operator fun times(right: Pm2Value): Pm2Value {
        throw Pm2RuntimeError("No semantics for ${this::class.simpleName} * ${right::class.simpleName}")
    }

    @Throws(Pm2RuntimeError::class)
    open operator fun minus(right: Pm2Value): Pm2Value {
        throw Pm2RuntimeError("No semantics for ${this::class.simpleName} - ${right::class.simpleName}")
    }

    @Throws(Pm2RuntimeError::class)
    open operator fun plus(right: Pm2Value): Pm2Value {
        throw Pm2RuntimeError("No semantics for ${this::class.simpleName} + ${right::class.simpleName}")
    }

    @Throws(Pm2RuntimeError::class)
    open operator fun compareTo(right: Pm2Value): Int {
        throw Pm2RuntimeError("No semantics for ${this::class.simpleName} cmp ${right::class.simpleName}")
    }
}
