package dragons.plugins.standard.vulkan.pipeline

import dragons.vulkan.memory.VulkanBufferRange
import dragons.vulkan.memory.VulkanImage
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.*
import troll.exceptions.VulkanFailureException.assertVkSuccess

fun createBasicStaticDescriptorPool(vkDevice: VkDevice): Long {
    return stackPush().use { stack ->

        // Uniform descriptor, texture sampling descriptor, and storage descriptor
        val poolSizes = VkDescriptorPoolSize.calloc(3, stack)

        val uniformPoolSize = poolSizes[0]
        uniformPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
        uniformPoolSize.descriptorCount(1)

        val samplerPoolSize = poolSizes[1]
        samplerPoolSize.type(VK_DESCRIPTOR_TYPE_SAMPLER)
        samplerPoolSize.descriptorCount(1)

        val storagePoolSize = poolSizes[2]
        storagePoolSize.type(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
        storagePoolSize.descriptorCount(1)

        val ciDescriptorPool = VkDescriptorPoolCreateInfo.calloc(stack)
        ciDescriptorPool.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
        // The basic graphics pipeline uses only 1 descriptor set
        ciDescriptorPool.maxSets(1)
        ciDescriptorPool.pPoolSizes(poolSizes)

        val pDescriptorPool = stack.callocLong(1)
        assertVkSuccess(
            vkCreateDescriptorPool(vkDevice, ciDescriptorPool, null, pDescriptorPool),
            "CreateDescriptorPool", "standard plug-in: basic static"
        )
        pDescriptorPool[0]
    }
}

fun createBasicDynamicDescriptorPool(vkDevice: VkDevice, maxSets: Int): Long {
    return stackPush().use { stack ->
        // Color images and height images
        val poolSizes = VkDescriptorPoolSize.calloc(2, stack)
        val colorImagesPoolSize = poolSizes[0]
        colorImagesPoolSize.type(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
        colorImagesPoolSize.descriptorCount(MAX_NUM_DESCRIPTOR_IMAGES * maxSets)

        val heightImagesPoolSize = poolSizes[1]
        heightImagesPoolSize.type(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
        heightImagesPoolSize.descriptorCount(MAX_NUM_DESCRIPTOR_IMAGES * maxSets)

        val ciDescriptorPool = VkDescriptorPoolCreateInfo.calloc(stack)
        ciDescriptorPool.`sType$Default`()
        ciDescriptorPool.maxSets(maxSets)
        ciDescriptorPool.pPoolSizes(poolSizes)

        val pDescriptorPool = stack.callocLong(1)
        assertVkSuccess(
            vkCreateDescriptorPool(vkDevice, ciDescriptorPool, null, pDescriptorPool),
            "CreateDescriptorPool", "standard plug-in: basic dynamic"
        )
        pDescriptorPool[0]
    }
}

fun createBasicStaticDescriptorSet(
    vkDevice: VkDevice, descriptorPool: Long, descriptorSetLayout: Long,
    cameraDeviceBuffer: VulkanBufferRange, transformationMatrixDeviceBuffer: VulkanBufferRange,
    sampler: Long
): Long {
    return stackPush().use { stack ->
        val aiDescriptor = VkDescriptorSetAllocateInfo.calloc(stack)
        aiDescriptor.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
        aiDescriptor.descriptorPool(descriptorPool)
        aiDescriptor.pSetLayouts(stack.longs(descriptorSetLayout))

        val pDescriptorSet = stack.callocLong(1)
        assertVkSuccess(
            vkAllocateDescriptorSets(vkDevice, aiDescriptor, pDescriptorSet),
            "AllocateDescriptorSets", "standard plug-in: basic static"
        )
        val basicDescriptorSet = pDescriptorSet[0]

        val biUniforms = VkDescriptorBufferInfo.calloc(1, stack)
        val biUniform = biUniforms[0]
        biUniform.buffer(cameraDeviceBuffer.buffer.handle)
        biUniform.offset(cameraDeviceBuffer.offset)
        biUniform.range(cameraDeviceBuffer.size)

        val iiSamplers = VkDescriptorImageInfo.calloc(1, stack)
        val iiSampler = iiSamplers[0]
        iiSampler.sampler(sampler)
        // imageView and imageLayout are ignored

        val biStorages = VkDescriptorBufferInfo.calloc(1, stack)
        val biStorage = biStorages[0]
        biStorage.buffer(transformationMatrixDeviceBuffer.buffer.handle)
        biStorage.offset(transformationMatrixDeviceBuffer.offset)
        biStorage.range(transformationMatrixDeviceBuffer.size)

        val descriptorWrites = VkWriteDescriptorSet.calloc(3, stack)
        val uniformWrite = descriptorWrites[0]
        uniformWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
        uniformWrite.dstSet(basicDescriptorSet)
        uniformWrite.dstBinding(0)
        uniformWrite.dstArrayElement(0)
        uniformWrite.descriptorCount(1)
        uniformWrite.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
        uniformWrite.pBufferInfo(biUniforms)

        val samplerWrite = descriptorWrites[1]
        samplerWrite.`sType$Default`()
        samplerWrite.dstSet(basicDescriptorSet)
        samplerWrite.dstBinding(1)
        samplerWrite.dstArrayElement(0)
        samplerWrite.descriptorCount(1)
        samplerWrite.descriptorType(VK_DESCRIPTOR_TYPE_SAMPLER)
        samplerWrite.pImageInfo(iiSamplers)

        val storageWrite = descriptorWrites[2]
        storageWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
        storageWrite.dstSet(basicDescriptorSet)
        storageWrite.dstBinding(2)
        storageWrite.dstArrayElement(0)
        storageWrite.descriptorCount(1)
        storageWrite.descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
        storageWrite.pBufferInfo(biStorages)

        vkUpdateDescriptorSets(vkDevice, descriptorWrites, null)
        basicDescriptorSet
    }
}

fun updateBasicDynamicDescriptorSet(
    vkDevice: VkDevice, basicDynamicDescriptorSet: Long, colorImages: List<VulkanImage?>, heightImages: List<VulkanImage?>
) {
    if (colorImages.size > MAX_NUM_DESCRIPTOR_IMAGES) {
        throw IllegalArgumentException("Too many color images (${colorImages.size}): at most $MAX_NUM_DESCRIPTOR_IMAGES are allowed")
    }
    if (heightImages.size > MAX_NUM_DESCRIPTOR_IMAGES) {
        throw IllegalArgumentException("Too many height images (${heightImages.size}): at most $MAX_NUM_DESCRIPTOR_IMAGES are allowed")
    }

    stackPush().use { stack ->

        val iiColorImages = VkDescriptorImageInfo.calloc(MAX_NUM_DESCRIPTOR_IMAGES, stack)
        val backupColorImage = colorImages.firstOrNull { candidate -> candidate != null }
            ?: throw IllegalArgumentException("At least 1 color image is required")

        for (index in 0 until MAX_NUM_DESCRIPTOR_IMAGES) {
            val iiColorImage = iiColorImages[index]
            // sampler will be ignored
            if (index < colorImages.size) {
                val maybeImage = colorImages[index]
                if (maybeImage != null) {
                    iiColorImage.imageView(maybeImage.fullView!!)
                } else {
                    iiColorImage.imageView(backupColorImage.fullView!!)
                }
            } else {
                iiColorImage.imageView(backupColorImage.fullView!!)
            }
            iiColorImage.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
        }

        val iiHeightImages = VkDescriptorImageInfo.calloc(MAX_NUM_DESCRIPTOR_IMAGES, stack)
        val backupHeightImage = heightImages.firstOrNull { candidate -> candidate != null }
            ?: throw IllegalArgumentException("At least 1 height image is required")

        for (index in 0 until MAX_NUM_DESCRIPTOR_IMAGES) {
            val iiHeightImage = iiHeightImages[index]
            // sampler will be ignored
            iiHeightImage.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
            if (index < heightImages.size) {
                val maybeImage = heightImages[index]
                if (maybeImage != null) {
                    iiHeightImage.imageView(maybeImage.fullView!!)
                } else {
                    iiHeightImage.imageView(backupHeightImage.fullView!!)
                }
            } else {
                iiHeightImage.imageView(backupHeightImage.fullView!!)
            }
        }

        val descriptorWrites = VkWriteDescriptorSet.calloc(2, stack)

        val colorImageWrites = descriptorWrites[0]
        colorImageWrites.`sType$Default`()
        colorImageWrites.dstSet(basicDynamicDescriptorSet)
        colorImageWrites.dstBinding(0)
        colorImageWrites.dstArrayElement(0)
        colorImageWrites.descriptorCount(MAX_NUM_DESCRIPTOR_IMAGES)
        colorImageWrites.descriptorType(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
        colorImageWrites.pImageInfo(iiColorImages)

        val heightImageWrites = descriptorWrites[1]
        heightImageWrites.`sType$Default`()
        heightImageWrites.dstSet(basicDynamicDescriptorSet)
        heightImageWrites.dstBinding(1)
        heightImageWrites.dstArrayElement(0)
        heightImageWrites.descriptorCount(MAX_NUM_DESCRIPTOR_IMAGES)
        heightImageWrites.descriptorType(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
        heightImageWrites.pImageInfo(iiHeightImages)

        vkUpdateDescriptorSets(vkDevice, descriptorWrites, null)
    }
}

fun createBasicSampler(vkDevice: VkDevice): Long {
    return stackPush().use { stack ->
        val ciSampler = VkSamplerCreateInfo.calloc(stack)
        ciSampler.`sType$Default`()
        ciSampler.magFilter(VK_FILTER_LINEAR)
        ciSampler.minFilter(VK_FILTER_LINEAR)
        ciSampler.mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR)
        ciSampler.addressModeU(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
        ciSampler.addressModeV(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
        ciSampler.addressModeW(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
        ciSampler.mipLodBias(0f)
        // TODO Experiment with this
        ciSampler.anisotropyEnable(true)
        ciSampler.maxAnisotropy(8f)
        ciSampler.compareEnable(false)
        ciSampler.compareOp(VK_COMPARE_OP_ALWAYS)
        // TODO Change this after adding support for mipmapping
        ciSampler.minLod(0f)
        ciSampler.maxLod(0f)
        ciSampler.borderColor(VK_BORDER_COLOR_FLOAT_OPAQUE_WHITE) // We shouldn't hit the border anyway
        ciSampler.unnormalizedCoordinates(false)

        val pSampler = stack.callocLong(1)

        assertVkSuccess(
            vkCreateSampler(vkDevice, ciSampler, null, pSampler),
            "CreateSampler", "standard plug-in"
        )
        pSampler[0]
    }
}
