package dragons.vulkan.queue

class QueueManager(
    val generalQueueFamily: QueueFamily,
    val computeOnlyQueueFamily: QueueFamily?,
    val transferOnlyQueueFamily: QueueFamily?
) {
    fun getComputeQueueFamily(): QueueFamily {
        return computeOnlyQueueFamily ?: generalQueueFamily
    }

    fun getTransferQueueFamily(): QueueFamily {
        return transferOnlyQueueFamily ?: getComputeQueueFamily()
    }
}
