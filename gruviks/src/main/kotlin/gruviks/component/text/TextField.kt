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
import kotlin.text.StringBuilder

class TextField(
    private val placeholder: String,
    private var currentText: String,
    private var currentStyle: TextFieldStyle
) : Component() {

    private lateinit var drawnCharacterPositions: List<CharacterPosition>
    private var caretPosition = 0

    override fun subscribeToEvents() {
        agent.subscribe(KeyboardFocusAcquiredEvent::class)
        agent.subscribe(KeyboardFocusLostEvent::class)
        agent.subscribe(KeyTypeEvent::class)
        agent.subscribe(KeyPressEvent::class)
        agent.subscribe(CursorClickEvent::class)
    }

    override fun processEvent(event: Event) {
        if (event is KeyTypeEvent) {
            val newCodepoints = if (caretPosition < currentText.length) {
                val oldCodepoints = currentText.codePoints().toArray()
                val newCodepoints = StringBuilder(currentText.length + 2)

                for ((oldIndex, oldCodepoint) in oldCodepoints.withIndex()) {
                    if (oldIndex == caretPosition) {
                        newCodepoints.appendCodePoint(event.codePoint)
                    }
                    newCodepoints.appendCodePoint(oldCodepoint)
                }
                newCodepoints
            } else {
                val newCodepoints = StringBuilder(currentText)
                newCodepoints.appendCodePoint(event.codePoint)
                newCodepoints
            }

            caretPosition += 1
            setText(newCodepoints.toString())
        } else if (event is KeyPressEvent) {
            when (event.key.type) {
                KeyType.Escape -> agent.giveFeedback(ReleaseKeyboardFocusFeedback())
                // TODO Switch to the next text field?
                KeyType.Enter -> agent.giveFeedback(ReleaseKeyboardFocusFeedback())
                KeyType.Tab -> agent.giveFeedback(ReleaseKeyboardFocusFeedback())
                KeyType.Backspace -> {
                    if (caretPosition > 0) {

                        val oldCodepoints = currentText.codePoints().toArray()
                        val newTextBuilder = StringBuilder()
                        for ((index, codepoint) in oldCodepoints.withIndex()) {
                            if (index != caretPosition - 1) newTextBuilder.appendCodePoint(codepoint)
                        }

                        caretPosition -= 1
                        setText(newTextBuilder.toString())
                    }
                }
                KeyType.Right -> {
                    if (drawnCharacterPositions.isNotEmpty()) {
                        if (caretPosition < drawnCharacterPositions.size) {
                            val currentMinX = drawnCharacterPositions[caretPosition].minX
                            val nextPosition = drawnCharacterPositions.filter { it.minX > currentMinX }.minByOrNull { it.minX }
                            if (nextPosition != null) {
                                caretPosition = drawnCharacterPositions.indexOf(nextPosition)
                                agent.giveFeedback(RenderFeedback())
                            } else if (caretPosition == drawnCharacterPositions.size - 1 && drawnCharacterPositions[caretPosition].isLeftToRight) {
                                caretPosition = drawnCharacterPositions.size
                                agent.giveFeedback(RenderFeedback())
                            }
                        } else if (!drawnCharacterPositions[drawnCharacterPositions.size - 1].isLeftToRight) {
                            caretPosition -= 1
                            agent.giveFeedback(RenderFeedback())
                        }
                    }
                }
                KeyType.Left -> {
                    if (drawnCharacterPositions.isNotEmpty()) {
                        if (caretPosition < drawnCharacterPositions.size) {
                            val currentMinX = drawnCharacterPositions[caretPosition].minX
                            val nextPosition = drawnCharacterPositions.filter { it.minX < currentMinX }.maxByOrNull { it.minX }
                            if (nextPosition != null) {
                                caretPosition = drawnCharacterPositions.indexOf(nextPosition)
                                agent.giveFeedback(RenderFeedback())
                            } else if (caretPosition == drawnCharacterPositions.size - 1 && !drawnCharacterPositions[caretPosition].isLeftToRight){
                                caretPosition = drawnCharacterPositions.size
                                agent.giveFeedback(RenderFeedback())
                            }
                        } else if (drawnCharacterPositions[drawnCharacterPositions.size - 1].isLeftToRight) {
                            caretPosition -= 1
                            agent.giveFeedback(RenderFeedback())
                        }
                    }
                }
                else -> {}
            }
        } else if (event is CursorClickEvent) {
            val clickedIndex = this.drawnCharacterPositions.indexOfFirst {
                it.minX <= event.position.x && it.maxX >= event.position.x
            }
            val numCodepoints = currentText.codePointCount(0, currentText.length)
            this.caretPosition = if (clickedIndex != -1 && clickedIndex < numCodepoints) {
                val isRightToLeft = !this.drawnCharacterPositions[clickedIndex].isLeftToRight
                val clickedPosition = this.drawnCharacterPositions[clickedIndex]
                val clickedOnLeftSide = event.position.x - clickedPosition.minX <= clickedPosition.maxX - event.position.x
                if (clickedOnLeftSide != isRightToLeft) {
                    clickedIndex
                } else {
                    clickedIndex + 1
                }
            } else numCodepoints

            agent.giveFeedback(RequestKeyboardFocusFeedback())
        } else if (event is KeyboardFocusEvent) {
            agent.giveFeedback(RenderFeedback())
        } else {
            throw UnsupportedOperationException("Unexpected event $event")
        }
    }

    fun getText() = currentText

    fun getStyle() = currentStyle

    fun setText(newText: String) {
        this.currentText = newText
        this.caretPosition = min(this.caretPosition, this.currentText.codePointCount(0, this.currentText.length))
        agent.giveFeedback(RenderFeedback())
    }

    fun setStyle(newStyle: TextFieldStyle) {
        this.currentStyle = newStyle
        agent.giveFeedback(RenderFeedback())
    }

    override fun render(target: GraviksTarget, force: Boolean): RenderResult {
        val hasFocus = agent.hasKeyboardFocus()
        val (textStyle, textRegion, drawPlaceholder) = if (hasFocus) currentStyle.drawFocusBackground(target, placeholder)
        else currentStyle.drawDefaultBackground(target, placeholder)

        this.drawnCharacterPositions = target.drawString(
            textRegion.minX, textRegion.minY, textRegion.maxX, textRegion.maxY, currentText, textStyle
        )

        val finalDrawnCharacterPositions = if (this.drawnCharacterPositions.isEmpty() && drawPlaceholder) {
            target.drawString(textRegion.minX, textRegion.minY, textRegion.maxX, textRegion.maxY, placeholder, textStyle)
        } else this.drawnCharacterPositions

        return if (hasFocus) {
            val caretCharIndex = min(drawnCharacterPositions.size - 1, caretPosition)
            val flip = caretPosition >= drawnCharacterPositions.size

            val characterPosition = if (drawnCharacterPositions.isEmpty()) CharacterPosition(
                textRegion.minX, 0f, textRegion.maxX, 0f, false
            )
            else drawnCharacterPositions[caretCharIndex]

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
