package graviks2d.target

import graviks2d.resource.image.ImageReference
import graviks2d.resource.text.CharacterPosition
import graviks2d.resource.text.FontReference
import graviks2d.resource.text.TextStyle
import graviks2d.util.Color
import troll.sync.WaitSemaphore

class ChildTarget(
    private val parent: GraviksTarget,
    private val minX: Float,
    private val minY: Float,
    private val maxX: Float,
    private val maxY: Float
): GraviksTarget {
    private fun transformX(x: Float) = this.minX + x * (this.maxX - this.minX)
    private fun transformY(y: Float) = this.minY + y * (this.maxY - this.minY)

    private fun <T> transform(x1: Float, y1: Float, x2: Float, y2: Float, drawFunction: (Float, Float, Float, Float) -> T): T {
        return drawFunction(transformX(x1), transformY(y1), transformX(x2), transformY(y2))
    }

    private fun <T> transformBack(x1: Float, y1: Float, x2: Float, y2: Float, backFunction: (Float, Float, Float, Float) -> T): T {
        fun transformBackX(x: Float) = (x - this.minX) / (this.maxX - this.minX)
        fun transformBackY(y: Float) = (y - this.minY) / (this.maxY - this.minY)

        return backFunction(transformBackX(x1), transformBackY(y1), transformBackX(x2), transformBackY(y2))
    }

    override fun setScissor(newScissor: GraviksScissor): GraviksScissor {
        val oldScissor = this.transform(newScissor.minX, newScissor.minY, newScissor.maxX, newScissor.maxY) { tx1, ty1, tx2, ty2 ->
            this.parent.setScissor(GraviksScissor(tx1, ty1, tx2, ty2))
        }

        return transformBack(oldScissor.minX, oldScissor.minY, oldScissor.maxX, oldScissor.maxY) { tx1, ty1, tx2, ty2 ->
            GraviksScissor(tx1, ty1, tx2, ty2)
        }
    }

    override fun getScissor(): GraviksScissor {
        val scissor = this.parent.getScissor()
        return transformBack(scissor.minX, scissor.minY, scissor.maxX, scissor.maxY) { tx1, ty1, tx2, ty2 ->
            GraviksScissor(tx1, ty1, tx2, ty2)
        }
    }

    override fun fillTriangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, color: Color) {
        this.parent.fillTriangle(transformX(x1), transformY(y1), transformX(x2), transformY(y2), transformX(x3), transformY(y3), color)
    }

    override fun fillRect(x1: Float, y1: Float, x2: Float, y2: Float, color: Color) {
        this.transform(x1, y1, x2, y2) { tx1, ty1, tx2, ty2 ->
            this.parent.fillRect(tx1, ty1, tx2, ty2, color)
        }
    }

    override fun drawRect(x1: Float, y1: Float, x2: Float, y2: Float, lineWidth: Float, color: Color) {
        this.transform(x1, y1, x2, y2) { tx1, ty1, tx2, ty2 ->
            this.parent.drawRect(tx1, ty1, tx2, ty2, lineWidth, color)
        }
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
        val dx = this.maxX - this.minX
        this.transform(x1, y1, x2, y2) { tx1, ty1, tx2, ty2 ->
            this.parent.drawRoundedRect(tx1, ty1, tx2, ty2, radiusX * dx,  lineWidth, color)
        }
    }

    override fun drawImage(xLeft: Float, yBottom: Float, xRight: Float, yTop: Float, image: ImageReference) {
        this.transform(xLeft, yBottom, xRight, yTop) { tLeft, tBottom, tRight, tTop ->
            this.parent.drawImage(tLeft, tBottom, tRight, tTop, image)
        }
    }

    override fun drawVulkanImage(
        xLeft: Float,
        yBottom: Float,
        xRight: Float,
        yTop: Float,
        vkImage: Long,
        vkImageView: Long
    ) {
        this.transform(xLeft, yBottom, xRight, yTop) { tLeft, tBottom, tRight, tTop ->
            this.parent.drawVulkanImage(tLeft, tBottom, tRight, tTop, vkImage, vkImageView)
        }
    }

    override fun addWaitSemaphore(semaphore: WaitSemaphore) {
        this.parent.addWaitSemaphore(semaphore)
    }

    override fun getImageSize(image: ImageReference) = parent.getImageSize(image)

    override fun drawString(
        minX: Float,
        yBottom: Float,
        maxX: Float,
        yTop: Float,
        string: String,
        style: TextStyle,
        dryRun: Boolean,
        suggestLeftToRight: Boolean
    ): List<CharacterPosition> {
        val result = mutableListOf<CharacterPosition>()
        this.transform(minX, yBottom, maxX, yTop) { tMinX, tBottom, tMaxX, tTop ->
            val childResult = this.parent.drawString(
                tMinX, tBottom, tMaxX, tTop, string, style, dryRun = dryRun, suggestLeftToRight = suggestLeftToRight
            )
            for (childArea in childResult) {
                transformBack(childArea.minX, childArea.minY, childArea.maxX, childArea.maxY) { bMinX, bMinY, bMaxX, bMaxY ->
                    transformBack(childArea.croppedMinX, childArea.minY, childArea.croppedMaxX, childArea.maxY)
                    { bCroppedMinX, _, bCroppedMaxX, _ ->
                        result.add(CharacterPosition(bMinX, bMinY, bMaxX, bMaxY, childArea.isLeftToRight, bCroppedMinX, bCroppedMaxX))
                    }
                }
            }
        }
        return result
    }

    override fun getStringAspectRatio(string: String, fontReference: FontReference?) = parent.getStringAspectRatio(string, fontReference)

    override fun getAspectRatio() = parent.getAspectRatio() * (maxX - minX) / (maxY - minY)
}
