package dragons.vulkan.memory.claim

import dragons.vulkan.memory.VulkanBuffer
import dragons.vulkan.memory.VulkanBufferRange
import dragons.vulkan.memory.VulkanImage
import dragons.vulkan.memory.scope.CombinedMemoryScopeClaims
import dragons.vulkan.queue.QueueFamily
import dragons.vulkan.queue.QueueManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.lwjgl.vulkan.VK10.*
import java.nio.ByteBuffer

class TestSharing {

    @Test
    fun testShareMemoryClaims() {
        val queueManager = QueueManager(
            // It's not really allowed to create a queue family without any queues, but this is only a test anyway
            generalQueueFamily = QueueFamily(0, emptyList(), emptyList()),
            computeOnlyQueueFamily = QueueFamily(2, emptyList(), emptyList()),
            transferOnlyQueueFamily = QueueFamily(1, emptyList(), emptyList())
        )

        val result1 = CompletableDeferred<VulkanBufferRange>()
        val result2 = CompletableDeferred<VulkanBufferRange>()
        val result3 = CompletableDeferred<VulkanBufferRange>()
        val result4 = CompletableDeferred<VulkanBufferRange>()

        val imageResult1 = CompletableDeferred<VulkanImage>()
        val imageResult2 = CompletableDeferred<VulkanImage>()
        val imageResult3 = CompletableDeferred<VulkanImage>()
        val imageResult4 = CompletableDeferred<VulkanImage>()

        val simplePrefill: (ByteBuffer) -> Unit = { dest ->
            dest.putInt(0, 12345)
        }

        fun createImageClaim(
            width: Int, height: Int, queueFamily: QueueFamily?,
            sharingID: String?, storeResult: CompletableDeferred<VulkanImage>
        ) = ImageMemoryClaim(
            width = width, height = height, queueFamily = queueFamily, bytesPerPixel = 4,
            imageFormat = VK_FORMAT_R8G8B8A8_SRGB, tiling = VK_IMAGE_TILING_OPTIMAL, imageUsage = VK_IMAGE_USAGE_STORAGE_BIT,
            initialLayout = VK_IMAGE_LAYOUT_UNDEFINED, aspectMask = VK_IMAGE_ASPECT_COLOR_BIT, storeResult = storeResult,
            sharingID = sharingID, prefill = null
        )

        val originalClaimsMap = mutableMapOf(
            Pair(queueManager.generalQueueFamily, QueueFamilyClaims(CombinedMemoryScopeClaims(allBufferClaims = listOf(
                BufferMemoryClaim(
                    size = 100, usageFlags = 1, queueFamily = queueManager.generalQueueFamily,
                    storeResult = result1, prefill = null, sharingID = "share1"
                ), BufferMemoryClaim(
                    size = 100, usageFlags = 2, queueFamily = queueManager.generalQueueFamily,
                    storeResult = result2, prefill = null, sharingID = null
                ), BufferMemoryClaim(
                    size = 100, usageFlags = 3, queueFamily = queueManager.generalQueueFamily,
                    storeResult = result3, prefill = simplePrefill,
                    dstAccessMask = 1, dstPipelineStageMask = 2, sharingID = "share2"
                ), BufferMemoryClaim(
                    size = 100, usageFlags = 3, queueFamily = queueManager.generalQueueFamily,
                    storeResult = result4, prefill = simplePrefill,
                    dstAccessMask = 1, dstPipelineStageMask = 2, sharingID = "share2"
                )
            ), stagingBufferClaims = emptyList(), allImageClaims = listOf(
                createImageClaim(2, 2, queueManager.generalQueueFamily, "share2", imageResult1),
                createImageClaim(5, 6, queueManager.generalQueueFamily, null, imageResult3),
                createImageClaim(2, 2, queueManager.generalQueueFamily, "share2", imageResult4)
            )))),
            Pair(null, QueueFamilyClaims(CombinedMemoryScopeClaims(
                allImageClaims = listOf(
                    createImageClaim(1, 1, null, "share1", imageResult2),
                ),
                stagingBufferClaims = emptyList(),
                allBufferClaims = emptyList()
            )))
        )

        val sharedClaimsMap = shareMemoryClaims(originalClaimsMap)
        assertEquals(2, sharedClaimsMap.size)

        assertEquals(0, sharedClaimsMap[null]!!.claims.allBufferClaims.size)
        assertEquals(0, sharedClaimsMap[null]!!.claims.stagingBufferClaims.size)
        assertEquals(1, sharedClaimsMap[null]!!.claims.allImageClaims.size)

        run {
            val sharedBufferClaims = sharedClaimsMap[queueManager.generalQueueFamily]!!.claims.allBufferClaims
            assertEquals(3, sharedBufferClaims.size)

            val unshared = sharedBufferClaims.find { it.sharingID == null }!!
            val shared1 = sharedBufferClaims.find { it.sharingID == "share1" }!!
            val shared2 = sharedBufferClaims.find { it.sharingID == "share2" }!!
            assertEquals(2, unshared.usageFlags)
            assertEquals(1, shared1.usageFlags)
            assertEquals(3, shared2.usageFlags)

            assertFalse(result1.isCompleted)
            assertFalse(result2.isCompleted)
            assertFalse(result3.isCompleted)
            assertFalse(result4.isCompleted)

            shared1.storeResult.complete(VulkanBufferRange(buffer = VulkanBuffer(1), offset = 2, size = 3))
            assertTrue(result1.isCompleted)
            assertFalse(result2.isCompleted)
            unshared.storeResult.complete(VulkanBufferRange(buffer = VulkanBuffer(4), offset = 5, size = 6))
            assertTrue(result2.isCompleted)
            assertFalse(result3.isCompleted)

            shared2.storeResult.complete(VulkanBufferRange(buffer = VulkanBuffer(7), offset = 8, size = 9))
            assertTrue(result3.isCompleted)
            assertTrue(result4.isCompleted)

            runBlocking {
                assertEquals(3, result1.await().size)
                assertEquals(6, result2.await().size)
                assertEquals(9, result3.await().size)
                assertEquals(9, result4.await().size)
            }
        }

        run {
            val generalImageClaims = sharedClaimsMap[queueManager.generalQueueFamily]!!.claims.allImageClaims
            assertEquals(2, generalImageClaims.size)

            val nullImageClaims = sharedClaimsMap[null]!!.claims.allImageClaims
            assertEquals(1, nullImageClaims.size)

            val unshared = generalImageClaims.find { it.sharingID == null }!!
            val shared1 = nullImageClaims.find { it.sharingID == "share1" }!!
            val shared2 = generalImageClaims.find { it.sharingID == "share2" }!!
            assertEquals(5, unshared.width)
            assertEquals(1, shared1.width)
            assertEquals(2, shared2.width)

            assertFalse(imageResult1.isCompleted)
            assertFalse(imageResult2.isCompleted)
            assertFalse(imageResult3.isCompleted)
            assertFalse(imageResult4.isCompleted)

            shared1.storeResult.complete(VulkanImage(1, 2, 3))
            assertTrue(imageResult2.isCompleted)
            assertFalse(imageResult1.isCompleted)
            unshared.storeResult.complete(VulkanImage(4, 5, 6))
            assertTrue(imageResult3.isCompleted)
            assertFalse(imageResult4.isCompleted)

            shared2.storeResult.complete(VulkanImage(7, 8, 9))
            assertTrue(imageResult1.isCompleted)
            assertTrue(imageResult4.isCompleted)

            runBlocking {
                assertEquals(8, imageResult1.await().width)
                assertEquals(2, imageResult2.await().width)
                assertEquals(5, imageResult3.await().width)
                assertEquals(8, imageResult4.await().width)
            }
        }
    }

