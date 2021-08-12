package dragons.init.trouble

import java.awt.Color
import javax.swing.*

class ExtensionStartupException(
    title: String,
    private val description: String,
    private val availableExtensions: Set<String>,
    private val requiredExtensions: Set<String>,
    private val extensionsWord: String = "extensions"
): StartupException(title, 800, 600) {
    override fun initFrame(target: JFrame) {
        val descriptionArea = JTextArea(description)
        descriptionArea.lineWrap = true
        descriptionArea.wrapStyleWord = true
        descriptionArea.isEditable = false

        val availableExtensionLabels = listOf(JLabel("Available $extensionsWord:")) + availableExtensions.map { extension -> JLabel(extension) }
        val requiredExtensionLabels = listOf(JLabel("Required $extensionsWord:")) + requiredExtensions.map { extension ->
            val label = JLabel(extension)
            if (!availableExtensions.contains(extension)) {
                label.foreground = Color.RED
            }
            label
        }

        val layout = GroupLayout(target.contentPane)
        target.contentPane.layout = layout

        layout.autoCreateContainerGaps = true

        val horizontalGroup = layout.createParallelGroup().addComponent(descriptionArea)
        run {
            val availableExtensionGroup = layout.createParallelGroup()
            for (label in availableExtensionLabels) {
                availableExtensionGroup.addComponent(label)
            }

            val requiredExtensionGroup = layout.createParallelGroup()
            for (label in requiredExtensionLabels) {
                requiredExtensionGroup.addComponent(label)
            }

            val extensionGroups = layout.createSequentialGroup()
            extensionGroups.addGroup(availableExtensionGroup)
            extensionGroups.addGroup(requiredExtensionGroup)

            horizontalGroup.addGroup(extensionGroups)
        }
        layout.setHorizontalGroup(horizontalGroup)

        val verticalGroup = layout.createSequentialGroup().addComponent(descriptionArea)
        run {
            val availableExtensionGroup = layout.createSequentialGroup()
            for (label in availableExtensionLabels) {
                availableExtensionGroup.addComponent(label)
            }

            val requiredExtensionGroup = layout.createSequentialGroup()
            for (label in requiredExtensionLabels) {
                requiredExtensionGroup.addComponent(label)
            }

            val extensionGroups = layout.createParallelGroup()
            extensionGroups.addGroup(availableExtensionGroup)
            extensionGroups.addGroup(requiredExtensionGroup)

            verticalGroup.addGroup(extensionGroups)
        }

        layout.setVerticalGroup(verticalGroup)

        target.pack()
    }
}
