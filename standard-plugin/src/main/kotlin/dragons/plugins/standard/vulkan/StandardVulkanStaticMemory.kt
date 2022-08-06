package dragons.plugins.standard.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.vulkan.VulkanStaticMemoryUser
import dragons.plugins.standard.state.StandardPluginState
import dragons.plugins.standard.vulkan.model.PreModel
import dragons.plugins.standard.vulkan.model.generator.*
import dragons.plugins.standard.vulkan.texture.PreTexture
import dragons.plugins.standard.vulkan.vertex.BasicVertex
import dragons.vulkan.memory.claim.BufferMemoryClaim
import dragons.vulkan.memory.claim.ImageMemoryClaim
import dragons.vulkan.memory.claim.StagingBufferMemoryClaim
import dragons.vulkan.memory.claim.prefillBufferedImage
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkDrawIndexedIndirectCommand
import java.awt.Color
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.nio.ByteBuffer
import javax.imageio.ImageIO

const val MAX_NUM_TRANSFORMATION_MATRICES = 100_000
const val MAX_NUM_INDIRECT_DRAW_CALLS = 100_000

class StandardVulkanStaticMemory: VulkanStaticMemoryUser {

    override fun claimStaticMemory(pluginInstance: PluginInstance, agent: VulkanStaticMemoryUser.Agent) {
        val preGraphics = (pluginInstance.state as StandardPluginState).preGraphics

        claimHeightImage(
            agent, 1, 1, preGraphics.mainMenu.zeroHeightTexture
        ) { destBuffer -> destBuffer.putFloat(0, 0f) }

        val mainMenuSkyland = generateSkylandModel({
            0.5f
        }, preGraphics.mainMenu.skyland.colorTextures[0], preGraphics.mainMenu.skyland.heightTextures[0])
        claimVertexAndIndexBuffer(agent, preGraphics.mainMenu.skyland, mainMenuSkyland)
        claimColorImage(
            agent, 1024, 1024, preGraphics.mainMenu.skylandColorTexture,
            "dragons/plugins/standard/images/testTerrain.jpg"
        )
        claimHeightImage(
            agent, 128, 128, preGraphics.mainMenu.skylandHeightTexture,
            "dragons/plugins/standard/images/testTerrainHeight.png", 0.001f
        )

        agent.claims.images.add(
            ImageMemoryClaim(
                width = 4000, height = 5000,
                queueFamily = agent.queueManager.generalQueueFamily,
                bytesPerPixel = 4, imageFormat = VK_FORMAT_R8G8B8A8_UNORM, tiling = VK_IMAGE_TILING_OPTIMAL,
                imageUsage = VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT,
                initialLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                accessMask = VK_ACCESS_SHADER_READ_BIT, aspectMask = VK_IMAGE_ASPECT_COLOR_BIT,
                dstPipelineStageMask = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                storeResult = preGraphics.mainMenu.debugPanelTexture.image, prefill = null
            )
        )
        val debugPanel = generatePanelModel(
            textureIndex = preGraphics.mainMenu.debugPanelTexture.index,
            heightTextureIndex = preGraphics.mainMenu.zeroHeightTexture.index
        )
        claimVertexAndIndexBuffer(agent, preGraphics.mainMenu.debugPanel, debugPanel)

        val mainMenuFlower1 = generateFlowerBushModel(Array(8) { FlowerGenerators.modelProps1(
            stemColorTextureIndex = preGraphics.mainMenu.flowerStem1ColorTexture.index,
            stemHeightTextureIndex = preGraphics.mainMenu.flowerStem1HeightTexture.index,
            topColorTextureIndex = preGraphics.mainMenu.flowerTop1ColorTexture.index,
            topHeightTextureIndex = preGraphics.mainMenu.flowerTop1HeightTexture.index
        ) }.toList())
        claimVertexAndIndexBuffer(agent, preGraphics.mainMenu.flower1, mainMenuFlower1)
        claimColorImage(agent, 32, 128, preGraphics.mainMenu.flowerStem1ColorTexture) { _, _ -> Color.GREEN }
        claimHeightImage(agent, 4, 32, preGraphics.mainMenu.flowerStem1HeightTexture) { _, _ -> 0f }
        claimColorImage(agent, 64, 64, preGraphics.mainMenu.flowerTop1ColorTexture) { _, _ -> Color.YELLOW }
        claimHeightImage(agent, 16, 16, preGraphics.mainMenu.flowerTop1HeightTexture) { _, _ -> 0f }

        val mainMenuFlower2 = generateFlowerBushModel(Array(8) { FlowerGenerators.modelProps2(
            stemColorTextureIndex = preGraphics.mainMenu.flowerStem2ColorTexture.index,
            stemHeightTextureIndex = preGraphics.mainMenu.flowerStem2HeightTexture.index,
            topColorTextureIndex = preGraphics.mainMenu.flowerTop2ColorTexture.index,
            topHeightTextureIndex = preGraphics.mainMenu.flowerTop2HeightTexture.index
        ) }.toList())
        claimVertexAndIndexBuffer(agent, preGraphics.mainMenu.flower2, mainMenuFlower2)
        claimColorImage(agent, 32, 128, preGraphics.mainMenu.flowerStem2ColorTexture) { _, _ -> Color.GREEN }
        claimHeightImage(agent, 4, 32, preGraphics.mainMenu.flowerStem2HeightTexture) { _, _ -> 0f }
        claimColorImage(agent, 64, 64, preGraphics.mainMenu.flowerTop2ColorTexture) { _, _ -> Color.RED}
        claimHeightImage(agent, 16, 16, preGraphics.mainMenu.flowerTop2HeightTexture) { _, _ -> 0f }

        // Camera buffers
        agent.claims.buffers.add(BufferMemoryClaim(
            // 2 4x4 float matrices
            size = 2 * 64,
            usageFlags = VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT or VK_BUFFER_USAGE_TRANSFER_DST_BIT,
            alignment = agent.vkPhysicalDeviceLimits.minUniformBufferOffsetAlignment().toInt(),
            queueFamily = agent.queueManager.generalQueueFamily,
            storeResult = preGraphics.cameraDeviceBuffer,
            prefill = null
        ))
        agent.claims.stagingBuffers.add(StagingBufferMemoryClaim(
            size = 2 * 64,
            queueFamily = agent.queueManager.generalQueueFamily,
            storeResult = preGraphics.cameraStagingBuffer
        ))

        // Transformation matrix buffers
        agent.claims.buffers.add(BufferMemoryClaim(
            size = 4 * 16 * MAX_NUM_TRANSFORMATION_MATRICES,
            usageFlags = VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
            alignment = agent.vkPhysicalDeviceLimits.minStorageBufferOffsetAlignment().toInt(),
            queueFamily = agent.queueManager.generalQueueFamily,
            storeResult = preGraphics.transformationMatrixDeviceBuffer,
            prefill = null
        ))
        agent.claims.stagingBuffers.add(StagingBufferMemoryClaim(
            size = 4 * 16 * MAX_NUM_TRANSFORMATION_MATRICES,
            queueFamily = agent.queueManager.generalQueueFamily,
            storeResult = preGraphics.transformationMatrixStagingBuffer
        ))

        // Indirect drawing buffer
        // TODO Enforce alignment of 4 bytes
        agent.claims.stagingBuffers.add(StagingBufferMemoryClaim(
            size = VkDrawIndexedIndirectCommand.SIZEOF * MAX_NUM_INDIRECT_DRAW_CALLS,
            usageFlags = VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT,
            queueFamily = agent.queueManager.generalQueueFamily,
            storeResult = preGraphics.indirectDrawingBuffer
        ))
        agent.claims.stagingBuffers.add(StagingBufferMemoryClaim(
            size = 4, queueFamily = agent.queueManager.generalQueueFamily,
            // I'm not even sure I need this
            usageFlags = VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT,
            storeResult = preGraphics.indirectDrawCountBuffer
        ))
    }
}

