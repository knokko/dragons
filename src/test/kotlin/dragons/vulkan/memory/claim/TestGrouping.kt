package dragons.vulkan.memory.claim

import dragons.plugin.interfaces.vulkan.VulkanStaticMemoryUser
import dragons.vulkan.queue.QueueFamily
import dragons.vulkan.queue.QueueManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestGrouping {

    @Test
    fun testGetUsedQueueFamilies() {
        val queueManager = QueueManager(
            // It's not really allowed to create a queue family without any queues, but this is only a test anyway
            generalQueueFamily = QueueFamily(0, emptyList(), emptyList()),
            computeOnlyQueueFamily = QueueFamily(2, emptyList(), emptyList()),
            transferOnlyQueueFamily = QueueFamily(1, emptyList(), emptyList())
        )

        // TODO Also test the image claims
        val agents = listOf(
            VulkanStaticMemoryUser.Agent(
                queueManager = queueManager,
                prefilledBuffers = mutableListOf(PrefilledBufferMemoryClaim(100, 0, queueManager.generalQueueFamily){})
            ),
            VulkanStaticMemoryUser.Agent(
                queueManager = queueManager,
                uninitializedBuffers = mutableListOf(
                    UninitializedBufferMemoryClaim(100, 0, queueManager.computeOnlyQueueFamily!!),
                    UninitializedBufferMemoryClaim(100, 0, null)
                )
            )
        )

        assertEquals(setOf(null, queueManager.generalQueueFamily, queueManager.computeOnlyQueueFamily), getUsedQueueFamilies(agents))
    }

    @Test
    fun testGroupMemoryClaims() {
        val queueManager = QueueManager(
            // It's not really allowed to create a queue family without any queues, but this is only a test anyway
            generalQueueFamily = QueueFamily(0, emptyList(), emptyList()),
            computeOnlyQueueFamily = QueueFamily(2, emptyList(), emptyList()),
            transferOnlyQueueFamily = QueueFamily(1, emptyList(), emptyList())
        )

        // TODO Also test the image claims

        val prefillBuffer1 = PrefilledBufferMemoryClaim(100, 0, queueManager.generalQueueFamily){}
        val prefillBuffer2 = PrefilledBufferMemoryClaim(200, 1, queueManager.generalQueueFamily){}
        val prefillBuffer3 = PrefilledBufferMemoryClaim(300, 2, queueManager.computeOnlyQueueFamily){}

        val uninitBuffer1 = UninitializedBufferMemoryClaim(400, 4, null)
        val uninitBuffer2 = UninitializedBufferMemoryClaim(500, 8, queueManager.computeOnlyQueueFamily)
        val uninitBuffer3 = UninitializedBufferMemoryClaim(600, 13, queueManager.generalQueueFamily)

        val agents = listOf(
            VulkanStaticMemoryUser.Agent(
                queueManager = queueManager,
                prefilledBuffers = mutableListOf(prefillBuffer1, prefillBuffer2),
                uninitializedBuffers = mutableListOf(uninitBuffer1)
            ),
            VulkanStaticMemoryUser.Agent(
                queueManager = queueManager,
                prefilledBuffers = mutableListOf(prefillBuffer3),
                uninitializedBuffers = mutableListOf(uninitBuffer2, uninitBuffer3)
            )
        )

        val expectedGrouping = mapOf(
            Pair(null, QueueFamilyClaims(
                prefilledBufferClaims = emptyList(),
                uninitializedBufferClaims = listOf(uninitBuffer1)
            )),
            Pair(queueManager.generalQueueFamily, QueueFamilyClaims(
                prefilledBufferClaims = listOf(prefillBuffer1, prefillBuffer2),
                uninitializedBufferClaims = listOf(uninitBuffer3)
            )),
            Pair(queueManager.computeOnlyQueueFamily, QueueFamilyClaims(
                prefilledBufferClaims = listOf(prefillBuffer3),
                uninitializedBufferClaims = listOf(uninitBuffer2)
            ))
        )

        assertEquals(expectedGrouping, groupMemoryClaims(agents))
    }

    @Test
    fun testPlaceMemoryClaims() {
        // TODO Also test for images
        val prefill1 = PrefilledBufferMemoryClaim(100, 0, null) {}
        val prefill2 = PrefilledBufferMemoryClaim(200, 0, null) {}

        val uninit1 = UninitializedBufferMemoryClaim(300, 0, null)
        val uninit2 = UninitializedBufferMemoryClaim(400, 0, null)
        val claims = QueueFamilyClaims(listOf(prefill1, prefill2), listOf(uninit1, uninit2))

        val expectedPlacements = PlacedQueueFamilyClaims(
            prefilledBufferClaims = listOf(Placed(prefill1, 0), Placed(prefill2, 100)),
            prefilledBufferStagingOffset = 0,
            prefilledBufferDeviceOffset = 0,
            uninitializedBufferClaims = listOf(Placed(uninit1, 0), Placed(uninit2, 300)),
            uninitializedBufferDeviceOffset = 300
        )

        assertEquals(expectedPlacements, placeMemoryClaims(claims))
    }
}
