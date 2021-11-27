package dragons.plugins.standard.menu

import dragons.plugins.standard.vulkan.model.PreModel
import dragons.plugins.standard.vulkan.texture.PreTexture
import dragons.plugins.standard.vulkan.texture.TextureType

class MainMenuPreModels {

    val skyland = PreModel(
        baseColorTextureIndex = 0,
        numColorTextures = 1,
        baseHeightTextureIndex = 0,
        numHeightTextures = 1,
        numTransformationMatrices = 1
    )

    val skylandColorTexture = PreTexture(
        index = 0,
        type = TextureType.COLOR
    )

    val skylandHeightTexture = PreTexture(
        index = 0,
        type = TextureType.HEIGHT
    )

    suspend fun await(): MainMenuModels {
        return MainMenuModels(
            skyland = this.skyland.await(),
            skylandColorTexture = this.skylandColorTexture.await(),
            skylandHeightTexture = this.skylandHeightTexture.await()
        )
    }
}
