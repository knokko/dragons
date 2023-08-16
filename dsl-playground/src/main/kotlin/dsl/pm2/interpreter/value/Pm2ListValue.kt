package dsl.pm2.interpreter.value

import dsl.pm2.interpreter.Pm2RuntimeError

class Pm2ListValue(val elements: MutableList<Pm2Value>): Pm2Value() {

    override fun toString() = "Pm2ListValue($elements)"

    override fun copy() = Pm2ListValue(elements.map { it.copy() }.toMutableList())

    override fun getProperty(propertyName: String): Pm2Value {
        if (propertyName == "size") return Pm2IntValue(elements.size)
        else throw Pm2RuntimeError("Unknown property: List.$propertyName")
    }

    override operator fun times(right: Pm2Value): Pm2Value {
        return if (right is Pm2IntValue) {
            val newElements = ArrayList<Pm2Value>(right.intValue() * elements.size)
            for (counter in 0 until right.intValue()) newElements.addAll(elements)
            Pm2ListValue(newElements)
        } else super.times(right)
    }

    override operator fun plus(right: Pm2Value): Pm2Value {
        return if (right is Pm2ListValue) Pm2ListValue((this.elements + right.elements).toMutableList())
        else super.times(right)
    }

    override operator fun get(key: Pm2Value): Pm2Value {
        return if (key is Pm2IntValue) elements[key.intValue()] else super.get(key)
    }

    override operator fun set(key: Pm2Value, value: Pm2Value) {
        if (key is Pm2IntValue) elements[key.intValue()] = value
        else super.set(key, value)
    }
}
