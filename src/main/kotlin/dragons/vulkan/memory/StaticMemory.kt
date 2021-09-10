package dragons.vulkan.memory

import dragons.init.trouble.SimpleStartupException
import dragons.plugin.PluginManager
import dragons.plugin.interfaces.vulkan.VulkanStaticMemoryUser
import dragons.util.nextMultipleOf
import dragons.vr.VrManager
import dragons.vulkan.memory.claim.*
import dragons.vulkan.memory.claim.PlacedQueueFamilyClaims
import dragons.vulkan.memory.claim.QueueFamilyClaims
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
    val deviceBuffers: Collection<Long>,
    val persistentStagingMemory: Long?,
    val persistentStagingBuffers: Collection<Long>
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
    val tempStagingBufferSize = groups.values.sumOf { it.tempStagingSize }

    var combinedDeviceBufferUsage = VK_BUFFER_USAGE_TRANSFER_DST_BIT
    for (claims in groups.values) {
        for (claim in claims.prefilledBufferClaims) {
            combinedDeviceBufferUsage = combinedDeviceBufferUsage or claim.usageFlags
        }
        for (claim in claims.uninitializedBufferClaims) {
            combinedDeviceBufferUsage = combinedDeviceBufferUsage or claim.usageFlags
        }
    }

    logger.info("The combined temporary static staging buffer size is ${tempStagingBufferSize / 1000_000} MB")
    logger.info("Static buffer sizes are ${groups.values.map { it.deviceBufferSize / 1000_000}} MB")
    logger.info("Combined device buffer usage is $combinedDeviceBufferUsage")

    return stackPush().use { stack ->

        fun createPersistentBuffers(
            getSize: (QueueFamilyClaims) -> Long, bufferUsage: Int, description: String,
            requiredMemoryPropertyFlags: Int, desiredMemoryPropertyFlags: Int, neutralMemoryPropertyFlags: Int
        ): Pair<Long?, Collection<Triple<Long, QueueFamily?, Long>>> {
            logger.info("Creating $description buffers...")

            val persistentBuffers = groups.entries.filter { (_, claims) ->
                getSize(claims) > 0
            }.map { (queueFamily, claims) ->
                val ciPersistentBuffer = VkBufferCreateInfo.calloc(stack)
                ciPersistentBuffer.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                ciPersistentBuffer.size(getSize(claims))
                ciPersistentBuffer.usage(bufferUsage)
                if (queueFamily == null && queueManager.relevantQueueFamilies.size > 1) {
                    ciPersistentBuffer.sharingMode(VK_SHARING_MODE_CONCURRENT)
                    val queueFamilies = queueManager.relevantQueueFamilies

                    val pQueueFamilies = stack.callocInt(queueFamilies.size)
                    for ((index, queueFamilyIndex) in queueFamilies.withIndex()) {
                        pQueueFamilies.put(index, queueFamilyIndex)
                    }
                    ciPersistentBuffer.pQueueFamilyIndices(pQueueFamilies)
                } else {
                    ciPersistentBuffer.sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                }

                val pPersistentBuffer = stack.callocLong(1)
                assertVkSuccess(
                    vkCreateBuffer(vkDevice, ciPersistentBuffer, null, pPersistentBuffer),
                    "CreateBuffer", "$description for queue family ${queueFamily?.index}"
                )
                val persistentBuffer = pPersistentBuffer[0]

                val persistentMemoryRequirements = VkMemoryRequirements.calloc(stack)
                vkGetBufferMemoryRequirements(vkDevice, persistentBuffer, persistentMemoryRequirements)
                Triple(persistentBuffer, persistentMemoryRequirements, queueFamily)
            }

            logger.info("Created $description buffers")

            var persistentBufferMemory: Long? = null

            val persistentBufferOffsets = if (persistentBuffers.isNotEmpty()) {

                // Since the flags of all buffers are 0 and the usage of all buffers is identical, the Vulkan specification
                // guarantees that the alignment and memoryTypeBits of all buffer memory requirements are identical.
                val memoryTypeBits = persistentBuffers[0].second.memoryTypeBits()
                val alignment = persistentBuffers[0].second.alignment()

                // We will create 1 buffer per used queue family, but they will share the same memory allocation.
                var currentOffset = 0L
                val persistentBufferOffsets = persistentBuffers.map { (_, memoryRequirements, _) ->
                    val bufferOffset = nextMultipleOf(alignment, currentOffset)
                    currentOffset = bufferOffset + memoryRequirements.size()
                    bufferOffset
                }
                val persistentMemorySize = currentOffset

                val aiPersistentMemory = VkMemoryAllocateInfo.calloc(stack)
                aiPersistentMemory.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                aiPersistentMemory.allocationSize(persistentMemorySize)
                aiPersistentMemory.memoryTypeIndex(memoryInfo.chooseMemoryTypeIndex(
                    memoryTypeBits, persistentMemorySize,
                    requiredPropertyFlags = requiredMemoryPropertyFlags,
                    desiredPropertyFlags = desiredMemoryPropertyFlags,
                    neutralPropertyFlags = neutralMemoryPropertyFlags
                )?: throw SimpleStartupException("Memory problem", listOf(
                    "The game couldn't find a suitable memory type for the $description memory"
                )))

                logger.info("Allocating $description memory with memory type ${aiPersistentMemory.memoryTypeIndex()}...")
                val pPersistentMemory = stack.callocLong(1)
                assertVkSuccess(
                    vkAllocateMemory(vkDevice, aiPersistentMemory, null, pPersistentMemory),
                    "AllocateMemory", description
                )
                persistentBufferMemory = pPersistentMemory[0]
                logger.info("Allocated $description memory")

                for ((index, bufferTriple) in persistentBuffers.withIndex()) {
                    val persistentBuffer = bufferTriple.first
                    assertVkSuccess(
                        vkBindBufferMemory(vkDevice, persistentBuffer, persistentBufferMemory, persistentBufferOffsets[index]),
                        "BindBufferMemory", "$description for queue family ${bufferTriple.third?.index}"
                    )
                }
                persistentBufferOffsets
            } else { null }

            return Pair(persistentBufferMemory, persistentBuffers.withIndex().map { (index, bufferTriple) ->
                Triple(bufferTriple.first, bufferTriple.third, persistentBufferOffsets!![index])
            })
        }

        val (persistentStagingMemory, persistentStagingBuffers) = createPersistentBuffers(
            getSize = { claims -> claims.persistentStagingSize },
            bufferUsage = VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
            description = "persistent staging",
            requiredMemoryPropertyFlags = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT,
            desiredMemoryPropertyFlags = 0,
            neutralMemoryPropertyFlags = VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
        )

        val persistentStagingAddress = if (persistentStagingMemory != null) {
            val ppPersistentStaging = stack.callocPointer(1)
            assertVkSuccess(
                vkMapMemory(vkDevice, persistentStagingMemory, 0, VK_WHOLE_SIZE, 0, ppPersistentStaging),
                "MapMemory", "persistent staging"
            )
            ppPersistentStaging[0]
        } else { null }

        val (deviceMemory, deviceBuffers) = createPersistentBuffers(
            getSize = { claims -> claims.deviceBufferSize },
            bufferUsage = combinedDeviceBufferUsage,
            description = "static device",
            requiredMemoryPropertyFlags = 0,
            desiredMemoryPropertyFlags = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
            neutralMemoryPropertyFlags = 0
        )

        if (tempStagingBufferSize > 0) {
            val ciTempStagingBuffer = VkBufferCreateInfo.calloc(stack)
            ciTempStagingBuffer.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
            ciTempStagingBuffer.size(tempStagingBufferSize)
            ciTempStagingBuffer.usage(VK_BUFFER_USAGE_TRANSFER_SRC_BIT)
            ciTempStagingBuffer.sharingMode(VK_SHARING_MODE_EXCLUSIVE)

            logger.info("Creating static staging buffer...")
            val pTempStagingBuffer = stack.callocLong(1)
            assertVkSuccess(
                vkCreateBuffer(vkDevice, ciTempStagingBuffer, null, pTempStagingBuffer),
                "CreateBuffer", "temp static staging"
            )
            logger.info("Created temporary static staging buffer")
            val tempStagingBuffer = pTempStagingBuffer[0]

            val stagingMemoryRequirements = VkMemoryRequirements.calloc(stack)
            vkGetBufferMemoryRequirements(vkDevice, tempStagingBuffer, stagingMemoryRequirements)

            val aiTempStagingMemory = VkMemoryAllocateInfo.calloc(stack)
            aiTempStagingMemory.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
            aiTempStagingMemory.allocationSize(stagingMemoryRequirements.size())
            aiTempStagingMemory.memoryTypeIndex(memoryInfo.chooseMemoryTypeIndex(
                stagingMemoryRequirements.memoryTypeBits(), stagingMemoryRequirements.size(),
                requiredPropertyFlags = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT,
                neutralPropertyFlags = VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
            )?: throw SimpleStartupException("Memory problem", listOf(
                 "The game couldn't find a suitable memory type for temporary staging buffer memory"
            )))

            logger.info("Allocating temporary static staging combined memory with memory type ${aiTempStagingMemory.memoryTypeIndex()}...")
            val pStagingMemory = stack.callocLong(1)
            assertVkSuccess(
                vkAllocateMemory(vkDevice, aiTempStagingMemory, null, pStagingMemory),
                "AllocateMemory", "temp static staging buffer"
            )
            val tempStagingMemory = pStagingMemory[0]
            logger.info("Allocated static staging combined memory")

            assertVkSuccess(
                vkBindBufferMemory(vkDevice, tempStagingBuffer, tempStagingMemory, 0L),
                "BindBufferMemory", "static staging buffer"
            )

            logger.info("Mapping temporary static staging combined memory...")
            val pStagingAddress = stack.callocPointer(1)
            assertVkSuccess(
                vkMapMemory(vkDevice, tempStagingMemory, 0L, VK_WHOLE_SIZE, 0, pStagingAddress),
                "MapMemory", "static staging buffer"
            )
            logger.info("Mapped temporary static staging combined memory")

            logger.info("Filling temporary static staging combined memory...")
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

                stagingOffset += claims.tempStagingSize
            }

            for (task in stagingFillTasks) {
                task.await()
            }
            logger.info("Filled static staging combined memory")

            // There is no guarantee that the buffer is coherent, so I may have to flush it explicitly
            val flushRanges = VkMappedMemoryRange.calloc(1, stack)
            val flushRange = flushRanges[0]
            flushRange.sType(VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE)
            flushRange.memory(tempStagingMemory)
            flushRange.offset(0)
            flushRange.size(VK_WHOLE_SIZE)

            logger.info("Flushing static combined staging memory...")
            assertVkSuccess(
                vkFlushMappedMemoryRanges(vkDevice, flushRanges),
                "FlushMappedMemoryRanges", "static combined staging"
            )
            logger.info("Flushed combined static staging memory; Unmapping it...")
            vkUnmapMemory(vkDevice, tempStagingMemory)
            logger.info("Unmapped combined static staging memory")

            val transferQueueFamily = queueManager.getTransferQueueFamily()

            // We use a dedicated command pool because this is a 1-time only transfer, and we don't expect to need a next
            // transfer anytime soon
            logger.info("Creating static transfer command resources...")
            val ciCommandPool = VkCommandPoolCreateInfo.calloc(stack)
            ciCommandPool.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
            ciCommandPool.flags(VK_COMMAND_POOL_CREATE_TRANSIENT_BIT)
            ciCommandPool.queueFamilyIndex(transferQueueFamily.index)

            val pCommandPool = stack.callocLong(1)
            assertVkSuccess(
                vkCreateCommandPool(vkDevice, ciCommandPool, null, pCommandPool),
                "CreateCommandPool", "static transfer"
            )
            val commandPool = pCommandPool[0]

            val aiCommandBuffer = VkCommandBufferAllocateInfo.calloc(stack)
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

            val biCopyCommands = VkCommandBufferBeginInfo.calloc(stack)
            biCopyCommands.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
            biCopyCommands.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)

            assertVkSuccess(
                vkBeginCommandBuffer(copyCommandBuffer, biCopyCommands),
                "BeginCommandBuffer", "static transfer"
            )

            logger.info("Created static transfer command resources")

            for ((deviceBuffer, queueFamily) in deviceBuffers) {
                val claims = groups[queueFamily]!!
                val placements = placementMap[queueFamily]!!

                if (claims.tempStagingSize > 0) {
                    val copyRegions = VkBufferCopy.calloc(1, stack)
                    val copyRegion = copyRegions[0]

                    copyRegion.srcOffset(bufferPartOffsets[queueFamily]!!)
                    copyRegion.dstOffset(placements.prefilledBufferDeviceOffset)
                    copyRegion.size(claims.tempStagingSize)

                    vkCmdCopyBuffer(copyCommandBuffer, tempStagingBuffer, deviceBuffer, copyRegions)

                    // If queueFamily is null, the device buffer will use VK_SHARING_MODE_CONCURRENT and therefore
                    // doesn't need ownership transfers. If queueFamily is transferQueueFamily, it is already owned by
                    // the right queue family.
                    if (queueFamily != null && queueFamily != transferQueueFamily) {
                        val pBufferBarriers = VkBufferMemoryBarrier.calloc(1, stack)
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
                            copyCommandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
                            0, null, pBufferBarriers, null
                        )
                    }
                }
            }

            // TODO Copy the buffer to the device images

            assertVkSuccess(
                vkEndCommandBuffer(copyCommandBuffer), "EndCommandBuffer", "static transfer"
            )

            val ciFence = VkFenceCreateInfo.calloc(stack)
            ciFence.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)

            val pFence = stack.callocLong(1)
            assertVkSuccess(
                vkCreateFence(vkDevice, ciFence, null, pFence),
                "CreateFence", "static transfer"
            )
            val transferFence = pFence[0]

            logger.info("Submitting static transfer command...")
            val siTransfers = VkSubmitInfo.calloc(1, stack)
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
            vkDestroyFence(vkDevice, pFence[0], null)
            logger.info("Static transfer command finished")

            val acquireQueueFamilies = groups.entries.filter { (queueFamily, claims) ->
                queueFamily != null && queueFamily != transferQueueFamily && claims.tempStagingSize > 0
            }.map { (queueFamily, claims) -> Pair(queueFamily!!, claims) }

            val acquireFences = Array(acquireQueueFamilies.size) {
                pFence.put(0, 0)
                assertVkSuccess(
                    vkCreateFence(vkDevice, ciFence, null, pFence),
                    "CreateFence", "acquire queue family buffer"
                )
                pFence[0]
            }

            logger.info("Start transferring queue family ownership for static buffers/images")
            val acquireCommandPools = Array(acquireQueueFamilies.size) { 0L }
            for ((rawIndex, queueFamilyPair) in acquireQueueFamilies.withIndex()) {
                val (queueFamily, claims) = queueFamilyPair
                val ciAcquirePool = VkCommandPoolCreateInfo.calloc(stack)
                ciAcquirePool.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                ciAcquirePool.flags(VK_COMMAND_POOL_CREATE_TRANSIENT_BIT)
                ciAcquirePool.queueFamilyIndex(queueFamily.index)

                val pAcquirePool = stack.callocLong(1)
                assertVkSuccess(
                    vkCreateCommandPool(vkDevice, ciAcquirePool, null, pAcquirePool),
                    "CreateCommandPool", "static buffer/image acquire for queue family ${queueFamily.index}"
                )
                val acquirePool = pAcquirePool[0]
                acquireCommandPools[rawIndex] = acquirePool

                val aiAcquireCommand = VkCommandBufferAllocateInfo.calloc(stack)
                aiAcquireCommand.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                aiAcquireCommand.commandPool(acquirePool)
                aiAcquireCommand.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                aiAcquireCommand.commandBufferCount(1)

                val pAcquireCommand = stack.callocPointer(1)

                assertVkSuccess(
                    vkAllocateCommandBuffers(vkDevice, aiAcquireCommand, pAcquireCommand),
                    "AllocateCommandBuffers", "static staging ownership transfers to queue family {$queueFamily.index}"
                )
                val acquireCommandBuffer = VkCommandBuffer(pAcquireCommand[0], vkDevice)

                assertVkSuccess(
                    vkBeginCommandBuffer(acquireCommandBuffer, biCopyCommands),
                    "BeginCommandBuffer", "queue family ownership transfer to ${queueFamily.index}"
                )

                val pBufferBarriers: VkBufferMemoryBarrier.Buffer?
                if (claims.prefilledBufferClaims.isNotEmpty()) {
                    pBufferBarriers = VkBufferMemoryBarrier.calloc(1, stack)
                    val bufferBarrier = pBufferBarriers[0]
                    bufferBarrier.sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
                    bufferBarrier.srcAccessMask(0) // Ignored because it is an acquire operation
                    bufferBarrier.dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT)
                    bufferBarrier.srcQueueFamilyIndex(transferQueueFamily.index)
                    bufferBarrier.dstQueueFamilyIndex(queueFamily.index)
                    bufferBarrier.buffer(deviceBuffers.find { (_, bufferQueueFamily, _) -> bufferQueueFamily == queueFamily }!!.first)
                    bufferBarrier.offset(0)
                    bufferBarrier.size(VK_WHOLE_SIZE)
                } else {
                    pBufferBarriers = null
                }

                // TODO Also acquire image ownership
                vkCmdPipelineBarrier(
                    acquireCommandBuffer, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
                    0, null, pBufferBarriers, null
                )

                assertVkSuccess(
                    vkEndCommandBuffer(acquireCommandBuffer), "EndCommandBuffer",
                    "queue family ownership transfer to ${queueFamily.index}"
                )

                val siAcquires = VkSubmitInfo.calloc(1, stack)
                val siAcquire = siAcquires[0]
                siAcquire.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                siAcquire.pCommandBuffers(stack.pointers(acquireCommandBuffer.address()))

                assertVkSuccess(
                    vkQueueSubmit(queueFamily.getRandomPriorityQueue().handle, siAcquires, acquireFences[rawIndex]),
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
            for (acquireCommandPool in acquireCommandPools) {
                vkDestroyCommandPool(vkDevice, acquireCommandPool, null)
            }
            for (fence in acquireFences) {
                vkDestroyFence(vkDevice, fence, null)
            }
            vkDestroyCommandPool(vkDevice, commandPool, null)
            logger.info("Finished transferring ownership of the static buffers and images")

            for ((queueFamily, placements) in placementMap.entries) {
                val deviceBuffer = VulkanBuffer(deviceBuffers.find { (_, candidateQueueFamily) -> candidateQueueFamily == queueFamily }!!.first)

                for (placedClaim in placements.uninitializedBufferClaims) {
                    placedClaim.claim.storeResult.complete(VulkanBufferRange(
                        buffer = deviceBuffer,
                        offset = placements.uninitializedBufferDeviceOffset + placedClaim.offset,
                        size = placedClaim.claim.size.toLong()
                    ))
                }

                for (placedClaim in placements.prefilledBufferClaims) {
                    placedClaim.claim.storeResult.complete(VulkanBufferRange(
                        buffer = deviceBuffer,
                        offset = placements.prefilledBufferDeviceOffset + placedClaim.offset,
                        size = placedClaim.claim.size.toLong()
                    ))
                }

                for (placedClaim in placements.stagingBufferClaims) {
                    val stagingTriple = persistentStagingBuffers.find { it.second == queueFamily }!!
                    val stagingBuffer = VulkanBuffer(stagingTriple.first)
                    val stagingBufferOffset = stagingTriple.third
                    placedClaim.claim.storeResult.complete(Pair(
                        memByteBuffer(
                            startStagingAddress + stagingBufferOffset +
                                    placements.stagingBufferOffset + placedClaim.offset,
                            placedClaim.claim.size
                        ), VulkanBufferRange(
                            buffer = stagingBuffer,
                            offset = placements.stagingBufferOffset + placedClaim.offset,
                            size = placedClaim.claim.size.toLong()
                        )
                    ))
                }
            }

            logger.info("Destroying and freeing static combined staging memory...")
            vkDestroyBuffer(vkDevice, tempStagingBuffer, null)
            vkFreeMemory(vkDevice, tempStagingMemory, null)
            logger.info("Destroyed and freed static combined staging memory")
        }

        StaticMemory(
            deviceMemory, deviceBuffers.map { it.first },
            persistentStagingMemory, persistentStagingBuffers.map { it.first }
        )
    }
}

fun destroyStaticMemory(vkDevice: VkDevice, staticMemory: StaticMemory) {
    for (deviceBuffer in staticMemory.deviceBuffers) {
        vkDestroyBuffer(vkDevice, deviceBuffer, null)
    }
    if (staticMemory.deviceBufferMemory != null) {
        vkFreeMemory(vkDevice, staticMemory.deviceBufferMemory, null)
    }
    for (stagingBuffer in staticMemory.persistentStagingBuffers) {
        vkDestroyBuffer(vkDevice, stagingBuffer, null)
    }
    if (staticMemory.persistentStagingMemory != null) {
        vkFreeMemory(vkDevice, staticMemory.persistentStagingMemory, null)
    }
    // TODO Destroy the images
}
