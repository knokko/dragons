package dsl.pm2.interpreter.value

import kotlin.random.Random

class Pm2RandomValue(val rng: Random) : Pm2Value() {

    override fun getProperty(propertyName: String): Pm2Value {
        return when (propertyName) {
            "nextFloat" -> Pm2FloatValue(rng.nextFloat())
            else -> super.getProperty(propertyName)
        }
    }
}
