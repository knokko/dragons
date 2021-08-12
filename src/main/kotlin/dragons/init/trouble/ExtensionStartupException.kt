package dragons.init.trouble

import javax.swing.*

class ExtensionStartupException(
    title: String,
    private val description: String,
    private val availableExtensions: Set<String>,
    private val requiredExtensions: Set<String>
): StartupException(title, 800, 600) {
    override fun initFrame(target: JFrame) {
        val descriptionArea = JTextArea(description)
        descriptionArea.lineWrap = true
        descriptionArea.wrapStyleWord = true
        descriptionArea.isEditable = false

        val availableExtensionLabels = availableExtensions.map { extension -> JLabel(extension) }

        val layout = GroupLayout(target.contentPane)
        target.contentPane.layout = layout

        layout.autoCreateContainerGaps = true

        val horizontalGroup = layout.createParallelGroup().addComponent(descriptionArea)
        val horizontalAvailableExtensions = layout.createParallelGroup()
        for (label in availableExtensionLabels) {
            horizontalAvailableExtensions.addComponent(label)
        }
        horizontalGroup.addGroup(horizontalAvailableExtensions)
        layout.setHorizontalGroup(horizontalGroup)

        val verticalGroup = layout.createSequentialGroup().addComponent(descriptionArea)
        val verticalAvailableExtensions = layout.createSequentialGroup()
        for (label in availableExtensionLabels) {
            verticalAvailableExtensions.addComponent(label)
        }
        verticalGroup.addGroup(verticalAvailableExtensions)
        layout.setVerticalGroup(verticalGroup)

        target.pack()


    }
}
