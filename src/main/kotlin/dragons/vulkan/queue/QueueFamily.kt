package dragons.vulkan.queue

class QueueFamily(
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
}