private fun claimVertexBuffer(
    agent: VulkanStaticMemoryUser.Agent, model: PreModel, generator: ModelGenerator
) {
    agent.claims.buffers.add(BufferMemoryClaim(
        size = generator.numVertices * BasicVertex.SIZE,
        alignment = BasicVertex.SIZE,
        usageFlags = VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
        dstAccessMask = VK_ACCESS_VERTEX_ATTRIBUTE_READ_BIT,
        dstPipelineStageMask = VK_PIPELINE_STAGE_VERTEX_INPUT_BIT,
        queueFamily = agent.queueManager.generalQueueFamily,
        storeResult = model.vertices
    ) { destBuffer ->
        val vertices = BasicVertex.createArray(destBuffer, 0, generator.numVertices.toLong())
        generator.fillVertexBuffer(vertices)
    })
}

private fun claimIndexBuffer(
    agent: VulkanStaticMemoryUser.Agent, model: PreModel, generator: ModelGenerator
) {
    agent.claims.buffers.add(BufferMemoryClaim(
        size = generator.numIndices * Int.SIZE_BYTES,
        alignment = 4,
        usageFlags = VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
        dstAccessMask = VK_ACCESS_INDEX_READ_BIT,
        dstPipelineStageMask = VK_PIPELINE_STAGE_VERTEX_INPUT_BIT,
        queueFamily = agent.queueManager.generalQueueFamily,
        storeResult = model.indices
    ) { destBuffer ->
        generator.fillIndexBuffer(destBuffer.asIntBuffer())
    })
}

