package dragons.vulkan.memory.scope

import dragons.vulkan.memory.MemoryInfo
import dragons.vulkan.memory.VulkanBufferRange
import dragons.vulkan.memory.VulkanImage
import dragons.vulkan.memory.claim.ImageMemoryClaim
import dragons.vulkan.memory.claim.groupMemoryClaims
import dragons.vulkan.queue.QueueManager
import dragons.vulkan.util.assertVkSuccess
import kotlinx.coroutines.CoroutineScope
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memByteBuffer
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkDevice
import org.slf4j.LoggerFactory.getLogger

suspend fun packMemoryClaims(
    vkDevice: VkDevice, queueManager: QueueManager, memoryInfo: MemoryInfo, scope: CoroutineScope,
    allClaims: Collection<MemoryScopeClaims>, description: String
): MemoryScope {
    val logger = getLogger("Vulkan")

    val familyClaimsMap = groupMemoryClaims(allClaims)

    val combinedStagingPlacements = determineStagingPlacements(familyClaimsMap)

    var combinedDeviceBufferUsage = VK_BUFFER_USAGE_TRANSFER_DST_BIT
    var combinedStagingBufferUsage = VK_BUFFER_USAGE_TRANSFER_SRC_BIT
    for (familyClaims in familyClaimsMap.values) {
        for (claim in familyClaims.claims.prefilledBufferClaims) {
            combinedDeviceBufferUsage = combinedDeviceBufferUsage or claim.usageFlags
        }
        for (claim in familyClaims.claims.uninitializedBufferClaims) {
            combinedDeviceBufferUsage = combinedDeviceBufferUsage or claim.usageFlags
        }
        for (claim in familyClaims.claims.stagingBufferClaims) {
            combinedStagingBufferUsage = combinedStagingBufferUsage or claim.usageFlags
        }
    }

    logger.info("Scope $description: The combined temporary staging buffer size is ${combinedStagingPlacements.totalSize / 1000_000} MB")
    logger.info("Scope $description: Buffer sizes are ${combinedStagingPlacements.queueFamilies.values.map { it.deviceBufferSize / 1000_000}} MB")
    logger.info("Scope $description: Combined device buffer usage is $combinedDeviceBufferUsage")

    return stackPush().use { stack ->
        val (persistentStagingMemory, stagingQueueFamilyToBufferMap, stagingQueueFamilyToOffsetMap) = createCombinedBuffers(
            logger = logger, stack = stack, vkDevice = vkDevice, queueManager = queueManager,
            groups = familyClaimsMap, memoryInfo = memoryInfo,

            getSize = { claims, _ -> claims.persistentStagingSize },
            bufferUsage = combinedStagingBufferUsage,
            description = "Scope $description: persistent staging",
            // Note: the Vulkan specification guarantees that at least 1 memory heap has these properties
            requiredMemoryPropertyFlags = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
            desiredMemoryPropertyFlags = 0,
            neutralMemoryPropertyFlags = 0
        )

        val persistentStagingAddress = if (persistentStagingMemory != null) {
            val ppPersistentStaging = stack.callocPointer(1)
            assertVkSuccess(
                vkMapMemory(vkDevice, persistentStagingMemory, 0, VK_WHOLE_SIZE, 0, ppPersistentStaging),
                "MapMemory", "Scope $description: persistent staging"
            )
            ppPersistentStaging[0]
        } else { null }

        val (deviceBufferMemory, deviceQueueFamilyToBufferMap) = createCombinedBuffers(
            logger = logger, stack = stack, vkDevice = vkDevice, queueManager = queueManager,
            groups = familyClaimsMap, memoryInfo = memoryInfo,

            getSize = { _, queueFamily -> combinedStagingPlacements.queueFamilies[queueFamily]!!.deviceBufferSize },
            bufferUsage = combinedDeviceBufferUsage,
            description = "Scope $description: device",
            requiredMemoryPropertyFlags = 0,
            desiredMemoryPropertyFlags = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
            neutralMemoryPropertyFlags = 0
        )

        val familiesCommands = FamiliesCommands.construct(vkDevice, queueManager, description)

        val numDeviceImages = familyClaimsMap.values.sumOf { it.claims.allImageClaims.size }
        val (deviceImageMemory, claimsToImageMap) = if (numDeviceImages > 0) {

            val pairedImageClaims = mutableListOf<Pair<ImageMemoryClaim, VulkanImage>>()
            for ((_, claims) in familyClaimsMap.entries) {
                pairedImageClaims.addAll(claims.claims.allImageClaims.map { claim ->
                    Pair(claim, createImage(stack, vkDevice, queueManager, claim))
                })
            }

            val (deviceImageMemory, claimsToImageMap) = bindAndAllocateImageMemory(stack, vkDevice, memoryInfo, pairedImageClaims, description)

            familiesCommands.performInitialTransition(claimsToImageMap, queueManager, description)

            Pair(deviceImageMemory, claimsToImageMap)
        } else { Pair(null, emptyMap()) }

        for ((queueFamily, stagingPlacements) in combinedStagingPlacements.queueFamilies) {
            for (placedClaim in stagingPlacements.internalPlacements.uninitializedBufferClaims) {
                placedClaim.claim.storeResult.complete(VulkanBufferRange(
                    buffer = deviceQueueFamilyToBufferMap[queueFamily]!!,
                    offset = stagingPlacements.internalPlacements.uninitializedBufferDeviceOffset + placedClaim.offset,
                    size = placedClaim.claim.size.toLong()
                ))
            }
            for (claim in stagingPlacements.internalPlacements.uninitializedImageClaims) {
                claim.storeResult.complete(claimsToImageMap[claim]!!)
            }
            for (placedClaim in stagingPlacements.internalPlacements.stagingBufferClaims) {
                val stagingMemoryAddress = persistentStagingAddress!! + stagingQueueFamilyToOffsetMap[queueFamily]!! + placedClaim.offset
                val stagingByteBuffer = memByteBuffer(stagingMemoryAddress, placedClaim.claim.size)
                placedClaim.claim.storeResult.complete(Pair(stagingByteBuffer, VulkanBufferRange(
                    buffer = stagingQueueFamilyToBufferMap[queueFamily]!!,
                    offset = stagingPlacements.internalPlacements.stagingBufferOffset + placedClaim.offset,
                    size = placedClaim.claim.size.toLong()
                )))
            }
        }

        if (combinedStagingPlacements.totalSize > 0) {
            val (tempStagingMemory, tempStagingBuffer, tempStagingAddress) = createCombinedStagingBuffer(
                vkDevice, memoryInfo, stack, combinedStagingPlacements.totalSize, description
            )

            fillStagingBuffer(
                vkDevice, scope, stack, tempStagingMemory, combinedStagingPlacements, tempStagingAddress, description
            )

            familiesCommands.performStagingCopy(
                tempStagingBuffer, familyClaimsMap, combinedStagingPlacements,
                claimsToImageMap, deviceQueueFamilyToBufferMap, queueManager, description
            )

            familiesCommands.finishAndCleanUp(description)

            vkDestroyBuffer(vkDevice, tempStagingBuffer, null)
            vkFreeMemory(vkDevice, tempStagingMemory, null)
        } else {
            familiesCommands.finishAndCleanUp(description)
        }

        for ((queueFamily, stagingPlacements) in combinedStagingPlacements.queueFamilies) {
            for (placedClaim in stagingPlacements.internalPlacements.prefilledBufferClaims) {
                placedClaim.claim.storeResult.complete(VulkanBufferRange(
                    buffer = deviceQueueFamilyToBufferMap[queueFamily]!!,
                    offset = stagingPlacements.internalPlacements.prefilledBufferDeviceOffset + placedClaim.offset,
                    size = placedClaim.claim.size.toLong()
                ))
            }
            for (placedClaim in stagingPlacements.internalPlacements.prefilledImageClaims) {
                placedClaim.claim.storeResult.complete(claimsToImageMap[placedClaim.claim]!!)
            }
        }

        MemoryScope(
            deviceBufferMemory = deviceBufferMemory,
            deviceBuffers = deviceQueueFamilyToBufferMap.values,
            persistentStagingMemory = persistentStagingMemory,
            persistentStagingBuffers = stagingQueueFamilyToBufferMap.values,
            deviceImageMemory = deviceImageMemory,
            deviceImages = claimsToImageMap.values
        )
    }
}
