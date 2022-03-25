package graviks2d.resource

import graviks2d.core.GraviksInstance
import kotlinx.coroutines.*
import org.lwjgl.util.vma.Vma.vmaDestroyImage
import org.lwjgl.vulkan.VK10.vkDestroyImageView
import java.nio.file.Files

internal class ImageCache(
    private val instance: GraviksInstance,
    private val softImageLimit: Int
) {

    private val cache = mutableMapOf<String, CachedImage>()

    fun borrowImage(image: ImageReference): BorrowedImage {

        // Custom images don't need to be cached
        if (image.customVkImage != null) {

            val imagePair = ImagePair(
                vkImage = image.customVkImage,
                vkImageView = image.customVkImageView!!,
                vmaAllocation = 0L // The allocation is not handled by this cache
            )
            return BorrowedImage(
                imageReference = image, imagePair = CompletableDeferred(imagePair)
            )
        }

        return synchronized(this) {

            if (image.isSvg) {
                throw UnsupportedOperationException("Not yet implemented")
            }

            val cached = this.cache[image.id]

            if (cached != null) {
                cached.numberOfBorrows += 1
                return BorrowedImage(
                    imageReference = image, imagePair = cached.imagePair
                )
            }

            val imageInputStream = if (image.file != null) {
                Files.newInputStream(image.file.toPath())
            } else {
                this.javaClass.classLoader.getResourceAsStream(image.path!!)!!
            }

            val imagePair = instance.coroutineScope.async {
                val imagePair = createImagePair(instance, imageInputStream, image.id)
                imageInputStream.close()
                imagePair
            }

            this.cache[image.id] = CachedImage(1, image, imagePair)

            BorrowedImage(imageReference = image, imagePair = imagePair)
        }
    }

    fun returnImage(borrowedImage: BorrowedImage) {
        if (!borrowedImage.wasReturned && borrowedImage.imageReference.customVkImage == null) {
            synchronized(this) {
                borrowedImage.wasReturned = true

                val cachedImage = this.cache[borrowedImage.imageReference.id]!!
                cachedImage.numberOfBorrows -= 1
                cachedImage.lastReturnTime = System.nanoTime()
                if (cachedImage.numberOfBorrows == 0 && this.cache.size > this.softImageLimit) {
                    this.cache.remove(borrowedImage.imageReference.path)
                    runBlocking { cachedImage.destroy(instance) }
                }
            }
        }
    }

    fun destroy() {
        synchronized(this) {
            runBlocking {
                for (cachedImage in cache.values) {
                    cachedImage.destroy(instance)
                }
            }
        }
    }
}

internal class BorrowedImage(
    val imageReference: ImageReference,
    val imagePair: Deferred<ImagePair>
) {
    internal var wasReturned = true

    override fun equals(other: Any?): Boolean {
        return other is BorrowedImage && this.imageReference.id == other.imageReference.id
    }

    override fun hashCode(): Int {
        return this.imageReference.id.hashCode()
    }
}

private class CachedImage(
    var numberOfBorrows: Int,
    val imageReference: ImageReference,
    val imagePair: Deferred<ImagePair>
) {
    // TODO Actually use the lastReturnTime
    var lastReturnTime = 0L

    suspend fun destroy(instance: GraviksInstance) {
        val imagePair = this.imagePair.await()
        vkDestroyImageView(instance.device, imagePair.vkImageView, null)
        vmaDestroyImage(instance.vmaAllocator, imagePair.vkImage, imagePair.vmaAllocation)
    }
}
