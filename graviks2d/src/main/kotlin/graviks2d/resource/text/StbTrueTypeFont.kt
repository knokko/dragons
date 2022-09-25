package graviks2d.resource.text

import org.lwjgl.stb.STBTTFontinfo
import org.lwjgl.stb.STBTruetype.*
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memCalloc
import org.lwjgl.system.MemoryUtil.memFree
import java.io.InputStream
import java.lang.IllegalStateException
import java.nio.ByteBuffer

/**
 * Warning: Do NOT use this on untrusted font files!
 */
class StbTrueTypeFont(ttfInput: InputStream, closeTtfInput: Boolean) {

    private val fontInfo: STBTTFontinfo
    private val rawTtfBuffer: ByteBuffer

    val ascent: Int
    val descent: Int
    val lineGap: Int

    private val codepointToGlyph = mutableMapOf<Int, Int>()

    init {
        val ttfArray = ttfInput.readAllBytes()
        this.rawTtfBuffer = memCalloc(ttfArray.size)
        this.rawTtfBuffer.put(0, ttfArray)

        this.fontInfo = STBTTFontinfo.calloc()
        if (!stbtt_InitFont(fontInfo, this.rawTtfBuffer)) {
            throw IllegalArgumentException("Failed to initialise StbTT font. Most likely, the given font data is invalid.")
        }

        stackPush().use { stack ->
            val pAscent = stack.callocInt(1)
            val pDescent = stack.callocInt(1)
            val pLineGap = stack.callocInt(1)

            stbtt_GetFontVMetrics(fontInfo, pAscent, pDescent, pLineGap)

            this.ascent = pAscent[0]
            this.descent = pDescent[0]
            this.lineGap = pLineGap[0]
        }

        if (closeTtfInput) {
            ttfInput.close()
        }
    }

    internal fun getExtraAdvance(previousCodepoint: Int, nextCodepoint: Int): Int {
        return stbtt_GetGlyphKernAdvance(this.fontInfo, this.getGlyph(previousCodepoint), this.getGlyph(nextCodepoint))
    }

    private fun getGlyph(codepoint: Int): Int {
        val cachedGlyph = this.codepointToGlyph[codepoint]
        if (cachedGlyph != null) return cachedGlyph

        var glyph = stbtt_FindGlyphIndex(this.fontInfo, codepoint)

        // If this codepoint is not supported, use the '?' character as a fallback
        if (glyph == 0) {
            if (codepoint == '?'.code) throw IllegalStateException("Font doesn't have a glyph for '?'")
            glyph = this.getGlyph('?'.code)
        }

        this.codepointToGlyph[codepoint] = glyph
        return glyph
    }

    internal fun getAdvanceWidth(codepoint: Int): Int {
        val glyph = this.getGlyph(codepoint)

        return stackPush().use { stack ->
            val pAdvanceWidth = stack.callocInt(1)
            val pLeftSideBearing = stack.callocInt(1)
            stbtt_GetGlyphHMetrics(this.fontInfo, glyph, pAdvanceWidth, pLeftSideBearing)
            pAdvanceWidth[0]
        }
    }

    internal fun getGlyphShape(codepoint: Int): GlyphShape {
        val glyph = this.getGlyph(codepoint)

        val shape = stbtt_GetGlyphShape(this.fontInfo, glyph)
        val (advanceWidth, leftSideBearing) = stackPush().use { stack ->
            val pAdvanceWidth = stack.callocInt(1)
            val pLeftSideBearing = stack.callocInt(1)
            stbtt_GetGlyphHMetrics(this.fontInfo, glyph, pAdvanceWidth, pLeftSideBearing)
            Pair(pAdvanceWidth[0], pLeftSideBearing[0])
        }

        return GlyphShape(this.fontInfo, shape, advanceWidth, leftSideBearing)
    }

    fun destroy() {
        this.fontInfo.free()
        memFree(this.rawTtfBuffer)
    }
}
