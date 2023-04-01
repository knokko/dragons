package gruviks.component.text

import graviks2d.resource.image.ImageReference
import graviks2d.target.GraviksTarget
import gruviks.component.*
import gruviks.event.CursorClickEvent
import gruviks.event.CursorEnterEvent
import gruviks.event.CursorLeaveEvent
import gruviks.event.Event
import gruviks.feedback.Feedback
import gruviks.feedback.RenderFeedback

class TextButton(
    private var text: String,
    private var icon: ImageReference?,
    private var style: TextButtonStyle,
    var clickAction: (CursorClickEvent, (Feedback) -> Unit) -> Unit
) : Component() {
    override fun subscribeToEvents() {
        agent.subscribe(CursorClickEvent::class)
        agent.subscribe(CursorEnterEvent::class)
        agent.subscribe(CursorLeaveEvent::class)
    }

    override fun processEvent(event: Event) {
        if (event is CursorClickEvent) {
            clickAction(event, agent.giveFeedback)
        } else if (event is CursorEnterEvent || event is CursorLeaveEvent) {
            agent.giveFeedback(RenderFeedback())
        } else {
            throw IllegalArgumentException("Unexpected event ${event::class.java}")
        }
    }

    override fun render(target: GraviksTarget, force: Boolean): RenderResult {
        val isHovering = agent.cursorTracker.getHoveringCursors().isNotEmpty()
        val textStyle = if (isHovering) { style.hoverTextStyle } else { style.baseTextStyle }

        val targetAspectRatio = target.getAspectRatio()
        val cornerRadiusX = 0.5f / targetAspectRatio
        val reservedRightSpace = cornerRadiusX * 0.8f
        val icon = this.icon
        val (renderIconWidth, reservedLeftSpace) = if (icon == null) Pair(0f, cornerRadiusX) else {
            val (iconWidth, iconHeight) = target.getImageSize(icon)
            val renderIconWidth = style.iconHeight * iconWidth / iconHeight / targetAspectRatio
            Pair(renderIconWidth, cornerRadiusX * 0.8f + renderIconWidth)
        }

        val textPositions = target.drawString(
            reservedLeftSpace, style.lineWidth, 1f - reservedRightSpace, 1f - style.lineWidth,
            text, textStyle, dryRun = true
        )

        val (textMinX, textMaxX) = if (textPositions.isEmpty()) {
            Pair(reservedLeftSpace, reservedLeftSpace + cornerRadiusX - reservedRightSpace)
        } else {
            Pair(textPositions.minOf { it.minX }, textPositions.maxOf { it.maxX })
        }

        val borderColor = if (isHovering) { style.hoverBorderColor } else { style.baseBorderColor }
        val backgroundColor = if (isHovering) { style.hoverBackgroundColor } else { style.baseBackgroundColor }

        val finalMinX = textMinX - reservedLeftSpace
        val finalMaxX = textMaxX + reservedRightSpace
        if (backgroundColor.alpha > 0) {
            target.fillRoundedRect(finalMinX, 0f, finalMaxX, 1f, cornerRadiusX, backgroundColor)
        }
        if (borderColor.alpha > 0 && backgroundColor.alpha < 255) {
            target.drawRoundedRect(finalMinX, 0f, finalMaxX, 1f, cornerRadiusX, style.lineWidth, borderColor)
        }

        if (icon != null) {
            val iconMaxX = textMinX - cornerRadiusX * 0.1f
            val iconMinX = iconMaxX - renderIconWidth
            val iconMinY = 0.5f - style.iconHeight * 0.5f
            val iconMaxY = 0.5f + style.iconHeight * 0.5f
            target.drawImage(iconMinX, iconMinY, iconMaxX, iconMaxY, icon)
        }

        target.drawString(
            reservedLeftSpace, style.lineWidth, 1f - reservedRightSpace, 1f - style.lineWidth,
            text, textStyle, dryRun = false
        )

        return RenderResult(
            drawnRegion = RoundedRectangularDrawnRegion(finalMinX, 0f, finalMaxX, 1f, cornerRadiusX, 0.5f),
            propagateMissedCursorEvents = true
        )
    }
}
