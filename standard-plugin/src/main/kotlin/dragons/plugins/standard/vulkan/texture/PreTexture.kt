package dragons.plugins.standard.vulkan.texture

import dragons.vulkan.memory.VulkanImage
import kotlinx.coroutines.CompletableDeferred

class PreTexture(
    val image: CompletableDeferred<VulkanImage> = CompletableDeferred(),
    val index: Int,
    val type: TextureType
) {
    suspend fun await(): Texture {
        return Texture(
            image = this.image.await(),
            index = this.index,
            type = this.type
        )
    }
}
