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

    private val codepointToGlyph = IntCache(size = 300, numSlotsPerIndex = 5, numLocks = 50)

    private val extraAdvanceCache = IntPairCache(size = 20_000, numSlotsPerIndex = 10, numLocks = 500)
    private val advanceWidthCache = IntCache(size = 300, numSlotsPerIndex = 5, numLocks = 50)
    private val glyphShapeCache = GlyphCache(size = 600, numSlotsPerIndex = 5, numLocks = 150)

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
        return extraAdvanceCache.getOrPut(previousCodepoint, nextCodepoint) {
            stbtt_GetGlyphKernAdvance(this.fontInfo, this.getGlyph(previousCodepoint), this.getGlyph(nextCodepoint))
        }
    }

    private fun getGlyph(codepoint: Int): Int = this.codepointToGlyph.getOrPut(codepoint) {
        var glyph = stbtt_FindGlyphIndex(this.fontInfo, codepoint)

        // If this codepoint is not supported, use the '?' character as a fallback
        if (glyph == 0) {
            if (codepoint == '?'.code) throw IllegalStateException("Font doesn't have a glyph for '?'")
            glyph = this.getGlyph('?'.code)
        }
        glyph
    }

    internal fun getAdvanceWidth(codepoint: Int): Int {
        // Special case: tab
        if (codepoint == '\t'.code) return 4 * getAdvanceWidth(' '.code)

        return advanceWidthCache.getOrPut(codepoint) {
            val glyph = this.getGlyph(codepoint)

            stackPush().use { stack ->
                val pAdvanceWidth = stack.mallocInt(1)
                val pLeftSideBearing = stack.mallocInt(1)
                stbtt_GetGlyphHMetrics(this.fontInfo, glyph, pAdvanceWidth, pLeftSideBearing)
                pAdvanceWidth[0]
            }
        }
    }

    internal fun borrowGlyphShape(codepoint: Int, useGlyph: (GlyphShape) -> Unit) {
        // Special case: tab
        if (codepoint == '\t'.code) {
            useGlyph(GlyphShape(fontInfo = this.fontInfo, ttfVertices = null, advanceWidth = this.getAdvanceWidth(codepoint)))
            return
        }

        glyphShapeCache.borrow(codepoint, {
            val glyph = this.getGlyph(codepoint)
            val shape = stbtt_GetGlyphShape(this.fontInfo, glyph)
            val advanceWidth = this.getAdvanceWidth(codepoint)

            GlyphShape(this.fontInfo, shape, advanceWidth)
        }, useGlyph)
    }

    fun destroy() {
        this.glyphShapeCache.destroy()
        this.fontInfo.free()
        memFree(this.rawTtfBuffer)
    }
}
