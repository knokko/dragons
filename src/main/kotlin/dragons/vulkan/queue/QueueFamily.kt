package dragons.vulkan.queue

import troll.queue.TrollQueue

class QueueFamily(
    val index: Int,
    val priorityQueues: List<TrollQueue>,
    val backgroundQueues: List<TrollQueue>
) {
    fun getFirstPriorityQueue() = priorityQueues[0]

    fun getFirstPriorityQueueIndex() = 0

    fun getRandomBackgroundQueue(): TrollQueue {
        return if (backgroundQueues.isEmpty()) {
            getRandomPriorityQueue()
        } else {
            backgroundQueues.random()
        }
    }

    fun getRandomPriorityQueue(): TrollQueue {
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
