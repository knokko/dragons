package dragons.vulkan.memory

class VulkanBuffer(val handle: Long) {
    override fun toString() = "VulkanBuffer($handle)"
}

class VulkanBufferRange(val buffer: VulkanBuffer, val offset: Long, val size: Long) {
    override fun toString() = "VulkanBufferRange(handle = ${buffer.handle}, offset = $offset, size = $size)"
}
