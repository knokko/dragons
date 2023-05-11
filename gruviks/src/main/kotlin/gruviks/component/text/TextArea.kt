package gruviks.component.text

import graviks2d.resource.text.CharacterPosition
import graviks2d.resource.text.TextStyle
import graviks2d.target.GraviksTarget
import graviks2d.util.Color
import gruviks.component.Component
import gruviks.component.RectangularDrawnRegion
import gruviks.component.RenderResult
import gruviks.event.*
import gruviks.feedback.ReleaseKeyboardFocusFeedback
import gruviks.feedback.RenderFeedback
import gruviks.feedback.RequestKeyboardFocusFeedback
import kotlin.math.max
import kotlin.math.min

class TextArea(
        private val initialText: String,
        private val lineHeight: Float,
        private val textStyle: TextStyle
): Component() {

    private lateinit var lineInputs: MutableList<TextInput>
    private var caretLine: Int? = null

    override fun subscribeToEvents() {
        agent.subscribe(KeyboardFocusAcquiredEvent::class)
        agent.subscribe(KeyboardFocusLostEvent::class)
        agent.subscribe(KeyTypeEvent::class)
        agent.subscribe(KeyPressEvent::class)
        agent.subscribe(CursorClickEvent::class)
    }

    override fun processEvent(event: Event) {
        if (event is KeyboardFocusAcquiredEvent || event is KeyboardFocusLostEvent) {
            agent.giveFeedback(RenderFeedback())
        }

        if (event is CursorClickEvent) {

            agent.giveFeedback(RequestKeyboardFocusFeedback())

            var newCaretLine = ((1f - event.position.y) / lineHeight).toInt()
            if (newCaretLine < 0) newCaretLine = 0
            if (newCaretLine >= lineInputs.size) newCaretLine = lineInputs.size - 1

            lineInputs[newCaretLine].moveTo(event.position.x)
            if (caretLine == null || caretLine != newCaretLine) {
                caretLine = newCaretLine
            }

            agent.giveFeedback(RenderFeedback())
        }

        if (event is KeyTypeEvent) {
            val caretLine = this.caretLine
            if (caretLine != null && caretLine < lineInputs.size) {
                lineInputs[caretLine].type(event.codePoint)
            }
        }

        if (event is KeyPressEvent) {
            if (event.key.type == KeyType.Escape) agent.giveFeedback(ReleaseKeyboardFocusFeedback())

            if (caretLine != null) {
                val textInput = lineInputs[caretLine!!]
                when (event.key.type) {
                    KeyType.Tab -> textInput.type('\t'.code)
                    KeyType.Enter -> {
                        val oldLine = textInput.getText()

                        val newLine = oldLine.substring(textInput.getCaretPosition())
                        textInput.setText(oldLine.substring(0, textInput.getCaretPosition()))
                        lineInputs.add(1 + caretLine!!, TextInput(newLine) { agent.giveFeedback(RenderFeedback()) })
                        caretLine = caretLine!! + 1
                        agent.giveFeedback(RenderFeedback())
                    }
                    KeyType.Backspace -> {
                        if (textInput.getCaretPosition() > 0) {
                            textInput.backspace()
                        } else if (caretLine!! > 0) {
                            val removedLine = lineInputs.removeAt(caretLine!!)
                            caretLine = caretLine!! - 1
                            lineInputs[caretLine!!].moveToEnd()
                            if (removedLine.getText().isNotEmpty()) {
                                lineInputs[caretLine!!].setText(lineInputs[caretLine!!].getText() + removedLine.getText())
                            }
                            agent.giveFeedback(RenderFeedback())
                        }
                    }
                    KeyType.Right -> textInput.moveRight()
                    KeyType.Left -> textInput.moveLeft()
                    KeyType.Up -> {
                        if (caretLine!! > 0) {
                            caretLine = caretLine!! - 1
                            lineInputs[caretLine!!].moveTo(textInput.estimateCaretX())
                            agent.giveFeedback(RenderFeedback())
                        }
                    }
                    KeyType.Down -> {
                        if (caretLine!! < lineInputs.size - 1) {
                            caretLine = caretLine!! + 1
                            lineInputs[caretLine!!].moveTo(textInput.estimateCaretX())
                            agent.giveFeedback(RenderFeedback())
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun splitLines(text: String) = text.replace("\r\n", "\n").replace("\n\r", "\n")
            .replace("\r", "\n").split('\n')

    override fun render(target: GraviksTarget, force: Boolean): RenderResult {
        target.fillRect(0f, 0f, 1f, 1f, Color.WHITE)

        if (!::lineInputs.isInitialized) {
            lineInputs = splitLines(initialText).map { TextInput(it) { agent.giveFeedback(RenderFeedback()) } }.toMutableList()
        }

        var maxY = 1f
        for ((lineIndex, line) in lineInputs.withIndex()) {
            if (maxY < 0f) break

            val minY = maxY - lineHeight
            val drawnCharPositions = target.drawString(0f, minY, 1f, maxY, line.getText(), textStyle)
            line.drawnCharacterPositions = drawnCharPositions.map { originalPosition -> CharacterPosition(
                    minX = originalPosition.minX, minY = (originalPosition.minY - minY) / lineHeight,
                    maxX = originalPosition.maxX, maxY = (originalPosition.maxY - minY) / lineHeight,
                    isLeftToRight = originalPosition.isLeftToRight
            ) }

            if (caretLine == lineIndex) {
                val caretCharIndex = min(line.drawnCharacterPositions.size - 1, line.getCaretPosition())
                val flip = line.getCaretPosition() >= line.drawnCharacterPositions.size

                val characterPosition = if (line.drawnCharacterPositions.isEmpty()) CharacterPosition(
                        0f, 0f, 1f, 0f, false
                )
                else line.drawnCharacterPositions[caretCharIndex]

                val caretWidth = lineHeight * target.getStringAspectRatio("|", textStyle.font) / target.getAspectRatio() / 4
                var caretX = if (characterPosition.isLeftToRight == flip) {
                    characterPosition.maxX - caretWidth / 2
                } else characterPosition.minX - caretWidth / 2
                caretX = max(caretX, 0f)
                caretX = min(caretX, 1f - caretWidth)

                target.fillRect(caretX, minY, caretX + caretWidth, maxY, Color.RED)
            }

            maxY = minY
        }

        return RenderResult(drawnRegion = RectangularDrawnRegion(0f, 0f, 1f, 1f), propagateMissedCursorEvents = false)
    }
}
