package gruviks.component.text

import graviks2d.resource.image.ImageReference
import graviks2d.target.GraviksTarget
import gruviks.component.*
import gruviks.event.CursorClickEvent
import gruviks.event.CursorEnterEvent
import gruviks.event.CursorLeaveEvent
import gruviks.event.Event

class TextButton(
    private var text: String,
    private var icon: ImageReference?,
    private var style: TextButtonStyle,
    var clickAction: (CursorClickEvent) -> Unit
) : Component() {
    override fun subscribeToEvents() {
        agent.subscribe(CursorClickEvent::class)
        agent.subscribe(CursorEnterEvent::class)
        agent.subscribe(CursorLeaveEvent::class)
    }

    override fun processEvent(event: Event) {
        if (event is CursorClickEvent) {
            clickAction(event)
        } else if (event is CursorEnterEvent || event is CursorLeaveEvent) {
            agent.didRequestRender = true
        } else {
            throw IllegalArgumentException("Unexpected event ${event::class.java}")
        }
    }

    override fun render(target: GraviksTarget, force: Boolean): RenderResult {
        val isHovering = agent.cursorTracker.getHoveringCursors().isNotEmpty()
        val textStyle = if (isHovering) { style.hoverTextStyle } else { style.baseTextStyle }

        val targetAspectRatio = target.getAspectRatio()
        val textAspectRatio = target.getStringAspectRatio(text, textStyle.font)
        val iconHeightFraction = 0.8f
        val icon = this.icon
        var iconAspectRatio = 0f

        if (icon != null) {
            val (iconWidth, iconHeight) = target.getImageSize(icon)
            iconAspectRatio = iconWidth.toFloat() / iconHeight.toFloat()
        }

        // The width if the height of the button were 1.0
        val referenceWidth = (1f + textAspectRatio + iconHeightFraction * iconAspectRatio) / targetAspectRatio

        val cornerRadiusY = if (referenceWidth <= 1f) { 0.5f } else { 0.5f / referenceWidth }
        val cornerRadiusX = cornerRadiusY / targetAspectRatio
        val finalWidth = if (referenceWidth <= 1f) { referenceWidth } else { 1f }

        val (minX, maxX) = computeBoundsX(0f, 1f, finalWidth, style.horizontalAlignment)
        val (minY, maxY) = computeBoundsY(0f, 1f, 2f * cornerRadiusY, style.verticalAlignment)

        val borderColor = if (isHovering) { style.hoverBorderColor } else { style.baseBorderColor }
        val backgroundColor = if (isHovering) { style.hoverBackgroundColor } else { style.baseBackgroundColor }

        val normalizedLineWidth = 0.05f

        if (backgroundColor.alpha > 0) {
            target.fillRoundedRect(minX, minY, maxX, maxY, cornerRadiusX, cornerRadiusY, backgroundColor)
        }
        if (borderColor.alpha > 0) {
            target.drawRoundedRect(minX, minY, maxX, maxY, cornerRadiusX, cornerRadiusY, normalizedLineWidth, borderColor)
        }

        var textMinX = minX + cornerRadiusX
        val textMaxX = maxX - cornerRadiusX

        if (icon != null) {
            val deltaY = maxY - minY
            val iconMinX = minX + cornerRadiusX * 0.6f
            val iconMinY = minY + deltaY * 0.5f * (1f - iconHeightFraction)
            val iconMaxY = maxY - deltaY * 0.5f * (1f - iconHeightFraction)
            val finalIconHeight = iconMaxY - iconMinY
            val iconMaxX = iconMinX + finalIconHeight / targetAspectRatio
            target.drawImage(iconMinX, iconMinY, iconMaxX, iconMaxY, icon)

            textMinX = iconMaxX + 0.01f
        }

        val dy = maxY - minY

        target.drawString(
            textMinX, minY + dy * normalizedLineWidth * 0.5f,
            textMaxX, maxY - dy * normalizedLineWidth * 0.5f,
            text, textStyle, backgroundColor
        )

        return RenderResult(
            drawnRegion = RoundedRectangularDrawnRegion(minX, minY, maxX, maxY, cornerRadiusX, cornerRadiusY),
            propagateMissedCursorEvents = true
        )
    }
}
