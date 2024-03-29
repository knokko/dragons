package dragons.plugins.standard.vulkan.util

import dragons.plugins.standard.vulkan.model.generator.ModelGenerator
import dragons.plugins.standard.vulkan.vertex.BasicVertex
import dragons.vulkan.memory.VulkanBufferRange
import dragons.vulkan.memory.VulkanImage
import dragons.vulkan.memory.claim.BufferMemoryClaim
import dragons.vulkan.memory.claim.ImageMemoryClaim
import dragons.vulkan.memory.claim.prefillBufferedImage
import dragons.vulkan.memory.scope.MemoryScopeClaims
import dragons.vulkan.queue.QueueManager
import kotlinx.coroutines.CompletableDeferred
import org.lwjgl.vulkan.VK10.*
import java.awt.Color
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.nio.ByteBuffer
import javax.imageio.ImageIO

fun claimHeightImage(
    claims: MemoryScopeClaims, queueManager: QueueManager,
    width: Int, height: Int, texture: CompletableDeferred<VulkanImage>,
    sharingID: String?, prefill: (ByteBuffer) -> Unit
) {
    claims.images.add(
        ImageMemoryClaim(
            width = width, height = height, queueFamily = queueManager.generalQueueFamily, bytesPerPixel = 4,
            imageFormat = VK_FORMAT_R32_SFLOAT, tiling = VK_IMAGE_TILING_OPTIMAL,
            imageUsage = VK_IMAGE_USAGE_SAMPLED_BIT, initialLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
            aspectMask = VK_IMAGE_ASPECT_COLOR_BIT, accessMask = VK_ACCESS_SHADER_READ_BIT,
            dstPipelineStageMask = VK_PIPELINE_STAGE_TESSELLATION_EVALUATION_SHADER_BIT or VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
            storeResult = texture, sharingID = if (sharingID == null) { null } else { "$sharingID-height" }, prefill = prefill
        )
    )
}

fun claimHeightImage(
    claims: MemoryScopeClaims, queueManager: QueueManager, pluginClassLoader: ClassLoader,
    width: Int, height: Int, texture: CompletableDeferred<VulkanImage>, resourceName: String, weight: Float,
    sharingID: String?
) {
    claimHeightImage(claims, queueManager, width, height, texture, sharingID) { destBuffer ->
        val bufferedHeightImage = ImageIO.read(
            pluginClassLoader.getResourceAsStream(resourceName)
        )

        for (x in 0 until width) {
            for (y in 0 until height) {
                val destIndex = 4 * (x + y * width)
                val bufferedHeightValue = Color(bufferedHeightImage.getRGB(x, y)).red
                val destHeightValue = weight * (bufferedHeightValue - 127)
                destBuffer.putFloat(destIndex, destHeightValue)
            }
        }
    }
}

fun claimHeightImage(
    claims: MemoryScopeClaims, queueManager: QueueManager,
    width: Int, height: Int, texture: CompletableDeferred<VulkanImage>,
    sharingID: String?, heightFunction: (Int, Int) -> Float
) {
    claimHeightImage(claims, queueManager, width, height, texture, sharingID) { destBuffer ->
        for (x in 0 until width) {
            for (y in 0 until height) {
                val destIndex = 4 * (x + y * width)
                destBuffer.putFloat(destIndex, heightFunction(x, y))
            }
        }
    }
}

fun claimVertexBuffer(
    claims: MemoryScopeClaims, queueManager: QueueManager, vertices: CompletableDeferred<VulkanBufferRange>,
    generator: ModelGenerator, colorImageIndices: List<Int>, heightImageIndices: List<Int>, sharingID: String?
) {
    claims.buffers.add(BufferMemoryClaim(
        size = generator.numVertices * BasicVertex.SIZE,
        alignment = BasicVertex.SIZE,
        usageFlags = VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
        dstAccessMask = VK_ACCESS_VERTEX_ATTRIBUTE_READ_BIT,
        dstPipelineStageMask = VK_PIPELINE_STAGE_VERTEX_INPUT_BIT,
        queueFamily = queueManager.generalQueueFamily,
        storeResult = vertices,
        sharingID = sharingID
    ) { destBuffer ->
        val vertexList = BasicVertex.createList(destBuffer, 0, generator.numVertices.toLong())
        generator.fillVertexBuffer(vertexList, colorImageIndices, heightImageIndices)
    })
}

