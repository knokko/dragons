package dragons.vulkan.queue

class QueueManager(
    val generalQueueFamily: QueueFamily,
    val computeOnlyQueueFamily: QueueFamily?,
    val transferOnlyQueueFamily: QueueFamily?
) {

    val relevantQueueFamilies: List<Int>

    init {
        val collectQueueFamilies = mutableListOf(generalQueueFamily.index)
        if (computeOnlyQueueFamily != null) {
            collectQueueFamilies.add(computeOnlyQueueFamily.index)
        }
        if (transferOnlyQueueFamily != null) {
            collectQueueFamilies.add(transferOnlyQueueFamily.index)
        }

        relevantQueueFamilies = collectQueueFamilies.toList()
    }

    fun getComputeQueueFamily(): QueueFamily {
        return computeOnlyQueueFamily ?: generalQueueFamily
    }

    fun getTransferQueueFamily(): QueueFamily {
        return transferOnlyQueueFamily ?: getComputeQueueFamily()
    }
}
