package dragons.plugins.standard.vulkan.render.entity

import dragons.state.StaticGraphicsState
import dragons.vulkan.memory.VulkanImage

class DummyEntityColorImage(override val width: Int, override val height: Int) : EntityColorImage {
    override fun create(graphicsState: StaticGraphicsState): Pair<VulkanImage, Long> {
        throw UnsupportedOperationException("This method should not be used on a dummy entity image")
    }
}
