package dragons.plugins.standard.vulkan.model.matrices

import dragons.plugins.standard.vulkan.model.generator.dragon.DragonWingProperties
import org.joml.Matrix4f

fun createDragonWingMatrices(baseMatrix: Matrix4f, shoulderMatrix: Matrix4f, props: DragonWingProperties): Array<Matrix4f> {
    val directionBasedElbow = baseMatrix.translate(0f, 0f, props.wingTopLength.meters.toFloat() * 2, Matrix4f())

    fun mix(mixer: Float, matrix0: Matrix4f, matrix1: Matrix4f): Matrix4f {
        return matrix0.lerp(matrix1, mixer, Matrix4f())
    }

    val positionBasedElbow = mix(0.5f, baseMatrix, shoulderMatrix)
    val elbowMatrix = mix(0.6f, directionBasedElbow, positionBasedElbow)

    return arrayOf(
        baseMatrix,
        mix(0.5f, elbowMatrix, baseMatrix),
        elbowMatrix,
        mix(0.5f, shoulderMatrix, elbowMatrix),
        shoulderMatrix
    )
}
