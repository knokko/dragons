package dragons.plugins.standard.vulkan.render.entity

import dragons.plugins.standard.vulkan.pipeline.MAX_NUM_DESCRIPTOR_IMAGES
import dragons.state.StaticGraphicsState
import dragons.vulkan.memory.VulkanImage
import org.lwjgl.util.vma.Vma.vmaDestroyImage
import org.lwjgl.vulkan.VK10.vkDestroyImageView
import org.lwjgl.vulkan.VkDevice
import java.util.*

internal class EntityImageTracker(
    private val graphicsState: StaticGraphicsState,
    private val maxNumPixels: Long
) {
    private val availableColorImages = LinkedList<Int>()
    private val availableHeightImages = LinkedList<Int>()

    private var currentNumPixels = 0L
    private val imageMap = mutableMapOf<UUID, TrackedImages>()

    init {
        for (imageIndex in 0 until MAX_NUM_DESCRIPTOR_IMAGES) {
            availableColorImages.add(imageIndex)
            availableHeightImages.add(imageIndex)
        }
    }

    fun startFrame() {
        for (images in this.imageMap.values) {
            images.age += 1
        }
    }

    private fun hasEnoughSpace(mesh: EntityMesh) = this.availableColorImages.size >= mesh.colorImages.size &&
            this.availableHeightImages.size >= mesh.heightImages.size &&
            this.currentNumPixels + getRequiredNumPixels(mesh) <= this.maxNumPixels

    private fun getRequiredNumPixels(mesh: EntityMesh) = mesh.colorImages.sumOf {
        4L * it.width * it.height
    } + mesh.heightImages.sumOf { it.width * it.height }

    fun useMesh(mesh: EntityMesh) {
        if (this.imageMap.containsKey(mesh.id)) {
            this.imageMap[mesh.id]!!.age = 0
        } else {

            var canAddMesh = this.hasEnoughSpace(mesh)

            if (!canAddMesh) {
                for (age in arrayOf(1000L, 100L, 10L, 1L)) {
                    this.removeOldImages(age)
                    if (this.hasEnoughSpace(mesh)) {
                        canAddMesh = true
                        break
                    }
                }
            }

            if (!canAddMesh) throw RuntimeException("Not enough space to store all entity images")

            val colorImages = mesh.colorImages.map {
                val (image, allocation) = it.create(this.graphicsState)
                TrackedImage(image, allocation, this.availableColorImages.pop())
            }

            val heightImages = mesh.heightImages.map {
                // TODO Create these images on some background thread
                val (image, allocation) = it.create(this.graphicsState)
                TrackedImage(image, allocation, this.availableHeightImages.pop())
            }

            this.imageMap[mesh.id] = TrackedImages(
                mesh = mesh, colorImages = colorImages, heightImages = heightImages
            )

            this.currentNumPixels += this.getRequiredNumPixels(mesh)
        }
    }

    fun getDescriptorIndices(mesh: EntityMesh): Pair<List<Int>, List<Int>> {
        val trackedImages = this.imageMap[mesh.id] ?: throw IllegalArgumentException("This mesh has not been used")

        return Pair(
            trackedImages.colorImages.map { it.descriptorIndex },
            trackedImages.heightImages.map { it.descriptorIndex }
        )
    }

    private fun getCurrentlyUsedImages(getImages: (TrackedImages) -> List<TrackedImage>): List<VulkanImage?> {
        val largestIndex = this.imageMap.values.maxOf { images -> getImages(images).maxOf { it.descriptorIndex } }
        val imageArray = Array<VulkanImage?>(largestIndex + 1) { null }
        for (images in this.imageMap.values) {
            for (image in getImages(images)) {
                if (imageArray[image.descriptorIndex] != null) throw IllegalStateException("Duplicate image index")
                imageArray[image.descriptorIndex] = image.image
            }
        }

        return imageArray.toList()
    }

    fun getCurrentlyUsedColorImages() = this.getCurrentlyUsedImages { it.colorImages }

    fun getCurrentlyUsedHeightImages() = this.getCurrentlyUsedImages { it.heightImages }

    private fun removeOldImages(thresholdAge: Long) {
        this.imageMap.values.removeIf { trackedImages ->
            if (trackedImages.age >= thresholdAge) {
                this.currentNumPixels -= this.getRequiredNumPixels(trackedImages.mesh)
                for (image in trackedImages.colorImages) {
                    this.availableColorImages.add(image.descriptorIndex)
                }
                for (image in trackedImages.heightImages) {
                    this.availableHeightImages.add(image.descriptorIndex)
                }
                trackedImages.destroy(graphicsState.boiler.vkDevice(), graphicsState.boiler.vmaAllocator())
                true
            } else {
                false
            }
        }
    }

    fun endFrame() {
        // Perhaps this will be useful later on
    }

    fun destroy() {
        for (images in this.imageMap.values) {
            images.destroy(graphicsState.boiler.vkDevice(), graphicsState.boiler.vmaAllocator())
        }
    }
}

private class TrackedImages(
    val mesh: EntityMesh,
    val colorImages: List<TrackedImage>,
    val heightImages: List<TrackedImage>
) {
    var age = 0L

    fun destroy(vkDevice: VkDevice, vmaAllocator: Long) {
        for (image in this.colorImages + this.heightImages) {
            vkDestroyImageView(vkDevice, image.image.fullView!!, null)
            vmaDestroyImage(vmaAllocator, image.image.handle, image.allocation)
        }
    }
}

private class TrackedImage(
    val image: VulkanImage,
    val allocation: Long,
    val descriptorIndex: Int
)
