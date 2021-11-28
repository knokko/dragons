package dragons.plugins.standard.vulkan.texture

import dragons.vulkan.memory.VulkanImage
import kotlinx.coroutines.CompletableDeferred

class PreTextureSet(
    textures: Collection<PreTexture>,
    maxNumTextures: Int
) {

    val colorTextureList: List<CompletableDeferred<VulkanImage>?>
    val heightTextureList: List<CompletableDeferred<VulkanImage>?>

    init {
        var numColorTextures = 0
        var numHeightTextures = 0

        for (textureType in TextureType.values()) {
            var nextIndex = 0
            for (texture in textures.filter { it.type == textureType }) {
                if (nextIndex >= maxNumTextures) {
                    throw IllegalArgumentException("Too many textures of type $textureType")
                }
                texture.index = nextIndex
                nextIndex += 1
            }

            if (textureType == TextureType.COLOR) {
                numColorTextures = nextIndex
            }
            if (textureType == TextureType.HEIGHT) {
                numHeightTextures = nextIndex
            }
        }

        val colorTextureArray = Array<CompletableDeferred<VulkanImage>?>(numColorTextures) { null }
        val heightTextureArray = Array<CompletableDeferred<VulkanImage>?>(numHeightTextures) { null }
        for (texture in textures) {
            if (texture.type == TextureType.COLOR) {
                colorTextureArray[texture.index] = texture.image
            }
            if (texture.type == TextureType.HEIGHT) {
                heightTextureArray[texture.index] = texture.image
            }
        }

        this.colorTextureList = colorTextureArray.toList()
        this.heightTextureList = heightTextureArray.toList()
    }

    suspend fun await() = TextureSet(
        colorTextureList = this.colorTextureList.map { it?.await() },
        heightTextureList = this.heightTextureList.map { it?.await() }
    )
}
