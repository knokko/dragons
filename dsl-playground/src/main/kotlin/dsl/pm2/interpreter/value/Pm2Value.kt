package dsl.pm2.interpreter.value

abstract class Pm2Value {

    @Throws(UnsupportedOperationException::class)
    open fun setProperty(propertyName: String, newValue: Pm2Value) {
        throw UnsupportedOperationException("This variable doesn't have properties")
    }

    @Throws(UnsupportedOperationException::class)
    open fun getProperty(propertyName: String): Pm2Value {
        throw UnsupportedOperationException("This variable doesn't have properties")
    }

    @Throws(UnsupportedOperationException::class)
    open fun floatValue(): Float {
        throw UnsupportedOperationException("This variable is not of type float")
    }

    @Throws(UnsupportedOperationException::class)
    open fun intValue(): Int {
        throw UnsupportedOperationException("This variable is not of type int")
    }

    @Throws(UnsupportedOperationException::class)
    open fun booleanValue(): Boolean {
        throw UnsupportedOperationException("This variable is not of type boolean")
    }

    @Throws(UnsupportedOperationException::class)
    open operator fun div(right: Pm2Value): Pm2Value {
        throw UnsupportedOperationException("No semantics for ${this::class.simpleName} / ${right::class.simpleName}")
    }

    @Throws(UnsupportedOperationException::class)
    open operator fun times(right: Pm2Value): Pm2Value {
        throw UnsupportedOperationException("No semantics for ${this::class.simpleName} * ${right::class.simpleName}")
    }

    @Throws(UnsupportedOperationException::class)
    open operator fun minus(right: Pm2Value): Pm2Value {
        throw UnsupportedOperationException("No semantics for ${this::class.simpleName} - ${right::class.simpleName}")
    }

    @Throws(UnsupportedOperationException::class)
    open operator fun plus(right: Pm2Value): Pm2Value {
        throw UnsupportedOperationException("No semantics for ${this::class.simpleName} + ${right::class.simpleName}")
    }

    @Throws(UnsupportedOperationException::class)
    open operator fun compareTo(right: Pm2Value): Int {
        throw UnsupportedOperationException("No semantics for ${this::class.simpleName} cmp ${right::class.simpleName}")
    }
}
