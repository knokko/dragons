package dragons.plugins.standard.vulkan.texture

import dragons.vulkan.memory.VulkanImage

class TextureSet(
    textures: Collection<Texture>,
    maxNumTextures: Int
) {

    val colorTextureList: List<VulkanImage?>
    val heightTextureList: List<VulkanImage?>

    init {
        var maxColorIndex = 0
        var maxHeightIndex = 0

        for (textureType in TextureType.values()) {
            val containsID = Array(maxNumTextures) { false }
            for (texture in textures) {
                if (texture.type == textureType) {
                    if (texture.index >= maxNumTextures) {
                        throw IllegalArgumentException("Too big texture index (${texture.index}): maximum is ${maxNumTextures - 1}")
                    }
                    if (texture.index < 0) {
                        throw IllegalArgumentException("Texture has negative index ${texture.index}")
                    }

                    if (textureType == TextureType.COLOR && texture.index > maxColorIndex) {
                        maxColorIndex = texture.index
                    }
                    if (textureType == TextureType.HEIGHT && texture.index > maxHeightIndex) {
                        maxHeightIndex = texture.index
                    }

                    if (containsID[texture.index]) {
                        throw IllegalArgumentException("Multiple textures of type $textureType have index ${texture.index}")
                    }
                    containsID[texture.index] = true
                }
            }
        }

        val colorTextureArray = Array<VulkanImage?>(maxColorIndex + 1) { null }
        val heightTextureArray = Array<VulkanImage?>(maxHeightIndex + 1) { null }
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
}
