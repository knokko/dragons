package dragons.vulkan.queue

import com.github.knokko.boiler.queue.BoilerQueue

class QueueFamily(
    val index: Int,
    val priorityQueues: List<BoilerQueue>,
    val backgroundQueues: List<BoilerQueue>
) {
    fun getFirstPriorityQueue() = priorityQueues[0]

    fun getFirstPriorityQueueIndex() = 0

    fun getRandomBackgroundQueue(): BoilerQueue {
        return if (backgroundQueues.isEmpty()) {
            getRandomPriorityQueue()
        } else {
            backgroundQueues.random()
        }
    }

    fun getRandomPriorityQueue(): BoilerQueue {
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
