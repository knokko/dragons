package gruviks.component.text

import graviks2d.resource.text.CharacterPosition
import graviks2d.resource.text.TextOverflowPolicy
import graviks2d.target.GraviksTarget
import gruviks.component.Component
import gruviks.component.RectangularDrawnRegion
import gruviks.component.RenderResult
import gruviks.event.*
import gruviks.feedback.ReleaseKeyboardFocusFeedback
import gruviks.feedback.RenderFeedback
import gruviks.feedback.RequestKeyboardFocusFeedback
import java.lang.StringBuilder
import java.lang.System.currentTimeMillis
import kotlin.math.max
import kotlin.math.min

class TextArea(
        private val initialText: String,
        private val style: TextAreaStyle,
        private val placeholder: String? = null,
): Component() {

    private lateinit var lineInputs: MutableList<TextInput>
    private val leftToRightTracker = LeftToRightTracker()
    private var lastLineHeight = 0f
    private var checkHorizontalScrollAfterRender = false

    private var caretLine: Int? = null
    private var lastCaretFlipTime = currentTimeMillis()
    private var showCaret = true

    private var scrollOffsetX = 0f
    private var scrollOffsetY = 0f

    init {
        val textStyles = arrayOf(style.defaultTextStyle, style.focusTextStyle) +
                if (style.placeholderTextStyle != null) arrayOf(style.placeholderTextStyle) else arrayOf()
        for (textStyle in textStyles) {
            if (textStyle.overflowPolicy != TextOverflowPolicy.DiscardEnd) {
                throw IllegalArgumentException("All overflow policies must be DiscardEnd")
            }
        }
    }

    override fun subscribeToEvents() {
        agent.subscribe(KeyboardFocusAcquiredEvent::class)
        agent.subscribe(KeyboardFocusLostEvent::class)
        agent.subscribe(KeyTypeEvent::class)
        agent.subscribe(KeyPressEvent::class)
        agent.subscribe(CursorClickEvent::class)
        agent.subscribe(CursorScrollEvent::class)
        agent.subscribe(UpdateEvent::class)
    }

    private fun resetCaretFlipper() {
        lastCaretFlipTime = currentTimeMillis()
        showCaret = true
    }

    private fun getCaretX() = if (caretLine != null) {
        val lineInput = lineInputs[caretLine!!]
        lineInput.estimateCaretX(leftToRightTracker[caretLine!!], scrollOffsetX)
    } else -scrollOffsetX

    private fun getCaretY() = caretLine!! * lastLineHeight - scrollOffsetY + lastLineHeight / 2f

    private fun checkHorizontalScroll(correctCaret: Boolean) {
        if (correctCaret) {
            val margin = 0.1f
            val caretX = getCaretX()
            if (caretX < margin) {
                scrollOffsetX -= margin - caretX
            } else if (caretX > 1f - margin) {
                scrollOffsetX += caretX - (1f - margin)
            }
        }

        if (!leftToRightTracker.hasRightToLeft() && scrollOffsetX < 0f) scrollOffsetX = 0f
        if (!leftToRightTracker.hasLeftToRight() && scrollOffsetX > 0f) scrollOffsetX = 0f
    }

    override fun processEvent(event: Event) {
        if (event is KeyboardFocusAcquiredEvent || event is KeyboardFocusLostEvent) {
            agent.giveFeedback(RenderFeedback())
        }

        if (event is CursorClickEvent) {

            agent.giveFeedback(RequestKeyboardFocusFeedback())

            var newCaretLine = ((1f + scrollOffsetY - event.position.y) / lastLineHeight).toInt()
            if (newCaretLine < 0) newCaretLine = 0
            if (newCaretLine >= lineInputs.size) newCaretLine = lineInputs.size - 1

            lineInputs[newCaretLine].moveTo(event.position.x - scrollOffsetX)
            if (caretLine == null || caretLine != newCaretLine) {
                caretLine = newCaretLine
            }

            checkHorizontalScroll(true)

            resetCaretFlipper()
            agent.giveFeedback(RenderFeedback())
        }

        if (event is KeyTypeEvent) {
            val caretLine = this.caretLine
            if (caretLine != null && caretLine < lineInputs.size) {
                lineInputs[caretLine].type(event.codePoint)
                checkHorizontalScroll(true)
                resetCaretFlipper()
            }
        }

        if (event is KeyPressEvent) {
            if (event.key.type == KeyType.Escape) agent.giveFeedback(ReleaseKeyboardFocusFeedback())

            if (caretLine != null) {
                val textInput = lineInputs[caretLine!!]
                var shouldResetCaretFlipper = true
                when (event.key.type) {
                    KeyType.Tab -> {
                        textInput.type('\t'.code)
                        checkHorizontalScroll(true)
                    }
                    KeyType.Enter -> {
                        val oldLine = textInput.getText()

                        val newLine = oldLine.substring(textInput.getCaretPosition())
                        textInput.setText(oldLine.substring(0, textInput.getCaretPosition()))
                        lineInputs.add(1 + caretLine!!, TextInput(newLine, this::giveRenderFeedback))
                        caretLine = caretLine!! + 1

                        if (getCaretY() > 1f) scrollOffsetY += lastLineHeight
                        checkHorizontalScrollAfterRender = true
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
                            if (getCaretY() < 0f) scrollOffsetY -= lastLineHeight
                            agent.giveFeedback(RenderFeedback())
                        }
                        checkHorizontalScroll(true)
                    }
                    KeyType.Right -> {
                        textInput.moveRight()
                        checkHorizontalScroll(true)
                    }
                    KeyType.Left -> {
                        textInput.moveLeft()
                        checkHorizontalScroll(true)
                    }
                    KeyType.Up -> {
                        if (caretLine!! > 0) {
                            val expectLeftToRight = leftToRightTracker[caretLine!!]
                            caretLine = caretLine!! - 1
                            lineInputs[caretLine!!].moveTo(textInput.estimateCaretX(expectLeftToRight, 0f))

                            checkHorizontalScroll(true)
                            if (getCaretY() < 0f) scrollOffsetY -= lastLineHeight
                            agent.giveFeedback(RenderFeedback())
                        }
                    }
                    KeyType.Down -> {
                        if (caretLine!! < lineInputs.size - 1) {
                            val expectLeftToRight = leftToRightTracker[caretLine!!]
                            caretLine = caretLine!! + 1
                            lineInputs[caretLine!!].moveTo(textInput.estimateCaretX(expectLeftToRight, 0f))

                            checkHorizontalScroll(true)
                            if (getCaretY() > 1f) scrollOffsetY += lastLineHeight
                            agent.giveFeedback(RenderFeedback())
                        }
                    }
                    else -> shouldResetCaretFlipper = false
                }

                if (shouldResetCaretFlipper) resetCaretFlipper()
            }
        }

        if (event is CursorScrollEvent) {
            if (event.direction == ScrollDirection.Horizontal) {
                scrollOffsetX += event.amount
                checkHorizontalScroll(false)
                agent.giveFeedback(RenderFeedback())
            }
            if (event.direction == ScrollDirection.Vertical) {
                scrollOffsetY = max(0f, min(lineInputs.size * lastLineHeight, scrollOffsetY + event.amount))
                agent.giveFeedback(RenderFeedback())
            }
        }

        if (event is UpdateEvent) {
            val currentTime = currentTimeMillis()
            if (currentTime - lastCaretFlipTime > 500) {
                lastCaretFlipTime = currentTime
                showCaret = !showCaret
                agent.giveFeedback(RenderFeedback())
            }
        }
    }

    private fun giveRenderFeedback() = agent.giveFeedback(RenderFeedback())

    fun getText(): String {
        val result = StringBuilder()
        for ((index, line) in lineInputs.withIndex()) {
            result.append(line.getText())
            if (index != lineInputs.size - 1) result.append('\n')
        }
        return result.toString()
    }

    private fun splitLines(text: String) = text.replace("\r\n", "\n").replace("\n\r", "\n")
            .replace("\r", "\n").split('\n')

    override fun render(target: GraviksTarget, force: Boolean): RenderResult {
        if (!::lineInputs.isInitialized) {
            lineInputs = splitLines(initialText).map { TextInput(it, this::giveRenderFeedback) }.toMutableList()
        }

        val drawBackgroundFunction = if (agent.hasKeyboardFocus()) style.drawFocusBackground else style.drawDefaultBackground
        val (textRegion, lineHeight) = drawBackgroundFunction(target)
        val drawPlaceholder = !agent.hasKeyboardFocus() && placeholder != null && getText().isEmpty()
        val textStyle = if (agent.hasKeyboardFocus()) style.focusTextStyle else if (drawPlaceholder) style.placeholderTextStyle!! else style.defaultTextStyle
        lastLineHeight = lineHeight

        var maxY = textRegion.maxY + scrollOffsetY

        val textToDraw = if (drawPlaceholder) splitLines(placeholder!!).map { TextInput(it, this::giveRenderFeedback) }.withIndex() else lineInputs.withIndex()

        // The lines that are almost visible should also be 'drawn' to ensure that the character placements are known
        val marginY = 3f * lineHeight

        for ((lineIndex, line) in textToDraw) {
            if (maxY < -marginY) break

            val minY = maxY - lineHeight

            if (minY < 1f + marginY) {
                val suggestLeftToRight = leftToRightTracker[lineIndex]
                val isRightToLeft = run {
                    val dryResult = target.drawString(
                        0f, 0f, 1000f, 1f, line.getText(), textStyle,
                        dryRun = true, suggestLeftToRight = suggestLeftToRight
                    )
                    if (dryResult.isEmpty()) !suggestLeftToRight
                    else dryResult.minOf { it.minX } > 5f
                }

                val drawnCharPositions = run {
                    val minTextX = if (isRightToLeft) textRegion.minX else textRegion.minX - scrollOffsetX
                    val maxTextX = if (isRightToLeft) textRegion.maxX - scrollOffsetX else textRegion.maxX
                    target.drawString(
                        minTextX, minY, maxTextX, maxY, line.getText(), textStyle,
                        suggestLeftToRight = suggestLeftToRight
                    )
                }

                if (drawnCharPositions.size >= 4) {
                    leftToRightTracker[lineIndex] = !isRightToLeft
                } else leftToRightTracker.remove(lineIndex)

                line.drawnCharacterPositions = drawnCharPositions.map { originalPosition ->
                    CharacterPosition(
                        minX = originalPosition.minX - scrollOffsetX, minY = (originalPosition.minY - minY) / lineHeight,
                        maxX = originalPosition.maxX - scrollOffsetX, maxY = (originalPosition.maxY - minY) / lineHeight,
                        isLeftToRight = originalPosition.isLeftToRight
                    )
                }

                if (showCaret && caretLine == lineIndex && agent.hasKeyboardFocus()) {
                    val caretCharIndex = min(line.drawnCharacterPositions.size - 1, line.getCaretPosition())
                    val flip = line.getCaretPosition() >= line.drawnCharacterPositions.size

                    val characterPosition = if (line.drawnCharacterPositions.isEmpty()) CharacterPosition(
                        0f - scrollOffsetX, 0f, 1f - scrollOffsetX, 0f, isRightToLeft
                    ) else drawnCharPositions[caretCharIndex]

                    val caretWidth =
                        lineHeight * target.getStringAspectRatio("|", textStyle.font) / target.getAspectRatio() / 4
                    var caretX = if (characterPosition.isLeftToRight == flip) {
                        characterPosition.maxX - caretWidth / 2
                    } else characterPosition.minX - caretWidth / 2

                    if (caretX < caretWidth) caretX = caretWidth
                    if (caretX > 1f - 2f * caretWidth) caretX = 1f - 2f * caretWidth

                    target.fillRect(caretX, minY, caretX + caretWidth, maxY, textStyle.fillColor)
                }
            }

            maxY = minY
        }

        if (checkHorizontalScrollAfterRender) {
            checkHorizontalScrollAfterRender = false
            val oldScrollOffset = scrollOffsetX
            checkHorizontalScroll(true)
            if (oldScrollOffset != scrollOffsetX) agent.giveFeedback(RenderFeedback())
        }

        return RenderResult(drawnRegion = RectangularDrawnRegion(0f, 0f, 1f, 1f), propagateMissedCursorEvents = false)
    }
}

