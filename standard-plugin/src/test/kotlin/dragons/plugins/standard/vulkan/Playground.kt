package dragons.plugins.standard.vulkan

import org.joml.Matrix4f
import org.joml.Vector3f

fun main() {
    val matrix = Matrix4f().rotate(0.4f, Vector3f(0f, 1f, 0f)).translate(4f, 5f, 6f)

    println("getTranslation: ${matrix.getTranslation(Vector3f())}")
    println("transformation: ${matrix.transformPosition(Vector3f())}")
}
