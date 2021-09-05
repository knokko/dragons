package dragons.vulkan.memory

import dragons.init.trouble.SimpleStartupException
import dragons.plugin.PluginManager
import dragons.plugin.interfaces.vulkan.VulkanStaticMemoryUser
import dragons.util.nextMultipleOf
import dragons.vr.VrManager
import dragons.vulkan.memory.claim.PlacedQueueFamilyClaims
import dragons.vulkan.memory.claim.UninitializedImageMemoryClaim
import dragons.vulkan.memory.claim.groupMemoryClaims
import dragons.vulkan.memory.claim.placeMemoryClaims
import dragons.vulkan.queue.QueueFamily
import dragons.vulkan.queue.QueueManager
import dragons.vulkan.util.assertVkSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
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

class StaticMemory(
    val deviceBufferMemory: Long?,
    val deviceBuffers: Collection<Long>
    // TODO deviceImageMemory
)

@Throws(SimpleStartupException::class)
suspend fun allocateStaticMemory(
    vkDevice: VkDevice, queueManager: QueueManager, pluginManager: PluginManager, vrManager: VrManager,
    memoryInfo: MemoryInfo, scope: CoroutineScope
): StaticMemory {
    val logger = getLogger("Vulkan")

    logger.info("Calling all VulkanStaticMemoryUsers...")
    val memoryUsers = pluginManager.getImplementations(VulkanStaticMemoryUser::class)
    val pluginTasks = memoryUsers.map { (memoryUser, pluginInstance) ->
        scope.async {
            val agent = VulkanStaticMemoryUser.Agent(queueManager)
            memoryUser.claimStaticMemory(pluginInstance, agent)
            agent
        }
    }
    logger.info("All calls to the VulkanStaticMemoryUsers started")
    val finishedAgents = pluginTasks.map { task -> task.await() }.toMutableList()
    logger.info("All calls to the VulkanStaticMemoryUsers finished")

    // The game core also needs to add some static resources...
    val customAgent = VulkanStaticMemoryUser.Agent(queueManager)
    claimStaticCoreMemory(customAgent, vrManager)
    finishedAgents.add(customAgent)

    val groups = groupMemoryClaims(finishedAgents)
    val stagingBufferSize = groups.values.sumOf { it.stagingSize }

    var combinedDeviceBufferUsage = VK_BUFFER_USAGE_TRANSFER_DST_BIT
    for (claims in groups.values) {
        for (claim in claims.prefilledBufferClaims) {
            combinedDeviceBufferUsage = combinedDeviceBufferUsage or claim.usageFlags
        }
        for (claim in claims.uninitializedBufferClaims) {
            combinedDeviceBufferUsage = combinedDeviceBufferUsage or claim.usageFlags
        }
    }

    logger.info("The combined static staging buffer size is ${stagingBufferSize / 1000_000} MB")
    logger.info("Static buffer sizes are ${groups.values.map { it.deviceBufferSize / 1000_000}} MB")
    logger.info("Combined device buffer usage is $combinedDeviceBufferUsage")

    return stackPush().use { stack ->

        logger.info("Creating static device buffers...")
        val deviceBuffers = groups.entries.filter { (_, claims) ->
            claims.deviceBufferSize > 0L
        }.map { (queueFamily, claims) ->
            val ciDeviceBuffer = VkBufferCreateInfo.callocStack(stack)
            ciDeviceBuffer.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
            ciDeviceBuffer.size(claims.deviceBufferSize)
            ciDeviceBuffer.usage(combinedDeviceBufferUsage)
            if (queueFamily == null && queueManager.relevantQueueFamilies.size > 1) {
                ciDeviceBuffer.sharingMode(VK_SHARING_MODE_CONCURRENT)
                val queueFamilies = queueManager.relevantQueueFamilies

                val pQueueFamilies = stack.callocInt(queueFamilies.size)
                for ((index, queueFamilyIndex) in queueFamilies.withIndex()) {
                    pQueueFamilies.put(index, queueFamilyIndex)
                }
                ciDeviceBuffer.pQueueFamilyIndices(pQueueFamilies)
            } else {
                ciDeviceBuffer.sharingMode(VK_SHARING_MODE_EXCLUSIVE)
            }

            val pDeviceBuffer = stack.callocLong(1)
            assertVkSuccess(
                vkCreateBuffer(vkDevice, ciDeviceBuffer, null, pDeviceBuffer),
                "CreateBuffer", "static device for queue family ${queueFamily?.index}"
            )
            val deviceBuffer = pDeviceBuffer[0]

            val deviceMemoryRequirements = VkMemoryRequirements.callocStack(stack)
            vkGetBufferMemoryRequirements(vkDevice, deviceBuffer, deviceMemoryRequirements)
            Triple(deviceBuffer, deviceMemoryRequirements, queueFamily)
        }
        logger.info("Created static device buffers")

        var deviceBufferMemory: Long? = null

        if (deviceBuffers.isNotEmpty()) {

            // Since the flags of all buffers are 0 and the usage of all buffers is identical, the Vulkan specification
            // guarantees that the alignment and memoryTypeBits of all buffer memory requirements are identical.
            val memoryTypeBits = deviceBuffers[0].second.memoryTypeBits()
            val alignment = deviceBuffers[0].second.alignment()

            var currentOffset = 0L
            val deviceBufferOffsets = deviceBuffers.map { (_, memoryRequirements, _) ->
                val bufferOffset = nextMultipleOf(alignment, currentOffset)
                currentOffset = bufferOffset + memoryRequirements.size()
                bufferOffset
            }
            val deviceMemorySize = currentOffset

            val aiDeviceMemory = VkMemoryAllocateInfo.callocStack(stack)
            aiDeviceMemory.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
            aiDeviceMemory.allocationSize(deviceMemorySize)
            aiDeviceMemory.memoryTypeIndex(memoryInfo.chooseMemoryTypeIndex(
                memoryTypeBits, deviceMemorySize,
                desiredPropertyFlags = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
            )?: throw SimpleStartupException("Memory problem", listOf(
                "The game couldn't find a suitable memory type for the static device buffer"
            )))

            logger.info("Allocating static device buffer memory...")
            val pDeviceMemory = stack.callocLong(1)
            assertVkSuccess(
                vkAllocateMemory(vkDevice, aiDeviceMemory, null, pDeviceMemory),
                "AllocateMemory", "static device buffer"
            )
            deviceBufferMemory = pDeviceMemory[0]
            logger.info("Allocated static device buffer memory")

            for ((index, bufferTriple) in deviceBuffers.withIndex()) {
                val deviceBuffer = bufferTriple.first
                assertVkSuccess(
                    vkBindBufferMemory(vkDevice, deviceBuffer, deviceBufferMemory, deviceBufferOffsets[index]),
                    "BindBufferMemory", "static device buffer for queue family ${bufferTriple.third?.index}"
                )
            }
        }

        if (stagingBufferSize > 0) {
            val ciStagingBuffer = VkBufferCreateInfo.callocStack(stack)
            ciStagingBuffer.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
            ciStagingBuffer.size(stagingBufferSize)
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

            val stagingMemoryRequirements = VkMemoryRequirements.callocStack(stack)
            vkGetBufferMemoryRequirements(vkDevice, stagingBuffer, stagingMemoryRequirements)

            val aiStagingMemory = VkMemoryAllocateInfo.callocStack(stack)
            aiStagingMemory.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
            aiStagingMemory.allocationSize(stagingMemoryRequirements.size())
            aiStagingMemory.memoryTypeIndex(memoryInfo.chooseMemoryTypeIndex(
                stagingMemoryRequirements.memoryTypeBits(), stagingMemoryRequirements.size(),
                requiredPropertyFlags = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT,
                neutralPropertyFlags = VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
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

            logger.info("Filling static staging combined memory...")
            val stagingFillTasks = ArrayList<Deferred<Unit>>(groups.values.sumOf {
                it.prefilledBufferClaims.size // TODO plus number of of prefilled images
            })
            val startStagingAddress = pStagingAddress[0]

            val bufferPartOffsets = mutableMapOf<QueueFamily?, Long>()
            var stagingOffset = 0L

            val placementMap = mutableMapOf<QueueFamily?, PlacedQueueFamilyClaims>()
            for ((queueFamily, claims) in groups.entries) {

                val placements = placeMemoryClaims(claims)
                placementMap[queueFamily] = placements

                bufferPartOffsets[queueFamily] = stagingOffset + placements.prefilledBufferStagingOffset

                for (prefilledClaim in placements.prefilledBufferClaims) {
                    val claimedStagingPlace = memByteBuffer(
                        startStagingAddress + stagingOffset + placements.prefilledBufferStagingOffset + prefilledClaim.offset,
                        prefilledClaim.claim.size
                    )
                    stagingFillTasks.add(scope.async { prefilledClaim.claim.prefill(claimedStagingPlace) })
                }

                // TODO Prefill the staging buffer images

                stagingOffset += claims.stagingSize
            }

            logger.info("Started all static staging memory fill tasks")
            for (task in stagingFillTasks) {
                task.await()
            }
            logger.info("Filled static staging combined memory")

            // There is no guarantee that the buffer is coherent, so I may have to flush it explicitly
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

            val transferQueueFamily = queueManager.getTransferQueueFamily()

            // We use a dedicated command pool because this is a 1-time only transfer
            // TODO Perhaps keep the command pool for the duration of the game and use it for other transfers
            logger.info("Creating static transfer command resources...")
            val ciCommandPool = VkCommandPoolCreateInfo.callocStack(stack)
            ciCommandPool.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
            ciCommandPool.flags(VK_COMMAND_POOL_CREATE_TRANSIENT_BIT)
            ciCommandPool.queueFamilyIndex(transferQueueFamily.index)

            val pCommandPool = stack.callocLong(1)
            assertVkSuccess(
                vkCreateCommandPool(vkDevice, ciCommandPool, null, pCommandPool),
                "CreateCommandPool", "static transfer"
            )
            val commandPool = pCommandPool[0]

            val aiCommandBuffer = VkCommandBufferAllocateInfo.callocStack(stack)
            aiCommandBuffer.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
            aiCommandBuffer.commandPool(commandPool)
            aiCommandBuffer.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
            aiCommandBuffer.commandBufferCount(1)

            val pCommandBuffer = stack.callocPointer(1)
            assertVkSuccess(
                vkAllocateCommandBuffers(vkDevice, aiCommandBuffer, pCommandBuffer),
                "AllocateCommandBuffers", "static transfer"
            )
            val copyCommandBuffer = VkCommandBuffer(pCommandBuffer[0], vkDevice)

            val biCopyCommands = VkCommandBufferBeginInfo.callocStack(stack)
            biCopyCommands.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
            biCopyCommands.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)

            assertVkSuccess(
                vkBeginCommandBuffer(copyCommandBuffer, biCopyCommands),
                "BeginCommandBuffer", "static transfer"
            )

            logger.info("Created static transfer command resources")

            for ((deviceBuffer, _, queueFamily) in deviceBuffers) {
                val claims = groups[queueFamily]!!
                val placements = placementMap[queueFamily]!!

                if (claims.stagingSize > 0) {
                    val copyRegions = VkBufferCopy.callocStack(1, stack)
                    val copyRegion = copyRegions[0]

                    copyRegion.srcOffset(bufferPartOffsets[queueFamily]!!)
                    copyRegion.dstOffset(placements.prefilledBufferDeviceOffset)
                    copyRegion.size(claims.stagingSize)

                    vkCmdCopyBuffer(copyCommandBuffer, stagingBuffer, deviceBuffer, copyRegions)

                    // If queueFamily is null, the device buffer will use VK_SHARING_MODE_CONCURRENT and therefore
                    // doesn't need ownership transfers. If queueFamily is transferQueueFamily, it is already owned by
                    // the right queue family.
                    if (queueFamily != null && queueFamily != transferQueueFamily) {
                        val pBufferBarriers = VkBufferMemoryBarrier.callocStack(1, stack)
                        val bufferBarrier = pBufferBarriers[0]
                        bufferBarrier.sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
                        bufferBarrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                        bufferBarrier.dstAccessMask(0) // Ignored because it is a release operation
                        bufferBarrier.srcQueueFamilyIndex(transferQueueFamily.index)
                        bufferBarrier.dstQueueFamilyIndex(queueFamily.index)
                        bufferBarrier.buffer(deviceBuffer)
                        bufferBarrier.offset(0)
                        bufferBarrier.size(VK_WHOLE_SIZE)

                        vkCmdPipelineBarrier(
                            copyCommandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, 0,
                            0, null, pBufferBarriers, null
                        )
                    }
                }
            }

            // TODO Copy the buffer to the device images

            assertVkSuccess(
                vkEndCommandBuffer(copyCommandBuffer), "EndCommandBuffer", "static transfer"
            )

            val ciFence = VkFenceCreateInfo.callocStack(stack)
            ciFence.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)

            val pFence = stack.callocLong(1)
            assertVkSuccess(
                vkCreateFence(vkDevice, ciFence, null, pFence),
                "CreateFence", "static transfer"
            )
            val transferFence = pFence[0]

            logger.info("Submitting static transfer command...")
            val siTransfers = VkSubmitInfo.callocStack(1, stack)
            val siTransfer = siTransfers[0]
            siTransfer.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
            siTransfer.pCommandBuffers(pCommandBuffer)

            assertVkSuccess(
                vkQueueSubmit(transferQueueFamily.getRandomPriorityQueue().handle, siTransfers, transferFence),
                "QueueSubmit", "static transfer"
            )
            logger.info("Submitted static transfer command")

            assertVkSuccess(
                vkWaitForFences(vkDevice, pFence, true, -1),
                "WaitForFences", "static transfer"
            )
            logger.info("Static transfer command finished")

            val acquireQueueFamilies = groups.entries.filter { (queueFamily, claims) ->
                queueFamily != null && queueFamily != transferQueueFamily && claims.stagingSize > 0
            }

            val aiAcquireCommands = VkCommandBufferAllocateInfo.callocStack(stack)
            aiAcquireCommands.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
            aiAcquireCommands.commandPool(commandPool)
            aiAcquireCommands.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
            aiAcquireCommands.commandBufferCount(acquireQueueFamilies.size)

            val pAcquireCommands = stack.callocPointer(acquireQueueFamilies.size)

            logger.info("Start creating queue family ownership transfer commands for static buffers/images...")
            assertVkSuccess(
                vkAllocateCommandBuffers(vkDevice, aiAcquireCommands, pAcquireCommands),
                "AllocateCommandBuffers", "static staging ownership transfers"
            )

            val acquireFences = Array(acquireQueueFamilies.size) {
                pFence.put(0, 0)
                assertVkSuccess(
                    vkCreateFence(vkDevice, ciFence, null, pFence),
                    "CreateFence", "acquire queue family buffer"
                )
                pFence[0]
            }
            for ((index, queueFamilyPair) in acquireQueueFamilies.withIndex()) {
                val commandBuffer = VkCommandBuffer(pAcquireCommands[index], vkDevice)
                val queueFamily = queueFamilyPair.key!!

                assertVkSuccess(
                    vkBeginCommandBuffer(commandBuffer, biCopyCommands),
                    "BeginCommandBuffer", "queue family ownership transfer to ${queueFamily.index}"
                )

                val pBufferBarriers: VkBufferMemoryBarrier.Buffer?
                if (queueFamilyPair.value.prefilledBufferClaims.isNotEmpty()) {
                    pBufferBarriers = VkBufferMemoryBarrier.callocStack(1, stack)
                    val bufferBarrier = pBufferBarriers[0]
                    bufferBarrier.sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
                    bufferBarrier.srcAccessMask(0) // Ignored because it is an acquire operation
                    bufferBarrier.dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT)
                    bufferBarrier.srcQueueFamilyIndex(queueFamily.index)
                    bufferBarrier.dstQueueFamilyIndex(transferQueueFamily.index)
                    bufferBarrier.buffer(deviceBuffers.find { (_, _, bufferQueueFamily) -> bufferQueueFamily == queueFamily }!!.first)
                    bufferBarrier.offset(0)
                    bufferBarrier.size(VK_WHOLE_SIZE)
                } else {
                    pBufferBarriers = null
                }

                // TODO Also acquire image ownership
                vkCmdPipelineBarrier(
                    commandBuffer, 0, VK_PIPELINE_STAGE_TRANSFER_BIT,
                    0, null, pBufferBarriers, null
                )

                assertVkSuccess(
                    vkEndCommandBuffer(commandBuffer), "EndCommandBuffer",
                    "queue family ownership transfer to ${queueFamily.index}"
                )

                val siAcquires = VkSubmitInfo.callocStack(1, stack)
                val siAcquire = siAcquires[0]
                siAcquire.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                siAcquire.pCommandBuffers(stack.pointers(commandBuffer.address()))

                assertVkSuccess(
                    vkQueueSubmit(queueFamily.getRandomPriorityQueue().handle, siAcquires, acquireFences[index]),
                    "QueueSubmit", "acquire static resources for queue family ${queueFamily.index}"
                )
            }

            val pAcquireFences = stack.callocLong(acquireFences.size)
            for ((index, fence) in acquireFences.withIndex()) {
                pAcquireFences.put(index, fence)
            }

            assertVkSuccess(
                vkWaitForFences(vkDevice, pAcquireFences, true, -1),
                "WaitForFences", "static buffer/image acquire"
            )
            logger.info("Finished transferring ownership of the static buffers and images")

            vkDestroyCommandPool(vkDevice, commandPool, null)

            logger.info("Destroying and freeing static combined staging memory...")
            vkDestroyBuffer(vkDevice, stagingBuffer, null)
            vkFreeMemory(vkDevice, stagingMemory, null)
            logger.info("Destroyed and freed static combined staging memory")
        }

        StaticMemory(deviceBufferMemory, deviceBuffers.map { it.first })
    }
}

fun destroyStaticMemory(vkDevice: VkDevice, staticMemory: StaticMemory) {
    for (deviceBuffer in staticMemory.deviceBuffers) {
        vkDestroyBuffer(vkDevice, deviceBuffer, null)
    }
    if (staticMemory.deviceBufferMemory != null) {
        vkFreeMemory(vkDevice, staticMemory.deviceBufferMemory, null)
    }
    // TODO Destroy the images
}
