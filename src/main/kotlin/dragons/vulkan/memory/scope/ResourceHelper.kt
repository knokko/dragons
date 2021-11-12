package dragons.vulkan.memory.scope

import dragons.init.trouble.SimpleStartupException
import dragons.util.nextMultipleOf
import dragons.vulkan.memory.MemoryInfo
import dragons.vulkan.memory.VulkanBuffer
import dragons.vulkan.memory.VulkanImage
import dragons.vulkan.memory.claim.ImageMemoryClaim
import dragons.vulkan.memory.claim.QueueFamilyClaims
import dragons.vulkan.queue.QueueFamily
import dragons.vulkan.queue.QueueManager
import dragons.vulkan.util.assertVkSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger

internal fun createCombinedBuffers(
    logger: Logger, stack: MemoryStack, vkDevice: VkDevice, queueManager: QueueManager,
    groups: Map<QueueFamily?, QueueFamilyClaims>, memoryInfo: MemoryInfo,
    getSize: (QueueFamilyClaims) -> Long, bufferUsage: Int, description: String,
    requiredMemoryPropertyFlags: Int, desiredMemoryPropertyFlags: Int, neutralMemoryPropertyFlags: Int
): Triple<Long?, Map<QueueFamily?, VulkanBuffer>, Map<QueueFamily?, Long>> {
    logger.info("Creating $description buffers...")

    val combinedBuffers = groups.entries.filter { (_, claims) ->
        getSize(claims) > 0
    }.map { (queueFamily, claims) ->
        val ciBuffer = VkBufferCreateInfo.calloc(stack)
        ciBuffer.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
        ciBuffer.size(getSize(claims))
        ciBuffer.usage(bufferUsage)
        if (queueFamily == null && queueManager.allQueueFamilies.size > 1) {
            ciBuffer.sharingMode(VK_SHARING_MODE_CONCURRENT)
            val queueFamilies = queueManager.allQueueFamilies

            val pQueueFamilies = stack.callocInt(queueFamilies.size)
            for ((index, listQueueFamily) in queueFamilies.withIndex()) {
                pQueueFamilies.put(index, listQueueFamily.index)
            }
            ciBuffer.pQueueFamilyIndices(pQueueFamilies)
        } else {
            ciBuffer.sharingMode(VK_SHARING_MODE_EXCLUSIVE)
        }

        val pBuffer = stack.callocLong(1)
        assertVkSuccess(
            vkCreateBuffer(vkDevice, ciBuffer, null, pBuffer),
            "CreateBuffer", "$description for queue family ${queueFamily?.index}"
        )
        val buffer = VulkanBuffer(pBuffer[0])

        val memoryRequirements = VkMemoryRequirements.calloc(stack)
        vkGetBufferMemoryRequirements(vkDevice, buffer.handle, memoryRequirements)
        Triple(buffer, memoryRequirements, queueFamily)
    }

    logger.info("Created $description buffers")

    val queueFamilyToMemoryOffsetMap = mutableMapOf<QueueFamily?, Long>()

    var combinedBufferMemory: Long? = null

    if (combinedBuffers.isNotEmpty()) {

        // Since the flags of all buffers are 0 and the usage of all buffers is identical, the Vulkan specification
        // guarantees that the alignment and memoryTypeBits of all buffer memory requirements are identical.
        val memoryTypeBits = combinedBuffers[0].second.memoryTypeBits()
        val alignment = combinedBuffers[0].second.alignment()

        // We will create 1 buffer per used queue family, but they will share the same memory allocation.
        var currentOffset = 0L
        val bufferMemoryOffsets = combinedBuffers.map { (_, memoryRequirements, _) ->
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
        ))
        )

        logger.info("Allocating $description memory with memory type ${aiPersistentMemory.memoryTypeIndex()}...")
        val pPersistentMemory = stack.callocLong(1)
        assertVkSuccess(
            vkAllocateMemory(vkDevice, aiPersistentMemory, null, pPersistentMemory),
            "AllocateMemory", description
        )
        combinedBufferMemory = pPersistentMemory[0]
        logger.info("Allocated $description memory")

        for ((index, bufferTriple) in combinedBuffers.withIndex()) {
            val buffer = bufferTriple.first
            queueFamilyToMemoryOffsetMap[bufferTriple.third] = bufferMemoryOffsets[index]
            assertVkSuccess(
                vkBindBufferMemory(vkDevice, buffer.handle, combinedBufferMemory, bufferMemoryOffsets[index]),
                "BindBufferMemory", "$description for queue family ${bufferTriple.third?.index}"
            )
        }
    }

    val queueFamilyToBufferMap = mutableMapOf<QueueFamily?, VulkanBuffer>()
    for ((buffer, _, queueFamily) in combinedBuffers) {
        queueFamilyToBufferMap[queueFamily] = VulkanBuffer(buffer.handle)
    }

    return Triple(combinedBufferMemory, queueFamilyToBufferMap, queueFamilyToMemoryOffsetMap)
}

