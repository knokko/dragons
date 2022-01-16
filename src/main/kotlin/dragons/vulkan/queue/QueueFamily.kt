package dragons.vulkan.queue

class QueueFamily(
    val index: Int,
    val priorityQueues: List<DeviceQueue>,
    val backgroundQueues: List<DeviceQueue>
) {
    fun getFirstPriorityQueue() = priorityQueues[0]

    fun getFirstPriorityQueueIndex() = 0

    fun getRandomBackgroundQueue(): DeviceQueue {
        return if (backgroundQueues.isEmpty()) {
            getRandomPriorityQueue()
        } else {
            backgroundQueues.random()
        }
    }

    fun getRandomPriorityQueue(): DeviceQueue {
        return priorityQueues.random()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is QueueFamily) {
            index == other.index
        } else {
            false
        }
    }

    override fun hashCode() = index

    override fun toString() = "QueueFamily($index)"
}
