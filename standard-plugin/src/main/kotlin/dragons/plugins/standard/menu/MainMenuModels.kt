package dragons.plugins.standard.menu

import dragons.plugins.standard.vulkan.model.Model
import dragons.plugins.standard.vulkan.model.ModelSet
import dragons.plugins.standard.vulkan.pipeline.MAX_NUM_DESCRIPTOR_IMAGES
import dragons.plugins.standard.vulkan.texture.Texture
import dragons.plugins.standard.vulkan.texture.TextureSet

class MainMenuModels(
    val skyland: Model,
    val skylandColorTexture: Texture,
    val skylandHeightTexture: Texture
    ) {

    val modelSet: ModelSet = ModelSet(
        listOf(
            skyland
        )
    )

    val textureSet: TextureSet = TextureSet(
        listOf(
            skylandColorTexture,
            skylandHeightTexture
        ),
        MAX_NUM_DESCRIPTOR_IMAGES
    )
}