private class LeftToRightTracker {

    private val map = mutableMapOf<Int, Boolean>()
    private var leftToRightCounter = 0
    private var rightToLeftCounter = 0

    fun remove(index: Int) {
        val oldValue = map.remove(index)
        if (oldValue != null) {
            if (oldValue) leftToRightCounter -= 1
            else rightToLeftCounter -= 1
        }
    }
    operator fun set(index: Int, newValue: Boolean) {
        remove(index)

        map[index] = newValue
        if (newValue) leftToRightCounter += 1 else rightToLeftCounter += 1
    }

    operator fun get(index: Int): Boolean {
        val result = map[index]
        if (result != null) return result

        // If no result is found, check the adjacent values
        for (offset in 1 until 5) {
            val resultAbove = map[index - offset]
            val resultBelow = map[index + offset]

            if (resultAbove != null && resultBelow != null && resultAbove == resultBelow) return resultAbove
            if (resultAbove != null && resultBelow == null) return resultAbove
            if (resultAbove == null && resultBelow != null) return resultBelow
        }

        // If the adjacent values didn't help either, use the globally most-common result
        return leftToRightCounter >= rightToLeftCounter
    }

    fun hasLeftToRight() = leftToRightCounter > 0

    fun hasRightToLeft() = rightToLeftCounter > 0
}
