package gruviks.component.text

import graviks2d.resource.text.CharacterPosition
import graviks2d.resource.text.TextOverflowPolicy
import graviks2d.target.GraviksTarget
import gruviks.component.Component
import gruviks.component.RectangularDrawnRegion
import gruviks.component.RenderResult
import gruviks.event.*
import gruviks.feedback.*
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

    private var selectionStart: SelectionCorner? = null
    private var selectionEnd: SelectionCorner? = null
    private var isSelecting = false
    private var shouldRecomputeIndent = false

    override fun subscribeToEvents() {
        agent.subscribe(KeyboardFocusAcquiredEvent::class)
        agent.subscribe(KeyboardFocusLostEvent::class)
        agent.subscribe(KeyTypeEvent::class)
        agent.subscribe(KeyPressEvent::class)
        agent.subscribe(CursorClickEvent::class)
        agent.subscribe(CursorPressEvent::class)
        agent.subscribe(CursorMoveEvent::class)
        agent.subscribe(CursorReleaseEvent::class)
        agent.subscribe(CursorScrollEvent::class)
        agent.subscribe(UpdateEvent::class)
        agent.subscribe(ClipboardCopyEvent::class)
        agent.subscribe(ClipboardPasteEvent::class)
    }

    private fun resetCaretFlipper() {
        lastCaretFlipTime = currentTimeMillis()
        showCaret = true
    }

    private fun getCaretX() = if (caretLine != null) {
        val lineInput = lineInputs[caretLine!!]
        lineInput.estimateCaretX(lineInput.getCaretPosition(), leftToRightTracker[caretLine!!], scrollOffsetX)
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

    private fun cancelSelection() {
        if (selectionStart != null || selectionEnd != null) {
            selectionStart = null
            selectionEnd = null
            agent.giveFeedback(RenderFeedback())
        }
    }

    private fun getSelectedString(): String {
        val selectionStart = this.selectionStart
        val selectionEnd = this.selectionEnd
        if (selectionStart != null && selectionEnd != null && selectionStart != selectionEnd) {
            val selectionMin = minOf(selectionStart, selectionEnd)
            val selectionMax = maxOf(selectionStart, selectionEnd)
            return if (selectionMin.lineIndex == selectionMax.lineIndex) {
                val line = lineInputs[selectionMin.lineIndex].getText()
                line.substring(selectionMin.caretIndex, selectionMax.caretIndex)
            } else {
                val result = StringBuilder()
                result.append(lineInputs[selectionMin.lineIndex].getText().substring(selectionMin.caretIndex))
                result.appendLine()
                for (lineIndex in selectionMin.lineIndex + 1 until selectionMax.lineIndex) {
                    result.append(lineInputs[lineIndex].getText())
                    result.appendLine()
                }
                result.append(lineInputs[selectionMax.lineIndex].getText().substring(0 until selectionMax.caretIndex))
                result.toString()
            }
        } else return ""
    }

    private fun deleteSelectedText() {
        val selectionStart = this.selectionStart
        val selectionEnd = this.selectionEnd
        if (selectionStart != null && selectionEnd != null) {
            this.selectionStart = null
            this.selectionEnd = null
            this.isSelecting = false

            val selectionMin = minOf(selectionStart, selectionEnd)
            val selectionMax = maxOf(selectionStart, selectionEnd)

            this.caretLine = selectionMin.lineIndex
            lineInputs[selectionMin.lineIndex].setCaretPosition(selectionMin.caretIndex)

            if (selectionMin.lineIndex == selectionMax.lineIndex) {
                val line = lineInputs[selectionMin.lineIndex]
                val oldText = line.getText()
                val newText = oldText.substring(0 until selectionMin.caretIndex) +
                        oldText.substring(selectionMax.caretIndex)
                line.setText(newText)
            } else {
                val lastLine = lineInputs[selectionMax.lineIndex]
                val firstLine = lineInputs[selectionMin.lineIndex]

                for (counter in 0 until selectionMax.lineIndex - selectionMin.lineIndex) {
                    lineInputs.removeAt(selectionMin.lineIndex + 1)
                }

                firstLine.setText(
                    firstLine.getText().substring(0 until selectionMin.caretIndex) +
                    lastLine.getText().substring(selectionMax.caretIndex)
                )
            }

            agent.giveFeedback(RenderFeedback())
        }
    }

    private fun determineCaretLine(y: Float): Int {
        var newCaretLine = ((1f + scrollOffsetY - y) / lastLineHeight).toInt()
        if (newCaretLine < 0) newCaretLine = 0
        if (newCaretLine >= lineInputs.size) newCaretLine = lineInputs.size - 1
        return newCaretLine
    }

    private fun determineSelectedPosition(position: EventPosition): SelectionCorner {
        val pressedLineIndex = determineCaretLine(position.y)
        val pressedLine = lineInputs[pressedLineIndex]
        val caretPosition = pressedLine.predictCaretPosition(position.x - scrollOffsetX)
        return SelectionCorner(pressedLineIndex, caretPosition)
    }

    private fun indentRight() {
        val selectionStart = this.selectionStart
        val selectionEnd = this.selectionEnd
        if (selectionStart != null && selectionEnd != null) {
            for (lineIndex in min(selectionStart.lineIndex, selectionEnd.lineIndex) .. max(selectionStart.lineIndex, selectionEnd.lineIndex)) {
                val line = lineInputs[lineIndex]
                line.setText("\t${line.getText()}")
            }
            this.selectionStart = SelectionCorner(selectionStart.lineIndex, selectionStart.caretIndex + 1)
            this.selectionEnd = SelectionCorner(selectionEnd.lineIndex, selectionEnd.caretIndex + 1)
            this.shouldRecomputeIndent = true
        }
    }

    private fun indentLeft() {
        val selectionStart = this.selectionStart
        val selectionEnd = this.selectionEnd
        if (selectionStart != null && selectionEnd != null) {
            for (lineIndex in min(selectionStart.lineIndex, selectionEnd.lineIndex) .. max(selectionStart.lineIndex, selectionEnd.lineIndex)) {
                val line = lineInputs[lineIndex]
                val oldText = line.getText()
                if (oldText.startsWith("\t")) {
                    line.setText(oldText.substring(1))
                    if (selectionStart.lineIndex == lineIndex) {
                        this.selectionStart = SelectionCorner(lineIndex, selectionStart.caretIndex - 1)
                    }
                    if (selectionEnd.lineIndex == lineIndex) {
                        this.selectionEnd = SelectionCorner(lineIndex, selectionEnd.caretIndex - 1)
                    }
                }
            }

            this.shouldRecomputeIndent = true
        }
    }

    private fun insertPastedString(content: String) {
        val caretLine = this.caretLine
        if (caretLine != null && caretLine < lineInputs.size) {

            val contentList = splitLines(content)
            if (contentList.isEmpty()) return

            val firstLine = lineInputs[caretLine]
            val firstPart = firstLine.getText().substring(0 until firstLine.getCaretPosition())
            val lastPart = firstLine.getText().substring(firstLine.getCaretPosition())

            firstLine.setText(firstPart + contentList[0])

            var currentLineIndex = caretLine + 1
            for (pastedLine in contentList.subList(1, contentList.size)) {

                val newLine = TextInput(pastedLine, this::giveRenderFeedback)
                lineInputs.add(currentLineIndex, newLine)
                currentLineIndex += 1
            }

            val lastLine = lineInputs[currentLineIndex - 1]
            lastLine.moveToEnd()
            lastLine.setText(lastLine.getText() + lastPart)
            this.caretLine = currentLineIndex - 1

            checkHorizontalScroll(true)
            resetCaretFlipper()
            agent.giveFeedback(RenderFeedback())
        }
    }

    override fun processEvent(event: Event) {
        if (event is KeyboardFocusAcquiredEvent || event is KeyboardFocusLostEvent) {
            agent.giveFeedback(RenderFeedback())
        }

        if (event is CursorClickEvent) {

            agent.giveFeedback(RequestKeyboardFocusFeedback())

            val newCaretLine = determineCaretLine(event.position.y)
            lineInputs[newCaretLine].moveTo(event.position.x - scrollOffsetX)
            if (caretLine == null || caretLine != newCaretLine) {
                caretLine = newCaretLine
            }

            checkHorizontalScroll(true)

            resetCaretFlipper()
            agent.giveFeedback(RenderFeedback())
        }

        if (event is CursorPressEvent) {
            cancelSelection()
            agent.giveFeedback(RequestKeyboardFocusFeedback())
            this.selectionStart = determineSelectedPosition(event.position)
            this.isSelecting = true
        }

        if (event is CursorReleaseEvent) {
            if (this.isSelecting) {
                this.isSelecting = false
                val selection = getSelectedString()
                if (selection.isNotEmpty()) {
                    agent.giveFeedback(AddressedFeedback(null, SelectionFeedback(selection)))
                }
            }
        }

        if (event is CursorMoveEvent) {
            if (this.selectionStart != null && this.isSelecting) {
                this.selectionEnd = determineSelectedPosition(event.newPosition)
                agent.giveFeedback(RenderFeedback())
            }
        }

        if (event is ClipboardCopyEvent) {
            val selectedString = getSelectedString()
            if (selectedString.isNotEmpty()) event.setText(selectedString)
            if (event.cut) deleteSelectedText()
        }

        if (event is ClipboardPasteEvent) {
            deleteSelectedText()
            cancelSelection()
            insertPastedString(event.content)
        }

        if (event is KeyTypeEvent) {
            val caretLine = this.caretLine
            if (caretLine != null && caretLine < lineInputs.size) {
                lineInputs[caretLine].type(event.codePoint)
                checkHorizontalScroll(true)
                resetCaretFlipper()
            }

            cancelSelection()
        }

        if (event is KeyPressEvent) {
            if (event.key.type == KeyType.Escape) agent.giveFeedback(ReleaseKeyboardFocusFeedback())

            if (selectionStart != null && selectionEnd != null) {
                when (event.key.type) {
                    KeyType.Tab -> indentRight()
                    KeyType.Right -> indentRight()
                    KeyType.Left -> indentLeft()
                    KeyType.Backspace -> deleteSelectedText()
                    else -> {}
                }
                return
            }

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
                            lineInputs[caretLine!!].moveTo(textInput.estimateCaretX(
                                textInput.getCaretPosition(), expectLeftToRight, 0f
                            ))

                            checkHorizontalScroll(true)
                            if (getCaretY() < 0f) scrollOffsetY -= lastLineHeight
                            agent.giveFeedback(RenderFeedback())
                        }
                    }
                    KeyType.Down -> {
                        if (caretLine!! < lineInputs.size - 1) {
                            val expectLeftToRight = leftToRightTracker[caretLine!!]
                            caretLine = caretLine!! + 1
                            lineInputs[caretLine!!].moveTo(textInput.estimateCaretX(
                                textInput.getCaretPosition(), expectLeftToRight, 0f
                            ))

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
        val hasFocus = agent.hasKeyboardFocus()
        if (!::lineInputs.isInitialized) {
            lineInputs = splitLines(initialText).map { TextInput(it, this::giveRenderFeedback) }.toMutableList()
        }

        val drawPlaceholder = !agent.hasKeyboardFocus() && placeholder != null && getText().isEmpty()
        val textToDraw = if (drawPlaceholder) splitLines(placeholder!!).map {
            TextInput(it, this::giveRenderFeedback)
        }.withIndex() else lineInputs.withIndex()
        val allLines = textToDraw.map { it.value.getText() }

        val (textRegion, lineHeight) = style.determineTextRegionAndLineHeight(target, hasFocus, drawPlaceholder)

        // The lines that are almost visible should also be 'drawn' to ensure that the character placements are known
        val marginY = 3f * lineHeight

        val linesToDraw = mutableListOf<LineToDraw>()
        var maxY = textRegion.maxY + scrollOffsetY
        for ((lineIndex, line) in textToDraw) {
            if (maxY < -marginY) break

            val minY = maxY - lineHeight

            if (minY < 1f + marginY) {
                val selectionStart = this.selectionStart
                val selectionEnd = this.selectionEnd
                val (minSelectionX, maxSelectionX) = if (selectionStart != null && selectionEnd != null) {
                    val selectionMin = minOf(selectionStart, selectionEnd)
                    val selectionMax = maxOf(selectionStart, selectionEnd)

                    val expectLeftToRight = leftToRightTracker[lineIndex]
                    if (selectionMin.lineIndex == selectionMax.lineIndex) {
                        if (lineIndex == selectionMin.lineIndex) {
                            val minX = line.estimateCaretX(selectionMin.caretIndex, expectLeftToRight, scrollOffsetX)
                            val maxX = line.estimateCaretX(selectionMax.caretIndex, expectLeftToRight, scrollOffsetX)
                            Pair(minX, maxX)
                        } else Pair(null, null)
                    } else if (lineIndex == selectionMin.lineIndex) {
                        val selectionX = line.estimateCaretX(selectionMin.caretIndex, expectLeftToRight, scrollOffsetX)
                        if (expectLeftToRight) Pair(selectionX, 1f) else Pair(0f, selectionX)
                    } else if (lineIndex == selectionMax.lineIndex) {
                        val selectionX = line.estimateCaretX(selectionMax.caretIndex, expectLeftToRight, scrollOffsetX)
                        if (expectLeftToRight) Pair(0f, selectionX) else Pair(selectionX, 1f)
                    } else if (lineIndex > selectionMin.lineIndex && lineIndex < selectionMax.lineIndex) {
                        Pair(0f, 1f)
                    } else Pair(null, null)
                } else Pair(null, null)
                linesToDraw.add(LineToDraw(line.getText(), lineIndex, minY, maxY, minSelectionX, maxSelectionX))
            }

            maxY = minY
        }

        style.drawBackgroundAndDecorations(target, linesToDraw, hasFocus, drawPlaceholder)

        lastLineHeight = lineHeight

        val indirectTextStyleFunction = if (hasFocus) style.focusTextStyle
        else if (drawPlaceholder) style.placeholderTextStyle!!
        else style.defaultTextStyle

        val textStyleFunction = indirectTextStyleFunction(allLines)

        for (lineToDraw in linesToDraw) {
            val textStyle = textStyleFunction(lineToDraw.index)
            if (textStyle.overflowPolicy != TextOverflowPolicy.DiscardEnd) {
                throw IllegalArgumentException("Overflow policy must be DiscardEnd, ubt is ${textStyle.overflowPolicy}")
            }

            val suggestLeftToRight = leftToRightTracker[lineToDraw.index]
            val isRightToLeft = run {
                val dryResult = target.drawString(
                    0f, 0f, 1000f, 1f, lineToDraw.text, textStyle,
                    dryRun = true, suggestLeftToRight = suggestLeftToRight
                )
                if (dryResult.isEmpty()) !suggestLeftToRight
                else dryResult.minOf { it.minX } > 5f
            }

            val drawnCharPositions = run {
                val minTextX = if (isRightToLeft) textRegion.minX else textRegion.minX - scrollOffsetX
                val maxTextX = if (isRightToLeft) textRegion.maxX - scrollOffsetX else textRegion.maxX
                target.drawString(
                    minTextX, lineToDraw.minY, maxTextX, lineToDraw.maxY, lineToDraw.text, textStyle,
                    suggestLeftToRight = suggestLeftToRight
                )
            }

            if (drawnCharPositions.size >= 4) {
                leftToRightTracker[lineToDraw.index] = !isRightToLeft
            } else leftToRightTracker.remove(lineToDraw.index)

            val lineInput = lineInputs[lineToDraw.index]
            lineInput.drawnCharacterPositions = drawnCharPositions.map { originalPosition ->
                CharacterPosition(
                    minX = originalPosition.minX - scrollOffsetX, minY = (originalPosition.minY - lineToDraw.minY) / lineHeight,
                    maxX = originalPosition.maxX - scrollOffsetX, maxY = (originalPosition.maxY - lineToDraw.minY) / lineHeight,
                    croppedMinX = originalPosition.croppedMinX - scrollOffsetX,
                    croppedMaxX = originalPosition.croppedMaxX - scrollOffsetX,
                    isLeftToRight = originalPosition.isLeftToRight
                )
            }

            if (showCaret && caretLine == lineToDraw.index && agent.hasKeyboardFocus()) {
                val caretCharIndex = min(lineInput.drawnCharacterPositions.size - 1, lineInput.getCaretPosition())
                val flip = lineInput.getCaretPosition() >= lineInput.drawnCharacterPositions.size

                val characterPosition = if (lineInput.drawnCharacterPositions.isEmpty()) CharacterPosition(
                    textRegion.minX - scrollOffsetX, 0f, textRegion.maxX - scrollOffsetX, 0f, isRightToLeft
                ) else drawnCharPositions[caretCharIndex]

                val caretWidth =
                    lineHeight * target.getStringAspectRatio("|", textStyle.font) / target.getAspectRatio() / 4
                var caretX = if (characterPosition.isLeftToRight == flip) {
                    characterPosition.maxX - caretWidth / 2
                } else characterPosition.minX - caretWidth / 2

                if (caretX < caretWidth) caretX = caretWidth
                if (caretX > 1f - 2f * caretWidth) caretX = 1f - 2f * caretWidth

                target.fillRect(caretX, lineToDraw.minY, caretX + caretWidth, lineToDraw.maxY, textStyle.fillColor)
            }
        }

        if (checkHorizontalScrollAfterRender) {
            checkHorizontalScrollAfterRender = false
            val oldScrollOffset = scrollOffsetX
            checkHorizontalScroll(true)
            if (oldScrollOffset != scrollOffsetX) agent.giveFeedback(RenderFeedback())
        }

        // An extra render is needed after changing selection indentation because it needs to know the drawn character
        // positions generated during this render
        if (shouldRecomputeIndent) {
            agent.giveFeedback(RenderFeedback())
            shouldRecomputeIndent = false
        }

        return RenderResult(drawnRegion = RectangularDrawnRegion(0f, 0f, 1f, 1f), propagateMissedCursorEvents = false)
    }
}

class LineToDraw(
    val text: String, val index: Int,
    val minY: Float, val maxY: Float,
    val minSelectionX: Float?, val maxSelectionX: Float?
)

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

private class SelectionCorner(val lineIndex: Int, val caretIndex: Int): Comparable<SelectionCorner> {
    override fun compareTo(other: SelectionCorner): Int {
        if (lineIndex < other.lineIndex) return -1
        if (lineIndex > other.lineIndex) return 1
        return caretIndex.compareTo(other.caretIndex)
    }

    override fun equals(other: Any?) = other is SelectionCorner && lineIndex == other.lineIndex
            && caretIndex == other.caretIndex

    override fun hashCode(): Int {
        var result = lineIndex
        result = 31 * result + caretIndex
        return result
    }

    override fun toString() = "SelectionCorner(line=$lineIndex, caret=$caretIndex)"
}
