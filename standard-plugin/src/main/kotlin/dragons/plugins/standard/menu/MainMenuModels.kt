package dragons.plugins.standard.menu

import dragons.plugins.standard.vulkan.model.Model
import dragons.plugins.standard.vulkan.model.ModelSet
import dragons.plugins.standard.vulkan.pipeline.MAX_NUM_DESCRIPTOR_IMAGES
import dragons.plugins.standard.vulkan.texture.Texture
import dragons.plugins.standard.vulkan.texture.PreTextureSet
import dragons.plugins.standard.vulkan.texture.TextureSet

class MainMenuModels(
    val textureSet: TextureSet,
    val skyland: Model,
    val flower1: Model
    ) {

    val modelSet: ModelSet = ModelSet(
        listOf(
            skyland
        )
    )
}
