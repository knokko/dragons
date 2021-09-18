package dragons.vulkan.memory.claim

import dragons.plugin.interfaces.vulkan.VulkanStaticMemoryUser
import dragons.vulkan.queue.QueueFamily
import dragons.vulkan.queue.QueueManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class TestGrouping {

    @Test
    fun testGetUsedQueueFamiliesBuffer() {
        runBlocking {
            val queueManager = QueueManager(
                // It's not really allowed to create a queue family without any queues, but this is only a test anyway
                generalQueueFamily = QueueFamily(0, emptyList(), emptyList()),
                computeOnlyQueueFamily = QueueFamily(2, emptyList(), emptyList()),
                transferOnlyQueueFamily = QueueFamily(1, emptyList(), emptyList())
            )

            val agents = listOf(
                VulkanStaticMemoryUser.Agent(
                    queueManager = queueManager, gameInitScope = this,
                            prefilledBuffers = mutableListOf (PrefilledBufferMemoryClaim(
                        100,
                        0,
                        queueManager.generalQueueFamily,
                        CompletableDeferred()
                    ) {})
                ),
                VulkanStaticMemoryUser.Agent(
                    queueManager = queueManager, gameInitScope = this,
                    uninitializedBuffers = mutableListOf(
                        UninitializedBufferMemoryClaim(100, 0, queueManager.generalQueueFamily, CompletableDeferred()),
                        UninitializedBufferMemoryClaim(
                            100,
                            0,
                            queueManager.computeOnlyQueueFamily!!,
                            CompletableDeferred()
                        )
                    ),
                    stagingBuffers = mutableListOf(StagingBufferMemoryClaim(200, null, CompletableDeferred()))
                )
            )

            assertEquals(
                setOf(null, queueManager.generalQueueFamily, queueManager.computeOnlyQueueFamily),
                getUsedQueueFamilies(agents)
            )
        }
    }

    @Test
    fun testGetUsedQueueFamiliesImage() {
        val queueManager = QueueManager(
            // It's not really allowed to create a queue family without any queues, but this is only a test anyway
            generalQueueFamily = QueueFamily(0, emptyList(), emptyList()),
            computeOnlyQueueFamily = QueueFamily(2, emptyList(), emptyList()),
            transferOnlyQueueFamily = QueueFamily(1, emptyList(), emptyList())
        )

        runBlocking {
            val agents = listOf(
                VulkanStaticMemoryUser.Agent(
                    queueManager = queueManager, gameInitScope = this,
                    prefilledImages = mutableListOf(PrefilledImageMemoryClaim(
                        100, 50, 4, queueManager.generalQueueFamily, CompletableDeferred()
                    ) { _: ByteBuffer -> }
                    )
                ),
                VulkanStaticMemoryUser.Agent(
                    queueManager = queueManager, gameInitScope = this,
                    uninitializedImages = mutableListOf(
                        UninitializedImageMemoryClaim(
                            100,
                            100,
                            1,
                            queueManager.generalQueueFamily,
                            CompletableDeferred()
                        ),
                        UninitializedImageMemoryClaim(
                            100,
                            200,
                            3,
                            queueManager.computeOnlyQueueFamily!!,
                            CompletableDeferred()
                        )
                    ),
                )
            )

            assertEquals(
                setOf(queueManager.generalQueueFamily, queueManager.computeOnlyQueueFamily),
                getUsedQueueFamilies(agents)
            )
        }
    }

    @Test
    fun testGroupMemoryClaims() {
        val queueManager = QueueManager(
            // It's not really allowed to create a queue family without any queues, but this is only a test anyway
            generalQueueFamily = QueueFamily(0, emptyList(), emptyList()),
            computeOnlyQueueFamily = QueueFamily(2, emptyList(), emptyList()),
            transferOnlyQueueFamily = QueueFamily(1, emptyList(), emptyList())
        )

        val prefillBuffer1 = PrefilledBufferMemoryClaim(100, 0, queueManager.generalQueueFamily, CompletableDeferred()){}
        val prefillBuffer2 = PrefilledBufferMemoryClaim(200, 1, queueManager.generalQueueFamily, CompletableDeferred()){}
        val prefillBuffer3 = PrefilledBufferMemoryClaim(300, 2, queueManager.computeOnlyQueueFamily, CompletableDeferred()){}

        val uninitBuffer1 = UninitializedBufferMemoryClaim(400, 4, null, CompletableDeferred())
        val uninitBuffer2 = UninitializedBufferMemoryClaim(500, 8, queueManager.computeOnlyQueueFamily, CompletableDeferred())
        val uninitBuffer3 = UninitializedBufferMemoryClaim(600, 13, queueManager.generalQueueFamily, CompletableDeferred())

        val stagingBuffer1 = StagingBufferMemoryClaim(700, null, CompletableDeferred())
        val stagingBuffer2 = StagingBufferMemoryClaim(800, queueManager.computeOnlyQueueFamily, CompletableDeferred())
        val stagingBuffer3 = StagingBufferMemoryClaim(900, queueManager.computeOnlyQueueFamily, CompletableDeferred())

        val prefillImage1 = PrefilledImageMemoryClaim(100, 100, 4, queueManager.computeOnlyQueueFamily, CompletableDeferred()){}
        val prefillImage2 = PrefilledImageMemoryClaim(150, 200, 3, queueManager.transferOnlyQueueFamily, CompletableDeferred()){}
        val prefillImage3 = PrefilledImageMemoryClaim(350, 400, 1, queueManager.transferOnlyQueueFamily, CompletableDeferred()){}

        val uninitImage1 = UninitializedImageMemoryClaim(10, 20, 4, queueManager.generalQueueFamily, CompletableDeferred())
        val uninitImage2 = UninitializedImageMemoryClaim(20, 30, 1, queueManager.computeOnlyQueueFamily, CompletableDeferred())
        val uninitImage3 = UninitializedImageMemoryClaim(20, 40, 1, null, CompletableDeferred())

        runBlocking {
            val agents = listOf(
                VulkanStaticMemoryUser.Agent(
                    queueManager = queueManager, gameInitScope = this,
                    prefilledBuffers = mutableListOf(prefillBuffer1, prefillBuffer2),
                    uninitializedBuffers = mutableListOf(uninitBuffer1),
                    stagingBuffers = mutableListOf(stagingBuffer1, stagingBuffer3),
                    prefilledImages = mutableListOf(prefillImage1),
                    uninitializedImages = mutableListOf(uninitImage2, uninitImage3)
                ),
                VulkanStaticMemoryUser.Agent(
                    queueManager = queueManager, gameInitScope = this,
                    prefilledBuffers = mutableListOf(prefillBuffer3),
                    uninitializedBuffers = mutableListOf(uninitBuffer2, uninitBuffer3),
                    stagingBuffers = mutableListOf(stagingBuffer2),
                    prefilledImages = mutableListOf(prefillImage2, prefillImage3),
                    uninitializedImages = mutableListOf(uninitImage1)
                )
            )

            val expectedGrouping = mapOf(
                Pair(
                    null, QueueFamilyClaims(
                        prefilledBufferClaims = emptyList(),
                        uninitializedBufferClaims = listOf(uninitBuffer1),
                        stagingBufferClaims = listOf(stagingBuffer1),
                        prefilledImageClaims = emptyList(),
                        uninitializedImageClaims = listOf(uninitImage3)
                    )
                ),
                Pair(
                    queueManager.generalQueueFamily, QueueFamilyClaims(
                        prefilledBufferClaims = listOf(prefillBuffer1, prefillBuffer2),
                        uninitializedBufferClaims = listOf(uninitBuffer3),
                        stagingBufferClaims = emptyList(),
                        prefilledImageClaims = emptyList(),
                        uninitializedImageClaims = listOf(uninitImage1)
                    )
                ),
                Pair(
                    queueManager.computeOnlyQueueFamily, QueueFamilyClaims(
                        prefilledBufferClaims = listOf(prefillBuffer3),
                        uninitializedBufferClaims = listOf(uninitBuffer2),
                        stagingBufferClaims = listOf(stagingBuffer3, stagingBuffer2),
                        prefilledImageClaims = listOf(prefillImage1),
                        uninitializedImageClaims = listOf(uninitImage2)
                    )
                ),
                Pair(
                    queueManager.transferOnlyQueueFamily, QueueFamilyClaims(
                        prefilledBufferClaims = emptyList(),
                        uninitializedBufferClaims = emptyList(),
                        stagingBufferClaims = emptyList(),
                        prefilledImageClaims = listOf(prefillImage2, prefillImage3),
                        uninitializedImageClaims = emptyList()
                    )
                )
            )

            assertEquals(expectedGrouping, groupMemoryClaims(agents))
        }
    }

    @Test
    fun testPlaceMemoryClaims() {
        val prefillBuffer1 = PrefilledBufferMemoryClaim(100, 0, null, CompletableDeferred()) {}
        val prefillBuffer2 = PrefilledBufferMemoryClaim(200, 0, null, CompletableDeferred()) {}

        val uninitBuffer1 = UninitializedBufferMemoryClaim(300, 0, null, CompletableDeferred())
        val uninitBuffer2 = UninitializedBufferMemoryClaim(400, 0, null, CompletableDeferred())

        val staging1 = StagingBufferMemoryClaim(500, null, CompletableDeferred())
        val staging2 = StagingBufferMemoryClaim(600, null, CompletableDeferred())

        val prefillImage1 = PrefilledImageMemoryClaim(100, 200, 1, null, CompletableDeferred()){}
        val prefillImage2 = PrefilledImageMemoryClaim(200, 300, 4, null, CompletableDeferred()){}

        val uninitImage1 = UninitializedImageMemoryClaim(400, 300, 3, null, CompletableDeferred())
        val uninitImage2 = UninitializedImageMemoryClaim(400, 400, 4, null, CompletableDeferred())

        val claims = QueueFamilyClaims(
            listOf(prefillBuffer1, prefillBuffer2),
            listOf(uninitBuffer1, uninitBuffer2),
            listOf(staging1, staging2),
            listOf(prefillImage1, prefillImage2),
            listOf(uninitImage1, uninitImage2)
        )

        val expectedPlacements = PlacedQueueFamilyClaims(
            prefilledBufferClaims = listOf(Placed(prefillBuffer1, 0), Placed(prefillBuffer2, 100)),
            prefilledBufferStagingOffset = 0,
            prefilledBufferDeviceOffset = 0,
            uninitializedBufferClaims = listOf(Placed(uninitBuffer1, 0), Placed(uninitBuffer2, 300)),
            uninitializedBufferDeviceOffset = 300,
            stagingBufferClaims = listOf(Placed(staging1, 0), Placed(staging2, 500)),
            stagingBufferOffset = 0,
            prefilledImageClaims = listOf(Placed(prefillImage1, 0), Placed(prefillImage2, 20_000)),
            prefilledImageStagingOffset = 300,
            uninitializedImageClaims = listOf(uninitImage1, uninitImage2),
        )

        assertEquals(expectedPlacements, placeMemoryClaims(claims))
    }
}
