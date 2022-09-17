package dragons.plugins.debug.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.vulkan.VulkanStaticMemoryUser
import dragons.vulkan.memory.VulkanBufferRange
import dragons.vulkan.memory.VulkanImage
import dragons.vulkan.memory.claim.BufferMemoryClaim
import dragons.vulkan.memory.claim.ImageMemoryClaim
import dragons.vulkan.memory.claim.StagingBufferMemoryClaim
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import org.lwjgl.system.MemoryUtil.memAddress
import org.lwjgl.vulkan.VK12.*
import java.nio.ByteBuffer

class TestStaticMemory: VulkanStaticMemoryUser {
    override fun claimStaticMemory(pluginInstance: PluginInstance, agent: VulkanStaticMemoryUser.Agent) {
        if (pluginInstance.gameInitProps.mainParameters.testParameters.staticMemory) {
            val lock = Object()
            for (queueFamily in agent.queueManager.allQueueFamilies + listOf(null)) {
                val prefilledIndirectResult = CompletableDeferred<VulkanBufferRange>()
                agent.claims.buffers.add(
                    BufferMemoryClaim(
                        size = 200,
                        usageFlags = VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT,
                        dstAccessMask = VK_ACCESS_INDIRECT_COMMAND_READ_BIT,
                        dstPipelineStageMask = VK_PIPELINE_STAGE_DRAW_INDIRECT_BIT,
                        queueFamily = queueFamily,
                        storeResult = prefilledIndirectResult
                    ) { byteBuffer ->
                        println("Prefilled indirect buffer starts at ${memAddress(byteBuffer)}")
                        check(byteBuffer.capacity() == 200)
                        while (byteBuffer.hasRemaining()) byteBuffer.put(121)
                    }
                )

                val prefilledStorageResult = CompletableDeferred<VulkanBufferRange>()
                agent.claims.buffers.add(
                    BufferMemoryClaim(
                        size = 300,
                        usageFlags = VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
                        dstAccessMask = VK_ACCESS_SHADER_READ_BIT or VK_ACCESS_SHADER_WRITE_BIT,
                        dstPipelineStageMask = VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                        queueFamily = queueFamily,
                        storeResult = prefilledStorageResult
                    ) { byteBuffer ->
                        println("Prefilled storage buffer starts at ${memAddress(byteBuffer)}")
                        check(byteBuffer.capacity() == 300)
                        while (byteBuffer.hasRemaining()) byteBuffer.put(123)
                    }
                )

                val prefilledVertexResult = CompletableDeferred<VulkanBufferRange>()
                agent.claims.buffers.add(
                    BufferMemoryClaim(
                        size = 150,
                        usageFlags = VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                        dstAccessMask = VK_ACCESS_TRANSFER_READ_BIT,
                        dstPipelineStageMask = VK_PIPELINE_STAGE_TRANSFER_BIT,
                        queueFamily = queueFamily,
                        storeResult = prefilledVertexResult
                    ) { byteBuffer ->
                        println("Prefilled vertex buffer starts at ${memAddress(byteBuffer)}")
                        check(byteBuffer.capacity() == 150)
                        while (byteBuffer.hasRemaining()) byteBuffer.put(126)
                    }
                )

                val uninitializedUniformResult = CompletableDeferred<VulkanBufferRange>()
                agent.claims.buffers.add(
                    BufferMemoryClaim(
                        size = 400,
                        usageFlags = VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                        queueFamily = queueFamily,
                        storeResult = uninitializedUniformResult,
                        prefill = null
                    )
                )

                val uninitializedStorageResult = CompletableDeferred<VulkanBufferRange>()
                agent.claims.buffers.add(
                    BufferMemoryClaim(
                        size = 345,
                        usageFlags = VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
                        queueFamily = queueFamily,
                        storeResult = uninitializedStorageResult,
                        prefill = null
                    )
                )

                val prefilledColorResult: CompletableDeferred<VulkanImage>?
                val prefilledDepthResult: CompletableDeferred<VulkanImage>?
                val uninitializedColorResult: CompletableDeferred<VulkanImage>?
                val uninitializedDepthResult: CompletableDeferred<VulkanImage>?

                if (queueFamily == agent.queueManager.generalQueueFamily) {
                    prefilledColorResult = CompletableDeferred()
                    agent.claims.images.add(
                        ImageMemoryClaim(
                            width = 50, height = 70, queueFamily = queueFamily,
                            bytesPerPixel = 4, imageFormat = VK_FORMAT_R8G8B8A8_SINT,
                            tiling = VK_IMAGE_TILING_OPTIMAL, samples = VK_SAMPLE_COUNT_1_BIT,
                            imageUsage = VK_IMAGE_USAGE_INPUT_ATTACHMENT_BIT, initialLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                            accessMask = VK_ACCESS_SHADER_READ_BIT, dstPipelineStageMask = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                            aspectMask = VK_IMAGE_ASPECT_COLOR_BIT, storeResult = prefilledColorResult
                        ) { byteBuffer ->
                            println("Prefilled color staging buffer starts at ${memAddress(byteBuffer)}")
                            check(byteBuffer.capacity() == (50 * 70 * 4))
                            while (byteBuffer.hasRemaining()) byteBuffer.put(127)
                        }
                    )

                    prefilledDepthResult = CompletableDeferred()
                    agent.claims.images.add(
                        ImageMemoryClaim(
                            width = 60, height = 20, queueFamily = queueFamily,
                            bytesPerPixel = 4, imageFormat = VK_FORMAT_D32_SFLOAT,
                            tiling = VK_IMAGE_TILING_OPTIMAL, samples = VK_SAMPLE_COUNT_1_BIT,
                            imageUsage = VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT,
                            initialLayout = VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
                            accessMask = VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT,
                            dstPipelineStageMask = VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT,
                            aspectMask = VK_IMAGE_ASPECT_DEPTH_BIT,
                            storeResult = prefilledDepthResult
                        ) { byteBuffer ->
                            println("Prefilled depth staging buffer starts at ${memAddress(byteBuffer)}")
                            check(byteBuffer.capacity() == (60 * 20 * 4))
                            while (byteBuffer.hasRemaining()) byteBuffer.put(-12)
                        }
                    )

                    uninitializedColorResult = CompletableDeferred()
                    agent.claims.images.add(
                        ImageMemoryClaim(
                            width = 10,
                            height = 11,
                            queueFamily = queueFamily,
                            bytesPerPixel = 4,
                            imageFormat = VK_FORMAT_R8G8B8A8_SRGB,
                            tiling = VK_IMAGE_TILING_OPTIMAL,
                            samples = VK_SAMPLE_COUNT_1_BIT,
                            imageUsage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT,
                            initialLayout = VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                            accessMask = VK_ACCESS_SHADER_READ_BIT,
                            dstPipelineStageMask = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                            aspectMask = VK_IMAGE_ASPECT_COLOR_BIT,
                            storeResult = uninitializedColorResult,
                            prefill = null
                        )
                    )
                    uninitializedDepthResult = CompletableDeferred()
                    agent.claims.images.add(
                        ImageMemoryClaim(
                            width = 100, height = 70, queueFamily = queueFamily,
                            bytesPerPixel = 4, imageFormat = VK_FORMAT_D32_SFLOAT,
                            tiling = VK_IMAGE_TILING_OPTIMAL, samples = VK_SAMPLE_COUNT_2_BIT,
                            imageUsage = VK_IMAGE_USAGE_INPUT_ATTACHMENT_BIT, initialLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                            accessMask = VK_ACCESS_SHADER_READ_BIT, dstPipelineStageMask = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                            aspectMask = VK_IMAGE_ASPECT_DEPTH_BIT, storeResult = uninitializedDepthResult, prefill = null
                        )
                    )
                } else {
                    prefilledColorResult = null
                    prefilledDepthResult = null
                    uninitializedColorResult = null
                    uninitializedDepthResult = null
                }

                val indexStagingResult = CompletableDeferred<Pair<ByteBuffer, VulkanBufferRange>>()
                agent.claims.stagingBuffers.add(
                    StagingBufferMemoryClaim(
                        size = 543, alignment = 3, queueFamily = queueFamily, storeResult = indexStagingResult
                    )
                )

                agent.gameInitScope.launch {
                    val prefilledIndirectBuffer = prefilledIndirectResult.await()
                    val prefilledStorageBuffer = prefilledStorageResult.await()
                    val prefilledVertexBuffer = prefilledVertexResult.await()
                    val uninitializedUniformBuffer = uninitializedUniformResult.await()
                    val uninitializedStorageBuffer = uninitializedStorageResult.await()
                    val prefilledColorImage = prefilledColorResult?.await()
                    val prefilledDepthImage = prefilledDepthResult?.await()
                    val uninitializedColorImage = uninitializedColorResult?.await()
                    val uninitializedDepthImage = uninitializedDepthResult?.await()
                    val indexStagingBuffer = indexStagingResult.await()

                    synchronized(lock) {
                        println(queueFamily)
                        println("Prefilled indirect buffer is $prefilledIndirectBuffer")
                        println("Prefilled storage buffer is $prefilledStorageBuffer")
                        println("Prefilled vertex buffer is $prefilledVertexBuffer")
                        println("Uninitialized uniform buffer is $uninitializedUniformBuffer")
                        println("Uninitialized storage buffer is $uninitializedStorageBuffer")
                        println("Prefilled color image is $prefilledColorImage")
                        println("Prefilled depth image is $prefilledDepthImage")
                        println("Uninitialized color image is $uninitializedColorImage")
                        println("Uninitialized depth image is $uninitializedDepthImage")
                        println("Index staging buffer is ${indexStagingBuffer.second} at CPU address ${memAddress(indexStagingBuffer.first)}")
                    }

                    check(prefilledIndirectBuffer.size == 200L)
                    check(prefilledStorageBuffer.size == 300L)
                    check(prefilledVertexBuffer.size == 150L)
                    check(uninitializedUniformBuffer.size == 400L)
                    check(uninitializedStorageBuffer.size == 345L)
                    check(indexStagingBuffer.first.capacity() == 543)
                }
            }
        }
    }
}
