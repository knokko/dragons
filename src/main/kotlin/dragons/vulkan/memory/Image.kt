package dragons.vulkan.memory

class VulkanImage(
    /** The handle of the `VkImage` */
    val handle: Long,
    /** A `VkImageView` covering the entire image */
    val fullView: Long
)
