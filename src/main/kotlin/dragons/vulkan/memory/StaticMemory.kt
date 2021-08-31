package dragons.vulkan.memory

import dragons.init.trouble.SimpleStartupException
import dragons.plugin.PluginManager
import dragons.plugin.interfaces.vulkan.VulkanStaticMemoryUser
import dragons.vr.VrManager
import dragons.vulkan.memory.claim.UninitializedImageMemoryClaim
import dragons.vulkan.queue.QueueManager
import dragons.vulkan.util.assertVkSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memByteBuffer
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.*
import org.slf4j.LoggerFactory.getLogger

private fun claimStaticCoreMemory(agent: VulkanStaticMemoryUser.Agent, vrManager: VrManager) {
    val width = vrManager.getWidth()
    val height = vrManager.getHeight()

    // Left eye: color buffer and depth buffer
    agent.uninitializedImages.add(UninitializedImageMemoryClaim(width, height, 4))
    agent.uninitializedImages.add(UninitializedImageMemoryClaim(width, height, 4))

    // Right eye: color buffer and depth buffer
    agent.uninitializedImages.add(UninitializedImageMemoryClaim(width, height, 4))
    agent.uninitializedImages.add(UninitializedImageMemoryClaim(width, height, 4))
}

@Throws(SimpleStartupException::class)
suspend fun allocateStaticMemory(
    vkDevice: VkDevice, queueManager: QueueManager, pluginManager: PluginManager, vrManager: VrManager,
    memoryInfo: MemoryInfo, scope: CoroutineScope
) {
    val logger = getLogger("Vulkan")

    logger.info("Calling all VulkanStaticMemoryUsers...")
    val memoryUsers = pluginManager.getImplementations(VulkanStaticMemoryUser::class)
    val pluginTasks = memoryUsers.map { (memoryUser, pluginInstance) ->
        scope.async {
            val agent = VulkanStaticMemoryUser.Agent()
            memoryUser.claimStaticMemory(pluginInstance, agent)
            agent
        }
    }
    logger.info("All calls to the VulkanStaticMemoryUsers started")
    val finishedAgents = pluginTasks.map { task -> task.await() }.toMutableList()
    logger.info("All calls to the VulkanStaticMemoryUsers finished")

    // The game core also needs to add some static resources...
    val customAgent = VulkanStaticMemoryUser.Agent()
    claimStaticCoreMemory(customAgent, vrManager)
    finishedAgents.add(customAgent)

    var totalDeviceOnlyBufferSize = 0L
    var totalStagingBufferSize = 0L
    var combinedBufferUsage = 0

    var totalStagingImageSize = 0L
    // TODO Add support for static images
    for (agent in finishedAgents) {
        totalStagingBufferSize += agent.prefilledBuffers.sumOf { bufferClaim -> bufferClaim.size }
        totalDeviceOnlyBufferSize += agent.uninitializedBuffers.sumOf { bufferClaim -> bufferClaim.size }
        for (bufferClaim in agent.prefilledBuffers) {
            combinedBufferUsage = combinedBufferUsage or bufferClaim.usageFlags
        }
        for (bufferClaim in agent.uninitializedBuffers) {
            combinedBufferUsage = combinedBufferUsage or bufferClaim.usageFlags
        }
        totalStagingImageSize += agent.prefilledImages.sumOf { imageClaim -> imageClaim.width * imageClaim.height * imageClaim.bytesPerPixel }
//        totalDeviceOnlyImageSize += agent.uninitializedImages.sumOf { imageClaim -> imageClaim.width * imageClaim.height * imageClaim.bytesPerPixel }
    }

    if (totalStagingBufferSize > 0) {
        // We will need to copy the staging buffer to the device buffer
        combinedBufferUsage = combinedBufferUsage or VK_BUFFER_USAGE_TRANSFER_DST_BIT
    }
    val combinedStagingBufferSize = totalStagingBufferSize + totalStagingImageSize
    val totalDeviceBufferSize = totalDeviceOnlyBufferSize + totalStagingBufferSize
    //val totalDeviceImageSize = totalDeviceOnlyImageSize + totalStagingImageSize
    logger.info("The combined static staging buffer size is ${combinedStagingBufferSize / 1000_000} MB")
    logger.info("Total static buffer size is ${totalDeviceBufferSize / 1000_000} MB")
    //logger.info("Total static image size is ${totalDeviceImageSize / 1000_000} MB")

    stackPush().use { stack ->

        if (combinedStagingBufferSize > 0) {
            val ciStagingBuffer = VkBufferCreateInfo.callocStack(stack)
            ciStagingBuffer.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
            // Combine the staging buffer with the 'staging image' because using the same buffer for both is easier
            ciStagingBuffer.size(combinedStagingBufferSize)
            ciStagingBuffer.usage(VK_BUFFER_USAGE_TRANSFER_SRC_BIT)
            ciStagingBuffer.sharingMode(VK_SHARING_MODE_EXCLUSIVE)

            logger.info("Creating static staging buffer...")
            val pStagingBuffer = stack.callocLong(1)
            assertVkSuccess(
                vkCreateBuffer(vkDevice, ciStagingBuffer, null, pStagingBuffer),
                "CreateBuffer", "static staging"
            )
            logger.info("Created static staging buffer")
            val stagingBuffer = pStagingBuffer[0]

            val memoryRequirements = VkMemoryRequirements.callocStack(stack)
            vkGetBufferMemoryRequirements(vkDevice, stagingBuffer, memoryRequirements)

            val aiStagingMemory = VkMemoryAllocateInfo.callocStack(stack)
            aiStagingMemory.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
            aiStagingMemory.allocationSize(memoryRequirements.size())
            aiStagingMemory.memoryTypeIndex(memoryInfo.chooseMemoryTypeIndex(
                memoryRequirements.memoryTypeBits(), VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
            )?: throw SimpleStartupException("Memory problem", listOf(
                 "The game couldn't find a suitable memory type for staging buffer memory"
            )))

            logger.info("Allocating static staging combined memory...")
            val pStagingMemory = stack.callocLong(1)
            assertVkSuccess(
                vkAllocateMemory(vkDevice, aiStagingMemory, null, pStagingMemory),
                "AllocateMemory", "static staging buffer"
            )
            val stagingMemory = pStagingMemory[0]
            logger.info("Allocated static staging combined memory")

            assertVkSuccess(
                vkBindBufferMemory(vkDevice, stagingBuffer, stagingMemory, 0L),
                "BindBufferMemory", "static staging buffer"
            )

            logger.info("Mapping static staging combined memory...")
            val pStagingAddress = stack.callocPointer(1)
            assertVkSuccess(
                vkMapMemory(vkDevice, stagingMemory, 0L, VK_WHOLE_SIZE, 0, pStagingAddress),
                "MapMemory", "static staging buffer"
            )
            logger.info("Mapped static staging combined memory")

            /*
             * Java ByteBuffers don't support capacities greater than Integer.MAX_VALUE. I could work around this
             * problem by using more unsafe tricks, but I'm not expecting to hit this limit anytime soon.
             *
             * Even when I do hit this limit, it would probably be better to solve it by reducing the total mapped
             * memory size since not all operating systems will appreciate such huge blocks of contiguous memory.
             */
            if (combinedStagingBufferSize > Integer.MAX_VALUE) {
                throw SimpleStartupException("Memory problem", listOf(
                    "The total size of the combined static staging buffer is $combinedStagingBufferSize bytes,",
                    "which exceeds the maximum allowed size of ${Integer.MAX_VALUE} bytes"
                ))
            }
            val cpuStagingBuffer = memByteBuffer(pStagingAddress[0], combinedStagingBufferSize.toInt())

            // TODO Fill cpuStagingBuffer

            val flushRanges = VkMappedMemoryRange.callocStack(1, stack)
            val flushRange = flushRanges[0]
            flushRange.sType(VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE)
            flushRange.memory(stagingMemory)
            flushRange.offset(0)
            flushRange.size(VK_WHOLE_SIZE)

            logger.info("Flushing static combined staging memory...")
            assertVkSuccess(
                vkFlushMappedMemoryRanges(vkDevice, flushRanges),
                "FlushMappedMemoryRanges", "static combined staging"
            )
            logger.info("Flushed combined static staging memory; Unmapping it...")
            vkUnmapMemory(vkDevice, stagingMemory)
            logger.info("Unmapped combined static staging memory")

            // TODO Copy the staging buffer to the device buffer and device image

            logger.info("Destroying and free static combined staging memory...")
            vkDestroyBuffer(vkDevice, stagingBuffer, null)
            vkFreeMemory(vkDevice, stagingMemory, null)
            logger.info("Destroyed and freed static combined staging memory")
        }
    }
}

fun destroyStaticMemory() {
    // TODO Destroy the static device memory
}
