package dsl.pm2.interpreter.value

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.random.Random

class Pm2RandomValue(private val rng: Random) : Pm2Value() {

    override fun getProperty(propertyName: String): Pm2Value {
        return when (propertyName) {
            "nextFloat" -> Pm2FloatValue(rng.nextFloat())
            else -> super.getProperty(propertyName)
        }
    }

    override fun copy(): Pm2RandomValue {
        // Ripped from https://stackoverflow.com/a/54156572
        // Uses serialization to create a copy of the given Random, needed for repeatability in some tests.
        val bo = ByteArrayOutputStream()
        val oos = ObjectOutputStream(bo)
        oos.writeObject(rng);
        oos.close();
        val ois = ObjectInputStream(ByteArrayInputStream(bo.toByteArray()))
        return Pm2RandomValue(ois.readObject() as Random)
    }
}
