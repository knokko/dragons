package dragons.vulkan.memory.scope

import dragons.vulkan.memory.MemoryInfo
import dragons.vulkan.memory.VulkanImage
import dragons.vulkan.memory.claim.ImageMemoryClaim
import dragons.vulkan.memory.claim.PrefilledImageMemoryClaim
import dragons.vulkan.memory.claim.UninitializedImageMemoryClaim
import dragons.vulkan.memory.claim.groupMemoryClaims
import dragons.vulkan.queue.QueueManager
import dragons.vulkan.util.assertVkSuccess
import kotlinx.coroutines.CoroutineScope
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkDevice
import org.slf4j.LoggerFactory.getLogger

suspend fun packMemoryClaims(
    vkDevice: VkDevice, queueManager: QueueManager, memoryInfo: MemoryInfo, scope: CoroutineScope,
    allClaims: Collection<MemoryScopeClaims>, description: String
): MemoryScope {
    val logger = getLogger("Vulkan")

    val familyClaimsMap = groupMemoryClaims(allClaims)

    val tempStagingBufferSize = familyClaimsMap.values.sumOf { it.tempStagingSize }

    var combinedDeviceBufferUsage = VK_BUFFER_USAGE_TRANSFER_DST_BIT
    for (familyClaims in familyClaimsMap.values) {
        for (claim in familyClaims.claims.prefilledBufferClaims) {
            combinedDeviceBufferUsage = combinedDeviceBufferUsage or claim.usageFlags
        }
        for (claim in familyClaims.claims.uninitializedBufferClaims) {
            combinedDeviceBufferUsage = combinedDeviceBufferUsage or claim.usageFlags
        }
    }

    logger.info("Scope $description: The combined temporary staging buffer size is ${tempStagingBufferSize / 1000_000} MB")
    logger.info("Scope $description: Buffer sizes are ${familyClaimsMap.values.map { it.deviceBufferSize / 1000_000}} MB")
    logger.info("Scope $description: Combined device buffer usage is $combinedDeviceBufferUsage")

    return stackPush().use { stack ->
        val (persistentStagingMemory, stagingQueueFamilyToBufferMap) = createCombinedBuffers(
            logger = logger, stack = stack, vkDevice = vkDevice, queueManager = queueManager,
            groups = familyClaimsMap, memoryInfo = memoryInfo,

            getSize = { claims -> claims.persistentStagingSize },
            bufferUsage = VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
            description = "Scope $description: persistent staging",
            requiredMemoryPropertyFlags = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT,
            desiredMemoryPropertyFlags = 0,
            neutralMemoryPropertyFlags = VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
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

            getSize = { claims -> claims.deviceBufferSize },
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
                    Pair(claim, createImage(stack, vkDevice, queueManager, claim, claim.prefill != null))
                })
            }

            val claimsToImageMap = bindAndAllocateImageMemory(stack, vkDevice, memoryInfo, pairedImageClaims, description)

            familiesCommands.performInitialTransition(claimsToImageMap, queueManager, description)

            Pair(deviceBufferMemory, claimsToImageMap)
        } else { Pair(null, emptyMap()) }

        if (tempStagingBufferSize > 0) {
            val (tempStagingMemory, tempStagingBuffer, tempStagingAddress) = createCombinedStagingBuffer(
                vkDevice, memoryInfo, stack, tempStagingBufferSize, description
            )

            val stagingPlacementMap = fillStagingBuffer(
                vkDevice, scope, stack, tempStagingMemory, familyClaimsMap, tempStagingAddress, description
            )

            familiesCommands.performStagingCopy(
                tempStagingBuffer, stagingPlacementMap, claimsToImageMap, deviceQueueFamilyToBufferMap, queueManager, description
            )

            // TODO Destroy staging resources after all copies and ownership transitions are complete
        }

        MemoryScope()
    }
}
