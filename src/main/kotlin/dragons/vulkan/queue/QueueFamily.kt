package dragons.vulkan.queue

class QueueFamily(
    val index: Int,
    val priorityQueues: Collection<DeviceQueue>,
    val backgroundQueues: Collection<DeviceQueue>
) {
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

    override fun hashCode(): Int {
        return index
    }
}
