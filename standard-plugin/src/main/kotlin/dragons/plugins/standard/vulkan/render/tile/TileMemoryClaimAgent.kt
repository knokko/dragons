package dragons.plugins.standard.vulkan.render.tile

import dragons.state.StaticGameState
import dragons.vulkan.memory.VulkanImage
import dragons.vulkan.memory.scope.MemoryScopeClaims
import kotlinx.coroutines.CompletableDeferred

class TileMemoryClaimAgent(
    val gameState: StaticGameState,
    val claims: MemoryScopeClaims,
    private val colorImages: MutableList<CompletableDeferred<VulkanImage>>,
    private val heightImages: MutableList<CompletableDeferred<VulkanImage>>
) {
    private fun claimImageIndex(
        images: MutableList<CompletableDeferred<VulkanImage>>,
        image: CompletableDeferred<VulkanImage>
    ): Int {
        val index = images.size
        images.add(image)
        return index
    }

    fun claimColorImageIndex(image: CompletableDeferred<VulkanImage>) = claimImageIndex(colorImages, image)

    fun claimHeightImageIndex(image: CompletableDeferred<VulkanImage>) = claimImageIndex(heightImages, image)
}
