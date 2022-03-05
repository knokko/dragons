package graviks2d.font

import org.lwjgl.BufferUtils.createByteBuffer
import org.lwjgl.stb.STBTTBakedChar
import org.lwjgl.stb.STBTTFontinfo
import org.lwjgl.stb.STBTruetype.*
import org.lwjgl.system.MemoryStack.stackPush
import java.awt.Color
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.io.File
import java.io.InputStream
import javax.imageio.ImageIO

class StbFont(ttfInput: InputStream) {

    init {
        val ttfArray = ttfInput.readAllBytes()
        val ttfBuffer = createByteBuffer(ttfArray.size)
        ttfBuffer.put(0, ttfArray)

        val fontInfo = STBTTFontinfo.create()
        if (!stbtt_InitFont(fontInfo, ttfBuffer)) {
            throw RuntimeException("Uh ooh")
        }

        val (ascent, descent, lineGap) = stackPush().use { stack ->
            val pAscent = stack.callocInt(1)
            val pDescent = stack.callocInt(1)
            val pLineGap = stack.callocInt(1)

            stbtt_GetFontVMetrics(fontInfo, pAscent, pDescent, pLineGap)

            Triple(pAscent[0], pDescent[0], pLineGap[0])
        }

        println("Ascent is $ascent and descent is $descent and lineGap is $lineGap")

        val resultWidth = 256
        val resultHeight = 256
        val resultBuffer = createByteBuffer(resultWidth * resultHeight)

        val firstChar = "ﺵ".codePointAt(0)
        val numChars = 1
        val charData = STBTTBakedChar.create(numChars)

        println("BaseFontBitMap returned ${stbtt_BakeFontBitmap(ttfBuffer, 100f, resultBuffer, resultWidth, resultHeight, firstChar, charData)}")
//        val testGlyph = stbtt_FindGlyphIndex(fontInfo, 'h'.code)
//        println("testGlyph is $testGlyph")
//        val x0: Int
//        val y0: Int
//        val x1: Int
//        val y1: Int
//
//        val scaleX = 0.1f
//        val scaleY = 0.1f
//        stackPush().use { stack ->
//            val px0 = stack.callocInt(1)
//            val py0 = stack.callocInt(1)
//            val px1 = stack.callocInt(1)
//            val py1 = stack.callocInt(1)
//            stbtt_GetGlyphBitmapBox(fontInfo, testGlyph, scaleX, scaleY, px0, py0, px1, py1)
//            x0 = px0[0]
//            y0 = py0[0]
//            x1 = px1[0]
//            y1 = py1[0]
//        }
//
//        println("box is ($x0, $y0) to ($x1, $y1)")
//        println("MakeGlyphBitMap returned ${stbtt_MakeGlyphBitmap(fontInfo, resultBuffer, resultWidth, resultHeight, resultWidth, scaleX, scaleY, testGlyph)}")

        for (charCounter in 0 until numChars) {
            charData[charCounter].run {
                println("coordinates are (${x0()}, ${y0()}) to (${x1()}, ${y1()}) and advance is ${xadvance()} and off is (${xoff()}, ${yoff()})")

            }
        }

        val resultImage = BufferedImage(resultWidth, resultHeight, TYPE_INT_ARGB)
        for (x in 0 until resultWidth) {
            for (y in 0 until resultHeight) {
                val index = x + y * resultWidth
                val value = resultBuffer[index].toUByte().toInt()
                resultImage.setRGB(x, y, Color(value, value, value, 255).rgb)
            }
        }

        ImageIO.write(resultImage, "PNG", File("testImage.png"))
    }
}
