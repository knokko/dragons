package dragons.plugins.standard.vulkan.texture

import dragons.vulkan.memory.VulkanImage

class Texture(
    val image: VulkanImage,
    val index: Int,
    val type: TextureType
) {
}
