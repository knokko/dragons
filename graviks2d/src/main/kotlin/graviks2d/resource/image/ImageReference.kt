package graviks2d.resource.image

import java.io.File

class ImageReference private constructor(
    internal val isSvg: Boolean,
    internal val path: String?,
    internal val file: File?
) {

    init {
        if (path != null && file != null) {
            throw Error("When path is non-null, file must be null")
        }
        if (path == null && file == null) {
            throw Error("Either path or file must be non-null")
        }
    }

    val id: String
    get() = if (this.file != null) {
                "[file]:${this.file.path}"
            } else {
                "[classloader]:${this.path}"
            }

    companion object {
        fun classLoaderPath(path: String, isSvg: Boolean) = ImageReference(
            isSvg = isSvg, path = path, file = null
        )

        fun file(file: File) = ImageReference(
            isSvg = file.name.endsWith(".svg"), path = null, file = file
        )
    }
}
