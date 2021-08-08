package dragons.init.trouble

import javax.swing.JFrame
import javax.swing.JTextArea

class SimpleStartupException(title: String, private val description: List<String>): StartupException(title, 600, 200) {
    override fun initFrame(target: JFrame) {
        val textArea = JTextArea(description.joinToString("\r\n"))
        textArea.lineCount
        textArea.lineWrap = true
        textArea.wrapStyleWord = true
        textArea.isEditable = false

        target.add(textArea)
    }
}
