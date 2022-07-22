package graviks2d.resource.image

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
                vmaAllocation = 0L, // The allocation is not handled by this cache
                width = image.customWidth!!,
                height = image.customHeight!!
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

            // If the image is not cached and the cache is full, we try to remove
            // a cached item that is no longer used. If multiple cached items are no
            // longer used, we remove the oldest one.
            if (this.cache.size >= this.softImageLimit) {
                var entryToRemove: CachedImage? = null
                for (cachedEntry in this.cache.values) {
                    if (cachedEntry.numberOfBorrows == 0 && (entryToRemove == null || cachedEntry.lastReturnTime < entryToRemove.lastReturnTime)) {
                        entryToRemove = cachedEntry
                    }
                }

                if (entryToRemove != null) {
                    this.cache.remove(entryToRemove.imageReference.id)
                    this.instance.coroutineScope.launch {
                        entryToRemove.destroy(instance)
                    }
                }
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
                    this.cache.remove(borrowedImage.imageReference.id)
                    this.instance.coroutineScope.launch {
                        cachedImage.destroy(instance)
                    }
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
            this.cache.clear()
        }
    }

    fun getCurrentCacheSize() = this.cache.size
}

internal class BorrowedImage(
    val imageReference: ImageReference,
    val imagePair: Deferred<ImagePair>
) {
    internal var wasReturned = false

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
    var lastReturnTime = 0L

    suspend fun destroy(instance: GraviksInstance) {
        val imagePair = this.imagePair.await()
        vkDestroyImageView(instance.device, imagePair.vkImageView, null)
        vmaDestroyImage(instance.vmaAllocator, imagePair.vkImage, imagePair.vmaAllocation)
    }
}
