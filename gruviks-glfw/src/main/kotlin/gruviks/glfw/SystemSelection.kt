package gruviks.glfw

import java.awt.HeadlessException
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

fun setSystemSelection(selectedText: String) {
    try {
        Toolkit.getDefaultToolkit().systemSelection?.setContents(
            StringSelection(selectedText)
        ) { _, _ -> }
    } catch (ignored: IllegalStateException) {
    } catch (ignored: HeadlessException) {}
}

fun getSystemSelection(): String? {
    return try {
        val content = Toolkit.getDefaultToolkit().systemSelection?.getContents(null)
        if (content != null) {
            val data = content.getTransferData(DataFlavor.stringFlavor)
            if (data is String) data else null
        } else null
    } catch (ignored: Exception) {
        null
    }
}
