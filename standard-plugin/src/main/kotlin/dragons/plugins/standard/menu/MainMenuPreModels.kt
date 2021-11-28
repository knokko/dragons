package dragons.plugins.standard.menu

import dragons.plugins.standard.vulkan.model.PreModel
import dragons.plugins.standard.vulkan.pipeline.MAX_NUM_DESCRIPTOR_IMAGES
import dragons.plugins.standard.vulkan.texture.PreTexture
import dragons.plugins.standard.vulkan.texture.PreTextureSet
import dragons.plugins.standard.vulkan.texture.TextureType

class MainMenuPreModels {

    val skylandColorTexture = PreTexture(type = TextureType.COLOR)
    val skylandHeightTexture = PreTexture(type = TextureType.HEIGHT)

    val textureSet = PreTextureSet(
        listOf(
            skylandColorTexture,
            skylandHeightTexture
        ),
        MAX_NUM_DESCRIPTOR_IMAGES
    )

    val skyland = PreModel(
        baseColorTextureIndex = skylandColorTexture.index,
        numColorTextures = 1,
        baseHeightTextureIndex = skylandHeightTexture.index,
        numHeightTextures = 1,
        numTransformationMatrices = 1
    )

    suspend fun await(): MainMenuModels {
        return MainMenuModels(
            textureSet = this.textureSet.await(),
            skyland = this.skyland.await()
        )
    }
}