internal fun createCombinedStagingBuffer(
    vkDevice: VkDevice, memoryInfo: MemoryInfo, stack: MemoryStack,
    tempStagingBufferSize: Long, description: String
): Triple<Long, Long, Long> {
    val logger = getLogger("Vulkan")

    val ciTempStagingBuffer = VkBufferCreateInfo.calloc(stack)
    ciTempStagingBuffer.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
    ciTempStagingBuffer.size(tempStagingBufferSize)
    ciTempStagingBuffer.usage(VK_BUFFER_USAGE_TRANSFER_SRC_BIT)
    ciTempStagingBuffer.sharingMode(VK_SHARING_MODE_EXCLUSIVE)

    logger.info("Scope $description: Creating staging buffer...")
    val pTempStagingBuffer = stack.callocLong(1)
    assertVkSuccess(
        vkCreateBuffer(vkDevice, ciTempStagingBuffer, null, pTempStagingBuffer),
        "CreateBuffer", "Scope $description: temp staging"
    )
    logger.info("Scope $description: Created temporary staging buffer")
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

    logger.info("Scope $description: Allocating temporary staging combined memory with memory type ${aiTempStagingMemory.memoryTypeIndex()}...")
    val pStagingMemory = stack.callocLong(1)
    assertVkSuccess(
        vkAllocateMemory(vkDevice, aiTempStagingMemory, null, pStagingMemory),
        "AllocateMemory", "Scope $description: temp staging buffer"
    )
    val tempStagingMemory = pStagingMemory[0]
    logger.info("Scope $description: Allocated staging combined memory")

    assertVkSuccess(
        vkBindBufferMemory(vkDevice, tempStagingBuffer, tempStagingMemory, 0L),
        "BindBufferMemory", "Scope $description: staging buffer"
    )

    logger.info("Scope $description: Mapping temporary staging combined memory...")
    val pStagingAddress = stack.callocPointer(1)
    assertVkSuccess(
        vkMapMemory(vkDevice, tempStagingMemory, 0L, VK_WHOLE_SIZE, 0, pStagingAddress),
        "MapMemory", "Scope $description: staging buffer"
    )
    logger.info("Scope $description: Mapped temporary staging combined memory")
    val tempStagingAddress = pStagingAddress[0]

    return Triple(tempStagingMemory, tempStagingBuffer, tempStagingAddress)
}

internal fun createImage(
    stack: MemoryStack, vkDevice: VkDevice, queueManager: QueueManager,
    claim: ImageMemoryClaim
): VulkanImage {

    val mipLevels = 1
    val arrayLayers = 1
    // TODO Stop hardcoding these and handle them appropriately

    val ciImage = VkImageCreateInfo.calloc(stack)
    ciImage.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
    ciImage.flags(claim.imageCreateFlags)
    ciImage.imageType(VK_IMAGE_TYPE_2D)
    ciImage.format(claim.imageFormat)
    ciImage.extent(VkExtent3D.calloc(stack).set(claim.width, claim.height, 1))
    ciImage.mipLevels(mipLevels)
    ciImage.arrayLayers(arrayLayers)
    ciImage.samples(claim.samples)
    ciImage.tiling(claim.tiling)
    if (claim.prefill != null) {
        ciImage.usage(claim.imageUsage or VK_IMAGE_USAGE_TRANSFER_DST_BIT)
    } else {
        ciImage.usage(claim.imageUsage)
    }
    if (claim.queueFamily == null && queueManager.allQueueFamilies.size > 1) {
        ciImage.sharingMode(VK_SHARING_MODE_CONCURRENT)

        val queueFamilies = queueManager.allQueueFamilies
        val pQueueFamilies = stack.callocInt(queueFamilies.size)
        for ((index, queueFamily) in queueFamilies.withIndex()) {
            pQueueFamilies.put(index, queueFamily.index)
        }
        ciImage.pQueueFamilyIndices(pQueueFamilies)
    } else {
        ciImage.sharingMode(VK_SHARING_MODE_EXCLUSIVE)
    }
    ciImage.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)

    val pImage = stack.callocLong(1)
    assertVkSuccess(
        vkCreateImage(vkDevice, ciImage, null, pImage),
        "CreateImage", "static"
    )
    val image = pImage[0]

    return VulkanImage(image, claim.width, claim.height)
}