private fun claimVertexAndIndexBuffer(
    agent: VulkanStaticMemoryUser.Agent, model: PreModel, generator: ModelGenerator
) {
    claimVertexBuffer(agent, model, generator)
    claimIndexBuffer(agent, model, generator)
}

private fun claimColorImage(
    agent: VulkanStaticMemoryUser.Agent, width: Int, height: Int, texture: PreTexture, prefill: (ByteBuffer) -> Unit
) {
    agent.claims.images.add(
        ImageMemoryClaim(
            width = width,
            height = height,
            queueFamily = agent.queueManager.generalQueueFamily,
            bytesPerPixel = 4,
            imageFormat = VK_FORMAT_R8G8B8A8_SRGB,
            tiling = VK_IMAGE_TILING_OPTIMAL,
            imageUsage = VK_IMAGE_USAGE_SAMPLED_BIT,
            initialLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
            aspectMask = VK_IMAGE_ASPECT_COLOR_BIT,
            accessMask = VK_ACCESS_SHADER_READ_BIT,
            dstPipelineStageMask = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
            storeResult = texture.image,
            prefill = prefill
        )
    )
}

private fun claimColorImage(
    agent: VulkanStaticMemoryUser.Agent, width: Int, height: Int, texture: PreTexture, resourceName: String
) {
    claimColorImage(agent, width, height, texture, prefillBufferedImage(
            // TODO Start loading the image asynchronously earlier
            { ImageIO.read(agent.pluginClassLoader.getResourceAsStream(resourceName)) },
            width, height, 4
        )
    )
}

private fun claimColorImage(
    agent: VulkanStaticMemoryUser.Agent, width: Int, height: Int, texture: PreTexture, pixelFunction: (Int, Int) -> Color
) {
    claimColorImage(agent, width, height, texture, prefillBufferedImage({
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

private fun claimHeightImage(
    agent: VulkanStaticMemoryUser.Agent, width: Int, height: Int, texture: PreTexture, prefill: (ByteBuffer) -> Unit
) {
    agent.claims.images.add(ImageMemoryClaim(
        width = width, height = height, queueFamily = agent.queueManager.generalQueueFamily, bytesPerPixel = 4,
        imageFormat = VK_FORMAT_R32_SFLOAT, tiling = VK_IMAGE_TILING_OPTIMAL,
        imageUsage = VK_IMAGE_USAGE_SAMPLED_BIT, initialLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
        aspectMask = VK_IMAGE_ASPECT_COLOR_BIT, accessMask = VK_ACCESS_SHADER_READ_BIT,
        dstPipelineStageMask = VK_PIPELINE_STAGE_TESSELLATION_EVALUATION_SHADER_BIT or VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
        storeResult = texture.image,
        prefill = prefill
    ))
}

private fun claimHeightImage(
    agent: VulkanStaticMemoryUser.Agent, width: Int, height: Int, texture: PreTexture, resourceName: String, weight: Float
) {
    claimHeightImage(agent, width, height, texture) { destBuffer ->
        val bufferedHeightImage = ImageIO.read(
            agent.pluginClassLoader.getResourceAsStream(resourceName)
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

private fun claimHeightImage(
    agent: VulkanStaticMemoryUser.Agent, width: Int, height: Int, texture: PreTexture, heightFunction: (Int, Int) -> Float
) {
    claimHeightImage(agent, width, height, texture) { destBuffer ->
        for (x in 0 until width) {
            for (y in 0 until height) {
                val destIndex = 4 * (x + y * width)
                destBuffer.putFloat(destIndex, heightFunction(x, y))
            }
        }
    }
}
