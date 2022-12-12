package dragons.plugins.standard.vulkan.model.matrices

import dragons.plugins.standard.vulkan.model.generator.dragon.DragonWingProperties
import dragons.space.Distance
import org.joml.Matrix4f

fun createDragonWingMatrices(baseMatrix: Matrix4f, props: DragonWingProperties): Array<Matrix4f> {

    fun createSubMatrix(distance: Distance): Matrix4f {
        return baseMatrix.translate(0f, 0f, distance.meters, Matrix4f())
    }

    return arrayOf(
        baseMatrix,
        createSubMatrix(props.wingTopLength),
        createSubMatrix(props.wingTopLength * 2),
        createSubMatrix(props.wingTopLength * 2 + props.baseWingLength * 0.5f),
        createSubMatrix(props.wingTopLength * 2 + props.baseWingLength)
    )
}
