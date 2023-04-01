package graviks2d

import graviks2d.resource.image.ImageReference
import graviks2d.resource.text.CharacterPosition
import graviks2d.resource.text.FontReference
import graviks2d.resource.text.TextStyle
import graviks2d.target.ChildTarget
import graviks2d.target.GraviksTarget
import graviks2d.util.Color
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestChildTarget {

    @Test
    fun testFillRect() {
        val parent = LoggedTarget()
        val child = ChildTarget(parent, 0.2f, 0.5f, 0.6f, 0.8f)
        val margin = 0.0001f
        child.fillRect(0.1f, 0.5f, 0.4f, 0.9f, Color.BLUE)

        assertEquals(0.24f, parent.x1, margin)
        assertEquals(0.65f, parent.y1, margin)
        assertEquals(0.36f, parent.x2, margin)
        assertEquals(0.77f, parent.y2, margin)
    }

    @Test
    fun testDrawRoundedRect() {
        val parent = LoggedTarget()
        val child = ChildTarget(parent, 0.2f, 0.5f, 0.6f, 0.8f)
        val margin = 0.0001f
        child.drawRoundedRect(0.1f, 0.5f, 0.4f, 0.9f, 0.1f, 0.01f, Color.BLUE)

        assertEquals(0.24f, parent.x1, margin)
        assertEquals(0.65f, parent.y1, margin)
        assertEquals(0.36f, parent.x2, margin)
        assertEquals(0.77f, parent.y2, margin)
        assertEquals(0.04f, parent.radiusX, margin)
    }

    @Test
    fun testDrawImage() {
        val parent = LoggedTarget()
        val child = ChildTarget(parent, 0.2f, 0.5f, 0.6f, 0.8f)
        val margin = 0.0001f
        child.drawImage(0.1f, 0.5f, 0.4f, 0.9f, ImageReference.classLoaderPath("nope", false))

        assertEquals(0.24f, parent.x1, margin)
        assertEquals(0.65f, parent.y1, margin)
        assertEquals(0.36f, parent.x2, margin)
        assertEquals(0.77f, parent.y2, margin)
    }

    @Test
    fun testGetImageSize() {
        val parent = LoggedTarget()
        val child = ChildTarget(parent, 0.2f, 0.5f, 0.6f, 0.8f)
        assertEquals(Pair(16, 64), child.getImageSize(ImageReference.classLoaderPath("test", false)))
    }

    @Test
    fun testDrawString() {
        val parent = LoggedTarget()
        val child = ChildTarget(parent, 0.2f, 0.5f, 0.6f, 0.8f)
        val margin = 0.0001f

        val result = child.drawString(
            0.1f, 0.2f, 0.5f, 0.8f, "T", TextStyle(
                fillColor = Color.GREEN, font = null
            )
        )

        assertEquals(0.24f, parent.x1, margin)
        assertEquals(0.56f, parent.y1, margin)
        assertEquals(0.4f, parent.x2, margin)
        assertEquals(0.74f, parent.y2, margin)

        assertEquals(1, result.size)
        assertEquals(0.15f, result[0].minX, margin)
        assertEquals(1f / 3f, result[0].minY, margin)
        assertEquals(0.375f, result[0].maxX, margin)
        assertEquals(2f / 3f, result[0].maxY, margin)
    }

    @Test
    fun testGetStringAspectRatio() {
        val parent = LoggedTarget()
        val child = ChildTarget(parent, 0.1f, 0.3f, 0.3f, 0.4f)

        assertEquals(5f, child.getStringAspectRatio("test", null), 0.01f)
    }

    @Test
    fun testGetAspectRatio() {
        val parent = LoggedTarget()
        val child = ChildTarget(parent, 0.1f, 0.3f, 0.3f, 0.4f)

        assertEquals(3f, child.getAspectRatio(), 0.01f)
    }
}

private class LoggedTarget : GraviksTarget {

    var x1 = 0f
    var y1 = 0f
    var x2 = 0f
    var y2 = 0f
    var radiusX = 0f

    override fun fillRect(x1: Float, y1: Float, x2: Float, y2: Float, color: Color) {
        this.x1 = x1
        this.y1 = y1
        this.x2 = x2
        this.y2 = y2
    }

    override fun drawRoundedRect(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        radiusX: Float,
        lineWidth: Float,
        color: Color
    ) {
        this.x1 = x1
        this.y1 = y1
        this.x2 = x2
        this.y2 = y2
        this.radiusX = radiusX
    }

    override fun drawImage(xLeft: Float, yBottom: Float, xRight: Float, yTop: Float, image: ImageReference) {
        this.x1 = xLeft
        this.y1 = yBottom
        this.x2 = xRight
        this.y2 = yTop
    }

    override fun getImageSize(image: ImageReference) = Pair(16, 64)

    override fun drawString(
        minX: Float,
        yBottom: Float,
        maxX: Float,
        yTop: Float,
        string: String,
        style: TextStyle,
        dryRun: Boolean
    ): List<CharacterPosition> {
        this.x1 = minX
        this.y1 = yBottom
        this.x2 = maxX
        this.y2 = yTop
        return listOf(CharacterPosition(0.26f, 0.6f, 0.35f, 0.7f, isLeftToRight = true))
    }

    override fun getStringAspectRatio(string: String, fontReference: FontReference?) = 5f

    override fun getAspectRatio() = 1.5f
}
