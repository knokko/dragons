package graviks2d.resource.text

import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap

internal class FontManager(
    defaultFont: FontReference
) {
    private val defaultFont = loadFont(defaultFont)
    private val fonts = ConcurrentHashMap<String, StbTrueTypeFont>()

    fun getFont(reference: FontReference?): StbTrueTypeFont {
        if (reference == null) return this.defaultFont

        return fonts.computeIfAbsent(reference.id) { loadFont(reference) }
    }
}

private fun loadFont(reference: FontReference): StbTrueTypeFont {
    val fontInput = if (reference.file != null) {
        Files.newInputStream(reference.file.toPath())
    } else {
        FontManager::class.java.classLoader.getResourceAsStream(reference.classLoaderPath!!)
            ?: throw IllegalArgumentException("Can't get resource ${reference.classLoaderPath}")
    }
    return StbTrueTypeFont(fontInput, true)
}
