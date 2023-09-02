package gruviks.component.text

import graviks2d.resource.text.CharacterPosition
import java.lang.Integer.min

internal class TextInput(
        private var currentText: String,
        private val giveRenderFeedback: () -> Unit
) {

    lateinit var drawnCharacterPositions: List<CharacterPosition>
    private var caretPosition = 0

    fun type(codepoint: Int) {
        val newCodepoints = if (caretPosition < currentText.length) {
            val oldCodepoints = currentText.codePoints().toArray()
            val newCodepoints = StringBuilder(currentText.length + 2)

            for ((oldIndex, oldCodepoint) in oldCodepoints.withIndex()) {
                if (oldIndex == caretPosition) {
                    newCodepoints.appendCodePoint(codepoint)
                }
                newCodepoints.appendCodePoint(oldCodepoint)
            }
            newCodepoints
        } else {
            val newCodepoints = StringBuilder(currentText)
            newCodepoints.appendCodePoint(codepoint)
            newCodepoints
        }

        caretPosition += 1
        setText(newCodepoints.toString())
    }

    fun backspace() {
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

    fun moveLeft() {
        if (drawnCharacterPositions.isNotEmpty()) {
            if (caretPosition < drawnCharacterPositions.size) {
                val currentMinX = drawnCharacterPositions[caretPosition].minX
                val nextPosition = drawnCharacterPositions.filter { it.minX < currentMinX }.maxByOrNull { it.minX }
                if (nextPosition != null) {
                    caretPosition = drawnCharacterPositions.indexOf(nextPosition)
                    giveRenderFeedback()
                } else if (caretPosition == drawnCharacterPositions.size - 1 && !drawnCharacterPositions[caretPosition].isLeftToRight){
                    caretPosition = drawnCharacterPositions.size
                    giveRenderFeedback()
                }
            } else if (drawnCharacterPositions[drawnCharacterPositions.size - 1].isLeftToRight) {
                caretPosition -= 1
                giveRenderFeedback()
            }
        }
    }

    fun moveRight() {
        if (drawnCharacterPositions.isNotEmpty()) {
            if (caretPosition < drawnCharacterPositions.size) {
                val currentMinX = drawnCharacterPositions[caretPosition].minX
                val nextPosition = drawnCharacterPositions.filter { it.minX > currentMinX }.minByOrNull { it.minX }
                if (nextPosition != null) {
                    caretPosition = drawnCharacterPositions.indexOf(nextPosition)
                    giveRenderFeedback()
                } else if (caretPosition == drawnCharacterPositions.size - 1 && drawnCharacterPositions[caretPosition].isLeftToRight) {
                    caretPosition = drawnCharacterPositions.size
                    giveRenderFeedback()
                }
            } else if (!drawnCharacterPositions[drawnCharacterPositions.size - 1].isLeftToRight) {
                caretPosition -= 1
                giveRenderFeedback()
            }
        }
    }

    fun estimateCaretX(caretPosition: Int, expectLeftToRight: Boolean, scrollOffsetX: Float): Float {
        if (!::drawnCharacterPositions.isInitialized || drawnCharacterPositions.isEmpty()) return -scrollOffsetX + if (expectLeftToRight) 0f else 1f

        return scrollOffsetX + if (caretPosition < drawnCharacterPositions.size) {
            val position = drawnCharacterPositions[caretPosition]
            if (position.isLeftToRight) position.minX
            else position.maxX
        } else {
            val position = drawnCharacterPositions.last()
            if (position.isLeftToRight) position.maxX
            else position.minX
        }
    }

    fun moveTo(x: Float) {
        this.caretPosition = this.predictCaretPosition(x)
    }

    private fun countCodepoints() = currentText.codePointCount(0, currentText.length)

    fun predictCaretPosition(x: Float): Int {
        val clickedIndex = this.drawnCharacterPositions.indexOfFirst {
            it.minX <= x && it.maxX >= x
        }
        val numCodepoints = countCodepoints()
        return if (clickedIndex != -1 && clickedIndex < numCodepoints) {
            val isRightToLeft = !this.drawnCharacterPositions[clickedIndex].isLeftToRight
            val clickedPosition = this.drawnCharacterPositions[clickedIndex]
            val clickedOnLeftSide = x - clickedPosition.minX <= clickedPosition.maxX - x
            if (clickedOnLeftSide != isRightToLeft) {
                clickedIndex
            } else {
                clickedIndex + 1
            }
        } else numCodepoints
    }

    fun moveToEnd() {
        this.caretPosition = countCodepoints()
        this.giveRenderFeedback()
    }

    fun setText(newText: String) {
        this.currentText = newText
        this.caretPosition = min(this.caretPosition, countCodepoints())
        giveRenderFeedback()
    }

    fun getText() = currentText

    fun getCaretPosition() = caretPosition

    fun setCaretPosition(newPosition: Int) {
        this.caretPosition = min(newPosition, countCodepoints())
        giveRenderFeedback()
    }
}
