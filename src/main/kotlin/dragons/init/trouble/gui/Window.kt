package dragons.init.trouble.gui

import dragons.init.trouble.StartupException
import java.awt.EventQueue
import javax.swing.JFrame

fun showStartupTroubleWindow(problem: StartupException) {
    EventQueue.invokeLater {
        val frame = StartupTroubleFrame(problem)
        frame.isVisible = true
    }
}

class StartupTroubleFrame(problem: StartupException): JFrame() {

    init {
        title = problem.title
        defaultCloseOperation = DISPOSE_ON_CLOSE
        setSize(problem.width, problem.height)
        setLocationRelativeTo(null)

        problem.initFrame(this)
    }
}
