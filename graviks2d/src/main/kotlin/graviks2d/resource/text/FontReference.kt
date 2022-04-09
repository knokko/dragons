package graviks2d.resource.text

import java.io.File

class FontReference private constructor(
    internal val file: File?,
    internal val classLoaderPath: String?
) {
    internal val id: String
    get() {
        return if (this.file != null) "file:${file.absolutePath}"
        else "classpath:${classLoaderPath!!}"
    }

    init {
        if ((file == null) != (classLoaderPath == null)) {
            throw UnsupportedOperationException("Exactly 1 of file and classLoaderPath must be non-null")
        }
    }

    companion object {
        fun fromFile(file: File) = FontReference(file = file, classLoaderPath = null)

        fun fromClassLoaderPath(path: String) = FontReference(file = null, classLoaderPath = path)
    }
}
