package graviks2d.resource.image

import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDevice
import java.io.File

class ImageReference private constructor(
    internal val isSvg: Boolean,
    internal val path: String?,
    internal val file: File?,
    internal val customVkImage: Long?,
    internal var customVkImageView: Long?,
    internal val customWidth: Int?,
    internal val customHeight: Int?,
    internal val vkDevice: VkDevice?
) {

    init {
        if (path != null && (file != null || customVkImage != null)) {
            throw Error("When path is non-null, file and customVkImage must be null")
        }
        if (file != null && customVkImage != null) {
            throw Error("When file is non-null, path and customVkImage must be null")
        }
        if (path == null && file == null && customVkImage == null) {
            throw Error("One of path, file, and customVkImage must be non-null")
        }

        if (customVkImage != null && customVkImageView == null) {
            if (vkDevice == null) {
                throw Error("If customVkImage is not null and customVkImageView is null, vkDevice must be non-null")
            }

            this.customVkImageView = createImageView(vkDevice, customVkImage)
        }

        if (customVkImage != null && (customWidth == null || customHeight == null)) {
            throw Error("When customVkImage is not null, customWidth and customHeight must be non-null")
        }
    }

    val id: String
    get() = if (this.file != null) {
                "[file]:${this.file.path}"
            } else if (this.path != null) {
                "[classloader]:${this.path}"
            } else {
                "[custom]:${this.customVkImageView}"
            }

    fun destroy() {
        if (vkDevice != null) {
            vkDestroyImageView(vkDevice, customVkImageView!!, null)
            customVkImageView = null
        }
    }

    companion object {
        fun classLoaderPath(path: String, isSvg: Boolean) = ImageReference(
            isSvg = isSvg, path = path, file = null,
            customVkImage = null, customVkImageView = null, vkDevice = null,
            customWidth = null, customHeight = null
        )

        fun file(file: File) = ImageReference(
            isSvg = file.name.endsWith(".svg"), path = null, file = file,
            customVkImage = null, customVkImageView = null, vkDevice = null,
            customWidth = null, customHeight = null
        )

        fun custom(vkImage: Long, vkImageView: Long?, vkDevice: VkDevice? = null, width: Int, height: Int) = ImageReference(
            isSvg = false, path = null, file = null,
            customVkImage = vkImage, customVkImageView = vkImageView,
            vkDevice = if (vkImageView == null) { vkDevice } else { null },
            customWidth = width, customHeight = height
        )
    }
}
