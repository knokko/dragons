package dragons.plugins.standard.vulkan.command

import dragons.plugins.standard.state.StandardGraphicsState
import dragons.plugins.standard.vulkan.model.generator.FlowerGenerators
import dragons.plugins.standard.vulkan.render.SceneRenderer
import org.joml.Math.toRadians
import org.joml.Matrix4f
import org.joml.Vector3f
import java.util.*

fun fillDrawingBuffers(
    renderer: SceneRenderer, graphicsState: StandardGraphicsState, averageEyePosition: Vector3f
) {
    val numTerrainDrawCalls = 300
    val numDebugPanels = 4

    // Since we use the average eye position as the origin of the render scene, we translate every object by its negation
    val negativeEyePosition = averageEyePosition.negate(Vector3f())

    for (currentDrawCall in 0 until numTerrainDrawCalls) {
        val transformationMatrix = Matrix4f()
            .translate(negativeEyePosition)
            .scale(10f)
            .translate(
                -10f + 2f * ((currentDrawCall % 100) / 10),
                -4f + 4f * (currentDrawCall / 100),
                -10f + 2f * (currentDrawCall % 10)
            )

        renderer.tileRenderer.drawTile(
            vertices = graphicsState.mainMenu.skyland.vertices,
            indices = graphicsState.mainMenu.skyland.indices,
            transformationMatrices = arrayOf(transformationMatrix)
        )
    }

    val debugMatrices = run {
        val scaleX = 60f
        val aspectRatio = graphicsState.debugPanel.width.toFloat() / graphicsState.debugPanel.height.toFloat()
        val scaleY = scaleX / aspectRatio
        val y = 5f

        arrayOf(
            Matrix4f().translate(0f, y, -30f).translate(negativeEyePosition).rotateY(toRadians(0f)).scale(scaleX, scaleY, 1f),
            Matrix4f().translate(0f, y, 30f).translate(negativeEyePosition).rotateY(toRadians(180f)).scale(scaleX, scaleY, 1f),
            Matrix4f().translate(30f, y, 0f).translate(negativeEyePosition).rotateY(toRadians(270f)).scale(scaleX, scaleY, 1f),
            Matrix4f().translate(-30f, y, 0f).translate(negativeEyePosition).rotateY(toRadians(90f)).scale(scaleX, scaleY, 1f)
        )
    }

    for (panelIndex in 0 until numDebugPanels) {
        renderer.tileRenderer.drawTile(
            vertices = graphicsState.mainMenu.debugPanel.vertices,
            indices = graphicsState.mainMenu.debugPanel.indices,
            transformationMatrices = arrayOf(debugMatrices[panelIndex])
        )
    }

    val rng = Random(1234)
    for (flowerModel in arrayOf(graphicsState.mainMenu.flower1, graphicsState.mainMenu.flower2)) {
        val numFlowerMatrices = FlowerGenerators.BUSH_SIZE1

        val flowerMatrices = (0 until numFlowerMatrices).map {
            Matrix4f()
                .translate(6 * rng.nextFloat() - 3, 0f, 6 * rng.nextFloat() - 3)
                .translate(negativeEyePosition)
                .scale(1f)
        }.toTypedArray()

        renderer.tileRenderer.drawTile(
            vertices = flowerModel.vertices,
            indices = flowerModel.indices,
            transformationMatrices = flowerMatrices
        )
    }
}
