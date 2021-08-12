package dragons.init.trouble

import javax.swing.JFrame

abstract class StartupException(val title: String, val width: Int, val height: Int): Exception(title) {

    abstract fun initFrame(target: JFrame)
}
