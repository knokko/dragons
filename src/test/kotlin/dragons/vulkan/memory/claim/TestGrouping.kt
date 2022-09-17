package dragons.vulkan.memory.claim

import dragons.vulkan.memory.scope.CombinedMemoryScopeClaims
import dragons.vulkan.memory.scope.MemoryScopeClaims
import dragons.vulkan.queue.QueueFamily
import dragons.vulkan.queue.QueueManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.lwjgl.vulkan.VK12.*

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

            val claimsList = listOf(
                MemoryScopeClaims(
                    buffers = mutableListOf(BufferMemoryClaim(
                        size = 100, usageFlags = 0, queueFamily = queueManager.generalQueueFamily,
                        dstAccessMask = 1, dstPipelineStageMask = 1, storeResult = CompletableDeferred()
                    ){})
                ),
                MemoryScopeClaims(
                    buffers = mutableListOf(
                        BufferMemoryClaim(
                            size = 100, usageFlags = 0, queueFamily = queueManager.generalQueueFamily,
                            storeResult = CompletableDeferred(), prefill = null
                        ),
                        BufferMemoryClaim(
                            size = 100, usageFlags = 0, queueFamily = queueManager.computeOnlyQueueFamily!!,
                            storeResult = CompletableDeferred(), prefill = null
                        )
                    ),
                    stagingBuffers = mutableListOf(StagingBufferMemoryClaim(
                        size = 200, alignment = 1, queueFamily = null, storeResult = CompletableDeferred()
                    ))
                )
            )

            assertEquals(
                setOf(null, queueManager.generalQueueFamily, queueManager.computeOnlyQueueFamily),
                getUsedQueueFamilies(claimsList)
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
            val claims = listOf(
                MemoryScopeClaims(
                    images = mutableListOf(
                        ImageMemoryClaim(
                            width = 100, height = 50, queueFamily = queueManager.generalQueueFamily,
                            bytesPerPixel = 4, imageFormat = VK_FORMAT_R8G8B8A8_UINT,
                            tiling = VK_IMAGE_TILING_OPTIMAL, imageUsage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT,
                            initialLayout = VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                            aspectMask = VK_IMAGE_ASPECT_COLOR_BIT, accessMask = VK_ACCESS_SHADER_READ_BIT,
                            dstPipelineStageMask = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                            storeResult = CompletableDeferred()
                        ) { },
                        ImageMemoryClaim(
                            width = 100, height = 100, queueFamily = queueManager.generalQueueFamily,
                            imageFormat = VK_FORMAT_X8_D24_UNORM_PACK32, tiling = VK_IMAGE_TILING_LINEAR,
                            imageUsage = VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT,
                            initialLayout = VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL,
                            aspectMask = VK_IMAGE_ASPECT_DEPTH_BIT, accessMask = VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT,
                            dstPipelineStageMask = VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT,
                            storeResult = CompletableDeferred(), prefill = null
                        ),
                        ImageMemoryClaim(
                            width = 200, height = 300, queueFamily = queueManager.computeOnlyQueueFamily!!,
                            imageFormat = VK_FORMAT_R32_SINT, tiling = VK_IMAGE_TILING_LINEAR,
                            imageUsage = VK_IMAGE_USAGE_STORAGE_BIT, initialLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                            aspectMask = VK_IMAGE_ASPECT_COLOR_BIT, accessMask = VK_ACCESS_COLOR_ATTACHMENT_READ_BIT,
                            dstPipelineStageMask = VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT,
                            storeResult = CompletableDeferred(), prefill = null
                        )
                    )
                )
            )

            assertEquals(
                setOf(queueManager.generalQueueFamily, queueManager.computeOnlyQueueFamily),
                getUsedQueueFamilies(claims)
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

        val prefillBuffer1 = BufferMemoryClaim(
            100, 0, 1, 1, 1, queueManager.generalQueueFamily, CompletableDeferred()
        ){}
        val prefillBuffer2 = BufferMemoryClaim(
            200, 1, 2, 2, 1, queueManager.generalQueueFamily, CompletableDeferred()
        ){}
        val prefillBuffer3 = BufferMemoryClaim(
            300, 2, 3, 3, 1, queueManager.computeOnlyQueueFamily, CompletableDeferred()
        ){}

        val uninitBuffer1 = BufferMemoryClaim(
            400, 4, 0, 0, 1, null, CompletableDeferred(), null, null
        )
        val uninitBuffer2 = BufferMemoryClaim(
            500, 8, 0, 0, 1, queueManager.computeOnlyQueueFamily, CompletableDeferred(), null, null
        )
        val uninitBuffer3 = BufferMemoryClaim(
            600, 13, 0, 0, 1, queueManager.generalQueueFamily, CompletableDeferred(), null, null
        )

        val stagingBuffer1 = StagingBufferMemoryClaim(700, 1, null, 0, CompletableDeferred())
        val stagingBuffer2 = StagingBufferMemoryClaim(800, 2, queueManager.computeOnlyQueueFamily, 0, CompletableDeferred())
        val stagingBuffer3 = StagingBufferMemoryClaim(900, 3, queueManager.computeOnlyQueueFamily, 0, CompletableDeferred())

        fun createImageClaim(width: Int, height: Int, queueFamily: QueueFamily?, prefill: Boolean): ImageMemoryClaim {
            return ImageMemoryClaim(
                width, height, queueFamily,
                0, 0, 4,
                VK_FORMAT_R16G16_UNORM, VK_IMAGE_TILING_LINEAR, VK_SAMPLE_COUNT_4_BIT,
                VK_IMAGE_USAGE_INPUT_ATTACHMENT_BIT, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                VK_IMAGE_ASPECT_COLOR_BIT, VK_ACCESS_HOST_READ_BIT, VK_ACCESS_HOST_READ_BIT, CompletableDeferred(),
                null, if (prefill) {{}} else { null }
            )
        }

        val prefillImage1 = createImageClaim(100, 100, queueManager.computeOnlyQueueFamily!!, true)
        val prefillImage2 = createImageClaim(150, 200, queueManager.transferOnlyQueueFamily!!, true)
        val prefillImage3 = createImageClaim(350, 400, queueManager.transferOnlyQueueFamily!!, true)

        val uninitImage1 = createImageClaim(10, 20, queueManager.generalQueueFamily, false)
        val uninitImage2 = createImageClaim(20, 30, queueManager.computeOnlyQueueFamily!!, false)
        val uninitImage3 = createImageClaim(20, 40, null, false)

        runBlocking {
            val claims = listOf(
                MemoryScopeClaims(
                    buffers = mutableListOf(prefillBuffer1, prefillBuffer2, uninitBuffer1),
                    images = mutableListOf(prefillImage1, uninitImage2, uninitImage3),
                    stagingBuffers = mutableListOf(stagingBuffer1, stagingBuffer3)
                ),
                MemoryScopeClaims(
                    buffers = mutableListOf(prefillBuffer3, uninitBuffer2, uninitBuffer3),
                    images = mutableListOf(prefillImage2, prefillImage3, uninitImage1),
                    stagingBuffers = mutableListOf(stagingBuffer2)
                )
            )

            val expectedGrouping = mapOf(
                Pair(
                    null, QueueFamilyClaims(CombinedMemoryScopeClaims(
                        allBufferClaims = listOf(uninitBuffer1),
                        allImageClaims = listOf(uninitImage3),
                        stagingBufferClaims = listOf(stagingBuffer1)
                    ))
                ),
                Pair(queueManager.generalQueueFamily, QueueFamilyClaims(CombinedMemoryScopeClaims(
                    allBufferClaims = listOf(prefillBuffer1, prefillBuffer2, uninitBuffer3),
                    allImageClaims = listOf(uninitImage1),
                    stagingBufferClaims = emptyList()
                ))),
                Pair(queueManager.computeOnlyQueueFamily, QueueFamilyClaims(CombinedMemoryScopeClaims(
                    allBufferClaims = listOf(prefillBuffer3, uninitBuffer2),
                    allImageClaims = listOf(prefillImage1, uninitImage2),
                    stagingBufferClaims = listOf(stagingBuffer3, stagingBuffer2)
                ))),
                Pair(queueManager.transferOnlyQueueFamily, QueueFamilyClaims(CombinedMemoryScopeClaims(
                    allBufferClaims = emptyList(),
                    allImageClaims = listOf(prefillImage2, prefillImage3),
                    stagingBufferClaims = emptyList()
                ))),
            )

            assertEquals(expectedGrouping, groupMemoryClaims(claims))
        }
    }
}
