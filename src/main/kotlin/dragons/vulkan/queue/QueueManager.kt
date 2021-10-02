package dragons.vulkan.queue

class QueueManager(
    val generalQueueFamily: QueueFamily,
    val computeOnlyQueueFamily: QueueFamily?,
    val transferOnlyQueueFamily: QueueFamily?
) {

    val allQueueFamilies: List<QueueFamily>

    init {
        val collectQueueFamilies = mutableListOf(generalQueueFamily)
        if (computeOnlyQueueFamily != null) {
            collectQueueFamilies.add(computeOnlyQueueFamily)
        }
        if (transferOnlyQueueFamily != null) {
            collectQueueFamilies.add(transferOnlyQueueFamily)
        }

        allQueueFamilies = collectQueueFamilies.toList()
    }

    fun getComputeQueueFamily(): QueueFamily {
        return computeOnlyQueueFamily ?: generalQueueFamily
    }

    fun getTransferQueueFamily(): QueueFamily {
        return transferOnlyQueueFamily ?: getComputeQueueFamily()
    }
}
