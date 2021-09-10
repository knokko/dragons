package dragons.vulkan.memory.claim

import dragons.plugin.interfaces.vulkan.VulkanStaticMemoryUser
import dragons.vulkan.queue.QueueFamily
import dragons.vulkan.queue.QueueManager
import kotlinx.coroutines.CompletableDeferred
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
                prefilledBuffers = mutableListOf(PrefilledBufferMemoryClaim(100, 0, queueManager.generalQueueFamily, CompletableDeferred()){})
            ),
            VulkanStaticMemoryUser.Agent(
                queueManager = queueManager,
                uninitializedBuffers = mutableListOf(
                    UninitializedBufferMemoryClaim(100, 0, queueManager.generalQueueFamily, CompletableDeferred()),
                    UninitializedBufferMemoryClaim(100, 0, queueManager.computeOnlyQueueFamily!!, CompletableDeferred())
                ),
                stagingBuffers = mutableListOf(StagingBufferMemoryClaim(200, null, CompletableDeferred()))
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

        val prefillBuffer1 = PrefilledBufferMemoryClaim(100, 0, queueManager.generalQueueFamily, CompletableDeferred()){}
        val prefillBuffer2 = PrefilledBufferMemoryClaim(200, 1, queueManager.generalQueueFamily, CompletableDeferred()){}
        val prefillBuffer3 = PrefilledBufferMemoryClaim(300, 2, queueManager.computeOnlyQueueFamily, CompletableDeferred()){}

        val uninitBuffer1 = UninitializedBufferMemoryClaim(400, 4, null, CompletableDeferred())
        val uninitBuffer2 = UninitializedBufferMemoryClaim(500, 8, queueManager.computeOnlyQueueFamily, CompletableDeferred())
        val uninitBuffer3 = UninitializedBufferMemoryClaim(600, 13, queueManager.generalQueueFamily, CompletableDeferred())

        val stagingBuffer1 = StagingBufferMemoryClaim(700, null, CompletableDeferred())
        val stagingBuffer2 = StagingBufferMemoryClaim(800, queueManager.computeOnlyQueueFamily, CompletableDeferred())
        val stagingBuffer3 = StagingBufferMemoryClaim(900, queueManager.computeOnlyQueueFamily, CompletableDeferred())

        val agents = listOf(
            VulkanStaticMemoryUser.Agent(
                queueManager = queueManager,
                prefilledBuffers = mutableListOf(prefillBuffer1, prefillBuffer2),
                uninitializedBuffers = mutableListOf(uninitBuffer1),
                stagingBuffers = mutableListOf(stagingBuffer1, stagingBuffer3)
            ),
            VulkanStaticMemoryUser.Agent(
                queueManager = queueManager,
                prefilledBuffers = mutableListOf(prefillBuffer3),
                uninitializedBuffers = mutableListOf(uninitBuffer2, uninitBuffer3),
                stagingBuffers = mutableListOf(stagingBuffer2)
            )
        )

        val expectedGrouping = mapOf(
            Pair(null, QueueFamilyClaims(
                prefilledBufferClaims = emptyList(),
                uninitializedBufferClaims = listOf(uninitBuffer1),
                stagingBufferClaims = listOf(stagingBuffer1)
            )),
            Pair(queueManager.generalQueueFamily, QueueFamilyClaims(
                prefilledBufferClaims = listOf(prefillBuffer1, prefillBuffer2),
                uninitializedBufferClaims = listOf(uninitBuffer3),
                stagingBufferClaims = emptyList()
            )),
            Pair(queueManager.computeOnlyQueueFamily, QueueFamilyClaims(
                prefilledBufferClaims = listOf(prefillBuffer3),
                uninitializedBufferClaims = listOf(uninitBuffer2),
                stagingBufferClaims = listOf(stagingBuffer3, stagingBuffer2)
            ))
        )

        assertEquals(expectedGrouping, groupMemoryClaims(agents))
    }

    @Test
    fun testPlaceMemoryClaims() {
        // TODO Also test for images
        val prefill1 = PrefilledBufferMemoryClaim(100, 0, null, CompletableDeferred()) {}
        val prefill2 = PrefilledBufferMemoryClaim(200, 0, null, CompletableDeferred()) {}

        val uninit1 = UninitializedBufferMemoryClaim(300, 0, null, CompletableDeferred())
        val uninit2 = UninitializedBufferMemoryClaim(400, 0, null, CompletableDeferred())

        val staging1 = StagingBufferMemoryClaim(500, null, CompletableDeferred())
        val staging2 = StagingBufferMemoryClaim(600, null, CompletableDeferred())

        val claims = QueueFamilyClaims(listOf(prefill1, prefill2), listOf(uninit1, uninit2), listOf(staging1, staging2))

        val expectedPlacements = PlacedQueueFamilyClaims(
            prefilledBufferClaims = listOf(Placed(prefill1, 0), Placed(prefill2, 100)),
            prefilledBufferStagingOffset = 0,
            prefilledBufferDeviceOffset = 0,
            uninitializedBufferClaims = listOf(Placed(uninit1, 0), Placed(uninit2, 300)),
            uninitializedBufferDeviceOffset = 300,
            stagingBufferClaims = listOf(Placed(staging1, 0), Placed(staging2, 500)),
            stagingBufferOffset = 0
        )

        assertEquals(expectedPlacements, placeMemoryClaims(claims))
    }
}
