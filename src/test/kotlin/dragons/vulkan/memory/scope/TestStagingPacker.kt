package dragons.vulkan.memory.scope

import dragons.vulkan.memory.claim.*
import kotlinx.coroutines.CompletableDeferred
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.lwjgl.vulkan.VK12.*

class TestStagingPacker {

    @Test
    fun testDetermineStagingPlacements() {
        val prefillBuffer1 = BufferMemoryClaim(
            100, 0, 5, 5, null, CompletableDeferred()
        ) {}
        val prefillBuffer2 = BufferMemoryClaim(
            200, 0, 6, 6, null, CompletableDeferred()
        ) {}

        val uninitBuffer1 = BufferMemoryClaim(
            300, 0, 0, 0, null, CompletableDeferred(), null
        )
        val uninitBuffer2 = BufferMemoryClaim(
            400, 0, 0, 0, null, CompletableDeferred(), null
        )

        val staging1 = StagingBufferMemoryClaim(500, null, 0, CompletableDeferred())
        val staging2 = StagingBufferMemoryClaim(600, null, 0, CompletableDeferred())

        fun createImageClaim(width: Int, height: Int, bytesPerPixel: Int, prefill: Boolean): ImageMemoryClaim {
            return ImageMemoryClaim(
                width, height, null,
                0, 0, bytesPerPixel,
                VK_FORMAT_R16G16_UNORM, VK_IMAGE_TILING_LINEAR, VK_SAMPLE_COUNT_4_BIT,
                VK_IMAGE_USAGE_INPUT_ATTACHMENT_BIT, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                VK_IMAGE_ASPECT_COLOR_BIT, VK_ACCESS_SHADER_READ_BIT, VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT,
                CompletableDeferred(), if (prefill) {{}} else { null }
            )
        }

        val prefillImage1 = createImageClaim(100, 200, 1, true)
        val prefillImage2 = createImageClaim(200, 300, 4, true)

        val uninitImage1 = createImageClaim(400, 300, 3, false)
        val uninitImage2 = createImageClaim(400, 400, 4, false)

        val claims = QueueFamilyClaims(CombinedMemoryScopeClaims(
            allBufferClaims = listOf(prefillBuffer1, prefillBuffer2, uninitBuffer1, uninitBuffer2),
            allImageClaims = listOf(prefillImage1, prefillImage2, uninitImage1, uninitImage2),
            stagingBufferClaims = listOf(staging1, staging2)
        ))

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

        assertEquals(expectedPlacements, determineStagingPlacements(
            mapOf(Pair(null, claims))).queueFamilies[null]!!.internalPlacements
        )
    }
}