fun claimIndexBuffer(
    claims: MemoryScopeClaims, queueManager: QueueManager, indices: CompletableDeferred<VulkanBufferRange>,
    generator: ModelGenerator, sharingID: String?
) {
    claims.buffers.add(BufferMemoryClaim(
        size = generator.numIndices * Int.SIZE_BYTES,
        alignment = 4,
        usageFlags = VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
        dstAccessMask = VK_ACCESS_INDEX_READ_BIT,
        dstPipelineStageMask = VK_PIPELINE_STAGE_VERTEX_INPUT_BIT,
        queueFamily = queueManager.generalQueueFamily,
        storeResult = indices,
        sharingID = sharingID
    ) { destBuffer ->
        generator.fillIndexBuffer(destBuffer.asIntBuffer())
    })
}

fun claimVertexAndIndexBuffer(
    claims: MemoryScopeClaims, queueManager: QueueManager, vertices: CompletableDeferred<VulkanBufferRange>,
    indices: CompletableDeferred<VulkanBufferRange>, generator: ModelGenerator,
    colorImageIndices: List<Int>, heightImageIndices: List<Int>, sharingIdPrefix: String?
) {
    claimVertexBuffer(
        claims, queueManager, vertices, generator, colorImageIndices, heightImageIndices,
        if (sharingIdPrefix == null) { null } else { "$sharingIdPrefix-vertices"}
    )
    claimIndexBuffer(claims, queueManager, indices, generator, if (sharingIdPrefix == null) { null} else { "$sharingIdPrefix-indices" })
}

fun claimColorImage(
    claims: MemoryScopeClaims, queueManager: QueueManager, width: Int, height: Int,
    texture: CompletableDeferred<VulkanImage>, sharingID: String?, prefill: (ByteBuffer) -> Unit
) {
    claims.images.add(
        ImageMemoryClaim(
            width = width,
            height = height,
            queueFamily = queueManager.generalQueueFamily,
            bytesPerPixel = 4,
            imageFormat = VK_FORMAT_R8G8B8A8_SRGB,
            tiling = VK_IMAGE_TILING_OPTIMAL,
            imageUsage = VK_IMAGE_USAGE_SAMPLED_BIT,
            initialLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
            aspectMask = VK_IMAGE_ASPECT_COLOR_BIT,
            accessMask = VK_ACCESS_SHADER_READ_BIT,
            dstPipelineStageMask = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
            storeResult = texture,
            sharingID = if (sharingID != null) { "$sharingID-color" } else { null },
            prefill = prefill
        )
    )
}

fun claimColorImage(
    claims: MemoryScopeClaims, queueManager: QueueManager, pluginClassLoader: ClassLoader, width: Int, height: Int,
    texture: CompletableDeferred<VulkanImage>, resourceName: String, sharingID: String?
) {
    claimColorImage(claims, queueManager, width, height, texture, sharingID, prefillBufferedImage(
        { ImageIO.read(pluginClassLoader.getResourceAsStream(resourceName)) },
        width, height, 4
    ))
}

fun claimColorImage(
    claims: MemoryScopeClaims, queueManager: QueueManager, width: Int, height: Int, texture:
    CompletableDeferred<VulkanImage>, sharingID: String?, pixelFunction: (Int, Int) -> Color
) {
    claimColorImage(claims, queueManager, width, height, texture, sharingID, prefillBufferedImage({
        val image = BufferedImage(width, height, TYPE_INT_ARGB)
        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixelColor = pixelFunction(x, y)
                image.setRGB(x, y, pixelColor.rgb)
            }
        }
        image
    }, width, height, 4))
}
