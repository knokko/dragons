package dragons.plugins.standard.vulkan.texture

import dragons.vulkan.memory.VulkanImage

class TextureSet(
    val colorTextureList: List<VulkanImage?>,
    val heightTextureList: List<VulkanImage?>
)
