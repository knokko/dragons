package gruviks.component.text

import graviks2d.resource.text.CharacterPosition
import graviks2d.target.GraviksTarget
import graviks2d.util.Color
import gruviks.component.Component
import gruviks.component.RectangularDrawnRegion
import gruviks.component.RenderResult
import gruviks.event.*
import gruviks.feedback.ReleaseKeyboardFocusFeedback
import gruviks.feedback.RenderFeedback
import gruviks.feedback.RequestKeyboardFocusFeedback
import kotlin.math.min
import kotlin.math.max

class TextField(
        private val placeholder: String,
        initialText: String,
        private var currentStyle: TextFieldStyle,
) : Component() {

    private val textInput = TextInput(initialText) { agent.giveFeedback(RenderFeedback()) }

    override fun subscribeToEvents() {
        agent.subscribe(KeyboardFocusAcquiredEvent::class)
        agent.subscribe(KeyboardFocusLostEvent::class)
        agent.subscribe(KeyTypeEvent::class)
        agent.subscribe(KeyPressEvent::class)
        agent.subscribe(CursorClickEvent::class)
    }

    override fun processEvent(event: Event) {
        if (event is KeyTypeEvent) {
            textInput.type(event.codePoint)
        } else if (event is KeyPressEvent) {
            when (event.key.type) {
                KeyType.Escape -> agent.giveFeedback(ReleaseKeyboardFocusFeedback())
                // TODO Switch to the next text field?
                KeyType.Enter -> agent.giveFeedback(ReleaseKeyboardFocusFeedback())
                KeyType.Tab -> agent.giveFeedback(ReleaseKeyboardFocusFeedback())
                KeyType.Backspace -> textInput.backspace()
                KeyType.Right -> textInput.moveRight()
                KeyType.Left -> textInput.moveLeft()
                else -> {}
            }
        } else if (event is CursorClickEvent) {
            textInput.moveTo(event.position.x)
            agent.giveFeedback(RequestKeyboardFocusFeedback())
        } else if (event is KeyboardFocusEvent) {
            agent.giveFeedback(RenderFeedback())
        } else {
            throw UnsupportedOperationException("Unexpected event $event")
        }
    }

    fun getText() = textInput.getText()

    fun getStyle() = currentStyle

    fun setText(newText: String) {
        textInput.setText(newText)
    }

    fun setStyle(newStyle: TextFieldStyle) {
        this.currentStyle = newStyle
        agent.giveFeedback(RenderFeedback())
    }

    override fun render(target: GraviksTarget, force: Boolean): RenderResult {
        val hasFocus = agent.hasKeyboardFocus()
        val (textStyle, textRegion, drawPlaceholder) = if (hasFocus) currentStyle.drawFocusBackground(target, placeholder)
        else currentStyle.drawDefaultBackground(target, placeholder)

        textInput.drawnCharacterPositions = target.drawString(
            textRegion.minX, textRegion.minY, textRegion.maxX, textRegion.maxY, textInput.getText(), textStyle
        )

        val finalDrawnCharacterPositions = if (textInput.drawnCharacterPositions.isEmpty() && drawPlaceholder) {
            target.drawString(textRegion.minX, textRegion.minY, textRegion.maxX, textRegion.maxY, placeholder, textStyle)
        } else textInput.drawnCharacterPositions

        return if (hasFocus) {
            val caretCharIndex = min(textInput.drawnCharacterPositions.size - 1, textInput.getCaretPosition())
            val flip = textInput.getCaretPosition() >= textInput.drawnCharacterPositions.size

            val characterPosition = if (textInput.drawnCharacterPositions.isEmpty()) CharacterPosition(
                textRegion.minX, 0f, textRegion.maxX, 0f, false
            )
            else textInput.drawnCharacterPositions[caretCharIndex]

            val caretWidth = target.getStringAspectRatio("|", textStyle.font) / target.getAspectRatio() / 4
            var caretX = if (characterPosition.isLeftToRight == flip) {
                characterPosition.maxX - caretWidth / 2
            } else characterPosition.minX - caretWidth / 2
            caretX = max(caretX, textRegion.minX)
            caretX = min(caretX, textRegion.maxX - caretWidth)

            target.fillRect(caretX, textRegion.minY, caretX + caretWidth, textRegion.maxY, Color.RED)
            val renderedTextPosition = if (finalDrawnCharacterPositions.isEmpty()) RectangularDrawnRegion(
                caretX, textRegion.minY, caretX + caretWidth, textRegion.maxY
            ) else RectangularDrawnRegion(
                minX = min(finalDrawnCharacterPositions.minOf { it.minX }, caretX),
                minY = finalDrawnCharacterPositions.minOf { it.minY },
                maxX = max(finalDrawnCharacterPositions.maxOf { it.maxX }, caretX + caretWidth),
                maxY = finalDrawnCharacterPositions.maxOf { it.maxY }
            )
            currentStyle.computeFocusResult(renderedTextPosition)
        } else {
            val renderedTextPosition = if (finalDrawnCharacterPositions.isEmpty()) null else RectangularDrawnRegion(
                minX = finalDrawnCharacterPositions.minOf { it.minX },
                minY = finalDrawnCharacterPositions.minOf { it.minY },
                maxX = finalDrawnCharacterPositions.maxOf { it.maxX },
                maxY = finalDrawnCharacterPositions.maxOf { it.maxY }
            )
            currentStyle.computeDefaultResult(renderedTextPosition)
        }
    }
}
