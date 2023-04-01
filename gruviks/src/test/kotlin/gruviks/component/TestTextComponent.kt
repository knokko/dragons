package gruviks.component

import graviks2d.resource.text.TextStyle
import graviks2d.util.Color
import gruviks.component.text.TextComponent
import gruviks.util.DummyGraviksTarget
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TestTextComponent {

    private val style = TextStyle(
        fillColor = Color.BLACK, font = null
    )

    @Test
    fun testRenderEmptyString() {
        val component = TextComponent("", style)
        val renderResult = component.render(DummyGraviksTarget(), false)
        assertNull(renderResult.drawnRegion)
        assertNull(renderResult.recentDrawnRegion)
    }

    @Test
    fun testRenderBasic() {
        val component = TextComponent("Hello, world!", style)
        val renderResult = component.render(DummyGraviksTarget(), false)
        assertTrue(renderResult.drawnRegion is RectangularDrawnRegion)
        assertTrue(renderResult.recentDrawnRegion is RectangularDrawnRegion)
    }
}
