package graviks2d.playground

import graviks2d.font.StbFont
import graviks2d.font.TrueTypeFont
import java.io.DataInputStream

fun main() {
    val dataInput = DataInputStream(TrueTypeFont::class.java.classLoader.getResourceAsStream("graviks2d/fonts/MainFont.ttf")!!)
    //TrueTypeFont(dataInput)
    StbFont(dataInput)
    dataInput.close()
}
