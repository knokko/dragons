package dragons.plugins.standard.menu

import dragons.plugins.standard.vulkan.model.PreModel
import dragons.plugins.standard.vulkan.model.generator.FlowerGenerators
import dragons.plugins.standard.vulkan.pipeline.MAX_NUM_DESCRIPTOR_IMAGES
import dragons.plugins.standard.vulkan.texture.PreTexture
import dragons.plugins.standard.vulkan.texture.PreTextureSet
import dragons.plugins.standard.vulkan.texture.TextureType

class MainMenuPreModels {

    val zeroHeightTexture = PreTexture(type = TextureType.HEIGHT)

    val skylandColorTexture = PreTexture(type = TextureType.COLOR)
    val skylandHeightTexture = PreTexture(type = TextureType.HEIGHT)

    val debugPanelTexture = PreTexture(type = TextureType.COLOR)

    val flowerStem1ColorTexture = PreTexture(type = TextureType.COLOR)
    val flowerStem1HeightTexture = PreTexture(type = TextureType.HEIGHT)
    val flowerTop1ColorTexture = PreTexture(type = TextureType.COLOR)
    val flowerTop1HeightTexture = PreTexture(type = TextureType.HEIGHT)

    val flowerStem2ColorTexture = PreTexture(type = TextureType.COLOR)
    val flowerStem2HeightTexture = PreTexture(type = TextureType.HEIGHT)
    val flowerTop2ColorTexture = PreTexture(type = TextureType.COLOR)
    val flowerTop2HeightTexture = PreTexture(type = TextureType.HEIGHT)

    val textureSet = PreTextureSet(
        listOf(
            skylandColorTexture,
            skylandHeightTexture,
            debugPanelTexture,
            flowerStem1ColorTexture,
            flowerStem1HeightTexture,
            flowerTop1ColorTexture,
            flowerTop1HeightTexture,
            flowerStem2ColorTexture,
            flowerStem2HeightTexture,
            flowerTop2ColorTexture,
            flowerTop2HeightTexture
        ),
        MAX_NUM_DESCRIPTOR_IMAGES
    )

    val skyland = PreModel(
        colorTextures = listOf(skylandColorTexture.index),
        heightTextures = listOf(skylandHeightTexture.index),
        numTransformationMatrices = 1
    )

    val debugPanel = PreModel(
        colorTextures = listOf(debugPanelTexture.index),
        heightTextures = listOf(zeroHeightTexture.index),
        numTransformationMatrices = 1
    )

    val flower1 = PreModel(
        colorTextures = listOf(flowerStem1ColorTexture.index, flowerTop1ColorTexture.index),
        heightTextures = listOf(flowerStem1HeightTexture.index, flowerTop1HeightTexture.index),
        numTransformationMatrices = FlowerGenerators.BUSH_SIZE1
    )

    val flower2 = PreModel(
        colorTextures = listOf(flowerStem2ColorTexture.index, flowerTop2ColorTexture.index),
        heightTextures = listOf(flowerStem2HeightTexture.index, flowerTop2HeightTexture.index),
        numTransformationMatrices = FlowerGenerators.BUSH_SIZE1
    )

    suspend fun await(): MainMenuModels {
        return MainMenuModels(
            textureSet = this.textureSet.await(),
            skyland = this.skyland.await(),
            debugPanel = this.debugPanel.await(),
            flower1 = this.flower1.await(),
            flower2 = this.flower2.await()
        )
    }
}
