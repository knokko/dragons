package graviks2d.resource.text

import org.lwjgl.stb.STBTTFontinfo
import org.lwjgl.stb.STBTTVertex
import org.lwjgl.stb.STBTruetype.stbtt_FreeShape

internal class GlyphShape(
    private val fontInfo: STBTTFontinfo,
    val ttfVertices: STBTTVertex.Buffer?,
    val advanceWidth: Int,
    val leftSideBearing: Int
) {
    fun destroy() {
        if (this.ttfVertices != null) {
            stbtt_FreeShape(this.fontInfo, this.ttfVertices)
        }
    }
}