internal fun createFullImageView(
    stack: MemoryStack, vkDevice: VkDevice, claim: ImageMemoryClaim, image: VulkanImage
) {
    val mipLevels = 1
    val arrayLayers = 1
    // TODO Stop hardcoding these and handle them appropriately

    val ciView = VkImageViewCreateInfo.calloc(stack)
    ciView.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
    ciView.flags(claim.imageViewFlags)
    ciView.image(image.handle)
    ciView.viewType(VK_IMAGE_VIEW_TYPE_2D) // TODO Use 2D_ARRAY if needed?
    ciView.format(claim.imageFormat)
    ciView.components { components ->
        components.r(VK_COMPONENT_SWIZZLE_IDENTITY)
        components.g(VK_COMPONENT_SWIZZLE_IDENTITY)
        components.b(VK_COMPONENT_SWIZZLE_IDENTITY)
        components.a(VK_COMPONENT_SWIZZLE_IDENTITY)
    }
    ciView.subresourceRange { range ->
        range.aspectMask(claim.aspectMask)
        range.baseMipLevel(0)
        range.levelCount(mipLevels)
        range.baseArrayLayer(0)
        range.layerCount(arrayLayers)
    }

    val pImageView = stack.callocLong(1)
    assertVkSuccess(
        vkCreateImageView(vkDevice, ciView, null, pImageView),
        "CreateImageView", "full static"
    )
    val fullImageView = pImageView[0]
    image.fullView = fullImageView
}

internal fun bindAndAllocateImageMemory(
    stack: MemoryStack, vkDevice: VkDevice, memoryInfo: MemoryInfo,
    allDeviceImages: Collection<Pair<ImageMemoryClaim, VulkanImage>>, description: String
): Pair<Long, Map<ImageMemoryClaim, VulkanImage>> {
    val imageRequirements = VkMemoryRequirements.calloc(stack)
    var memoryTypeBits = -1 // Note: the binary representation of -1 consists of only ones
    var nextImageOffset = 0L
    val imageOffsets = allDeviceImages.map { (claim, image) ->
        vkGetImageMemoryRequirements(vkDevice, image.handle, imageRequirements)
        memoryTypeBits = memoryTypeBits and imageRequirements.memoryTypeBits()

        val thisImageOffset = nextMultipleOf(imageRequirements.alignment(), nextImageOffset)
        nextImageOffset = thisImageOffset + imageRequirements.size()

        Triple(image, claim, thisImageOffset)
    }

    val aiImageMemory = VkMemoryAllocateInfo.calloc(stack)
    aiImageMemory.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
    aiImageMemory.allocationSize(nextImageOffset)
    // TODO I might want to handle the case where this returns null. I'm afraid this is possible in theory
    aiImageMemory.memoryTypeIndex(memoryInfo.chooseMemoryTypeIndex(
        memoryTypeBits, aiImageMemory.allocationSize(),
        requiredPropertyFlags = 0,
        desiredPropertyFlags = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
        neutralPropertyFlags = 0
    )!!)

    val pImageMemory = stack.callocLong(1)
    assertVkSuccess(
        vkAllocateMemory(vkDevice, aiImageMemory, null, pImageMemory),
        "AllocateMemory", "Scope $description: device image"
    )
    val deviceImageMemory = pImageMemory[0]

    for ((image, _, offset) in imageOffsets) {
        assertVkSuccess(
            vkBindImageMemory(vkDevice, image.handle, deviceImageMemory, offset),
            "BindImageMemory", "Scope $description: device image at $offset"
        )
    }

    for ((claim, image) in allDeviceImages) {
        createFullImageView(stack, vkDevice, claim, image)
    }

    val claimsToImageMap = mutableMapOf<ImageMemoryClaim, VulkanImage>()
    for ((image, claim) in imageOffsets) {
        claimsToImageMap[claim] = image
    }
    return Pair(deviceImageMemory, claimsToImageMap.toMap())
}