    @Test
    fun testErrorUponSharingUnequalMemoryClaims() {
        val queueManager = QueueManager(
            // It's not really allowed to create a queue family without any queues, but this is only a test anyway
            generalQueueFamily = QueueFamily(0, emptyList(), emptyList()),
            computeOnlyQueueFamily = QueueFamily(2, emptyList(), emptyList()),
            transferOnlyQueueFamily = QueueFamily(1, emptyList(), emptyList())
        )

        val result1 = CompletableDeferred<VulkanBufferRange>()
        val result2 = CompletableDeferred<VulkanBufferRange>()

        val originalClaimsMap = mutableMapOf<QueueFamily?, QueueFamilyClaims>(
            Pair(queueManager.generalQueueFamily, QueueFamilyClaims(CombinedMemoryScopeClaims(allBufferClaims = listOf(
                BufferMemoryClaim(
                    size = 100, usageFlags = 1, queueFamily = queueManager.generalQueueFamily,
                    storeResult = result1, prefill = null, sharingID = "bad share"
                ), BufferMemoryClaim(
                    size = 200, usageFlags = 2, queueFamily = queueManager.generalQueueFamily,
                    storeResult = result2, prefill = null, sharingID = "bad share"
                )
            ), stagingBufferClaims = emptyList(), allImageClaims = emptyList())))
        )

        assertThrows(IllegalArgumentException::class.java) {
            shareMemoryClaims(originalClaimsMap)
        }
    }
}
