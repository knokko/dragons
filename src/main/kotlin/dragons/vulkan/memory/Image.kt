package dragons.vulkan.memory

class VulkanImage(
    /** The handle of the `VkImage` */
    val handle: Long,

    val width: Int,
    val height: Int
) {
    /**
     * A `VkImageView` covering the entire image.
     * This will be assigned as soon as possible.
     */
    var fullView: Long? = null

    override fun toString() = "VulkanImage(handle = $handle, fullImageViewHandle = $fullView)"
}
