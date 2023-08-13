package dsl.pm2.interpreter.value

import dsl.pm2.interpreter.Pm2RuntimeError

class Pm2MapValue: Pm2Value() {

    internal val map = mutableMapOf<Pm2Value, Pm2Value>()

    override fun copy(): Pm2Value {
        val copy = Pm2MapValue()
        for ((key, value) in this.map) {
            copy.map[key.copy()] = value.copy()
        }
        return copy
    }

    override fun toString() = "Pm2MapValue(${map.size} entries)"

    override operator fun get(key: Pm2Value) = map[key] ?: throw Pm2RuntimeError("Map $map doesn't have key $key")

    override operator fun set(key: Pm2Value, value: Pm2Value) {
        map[key] = value
    }

    override fun equals(other: Any?) = other is Pm2MapValue && this.map == other.map

    override fun hashCode() = this.map.hashCode()
}
