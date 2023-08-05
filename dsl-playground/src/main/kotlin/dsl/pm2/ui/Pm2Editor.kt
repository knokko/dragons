package dsl.pm2.ui

import dsl.pm2.interpreter.*
import dsl.pm2.interpreter.program.Pm2Program
import dsl.pm2.renderer.Pm2Instance
import graviks.glfw.GraviksWindow
import graviks2d.context.GraviksContext
import graviks2d.resource.image.ImageReference
import graviks2d.resource.text.TextStyle
import graviks2d.util.Color
import gruviks.component.Component
import gruviks.component.HorizontalComponentAlignment
import gruviks.component.VerticalComponentAlignment
import gruviks.component.fill.SimpleColorFillComponent
import gruviks.component.menu.SimpleFlatMenu
import gruviks.component.menu.controller.SimpleFlatController
import gruviks.component.menu.controller.SimpleListViewController
import gruviks.component.menu.controller.TreeViewController
import gruviks.component.text.*
import gruviks.component.util.SwitchComponent
import gruviks.event.Event
import gruviks.event.RemoveEvent
import gruviks.glfw.createAndControlGruviksWindow
import gruviks.space.Coordinate
import gruviks.space.Point
import gruviks.space.RectRegion
import gruviks.space.SpaceLayout
import org.lwjgl.vulkan.VK10.*
import troll.instance.TrollInstance
import java.io.File
import java.io.IOException
import java.nio.file.Files

val textAreaStyle = squareTextAreaStyle(
    defaultTextStyle = TextStyle(fillColor = Color.BLACK, font = null),
    defaultBackgroundColor = Color.WHITE,
    focusTextStyle = TextStyle(fillColor = Color.rgbInt(50, 50, 50), font = null),
    focusBackgroundColor = Color.rgbInt(200, 200, 200),
    lineHeight = 0.05f, placeholderStyle = null
)

private class OpenFile(
    val file: File,
    val textArea: TextArea,
    private val createPreview: () -> Pm2PreviewComponent
) {
    val preview = lazy { createPreview() }

    fun updatePreview(errorComponent: TextComponent) {
        try {
            val startTime = System.currentTimeMillis()
            val newProgram = Pm2Program.compile(textArea.getText())
            val time1 = System.currentTimeMillis()
            val newModel = newProgram.run()
            val time2 = System.currentTimeMillis()

            println("Compilation took ${time1 - startTime} ms and running took ${time2 - time1} ms")
            preview.value.updateModel(newModel)
            errorComponent.setText("")
        } catch (compileError: Pm2CompileError) {
            errorComponent.setText(compileError.message!!)
        } catch (runtimeError: Pm2RuntimeError) {
            errorComponent.setText(runtimeError.message!!)
        }
    }

    fun save(handleErrors: Boolean): Boolean {
        val content = textArea.getText()
        return try {
            Files.writeString(file.toPath(), content)
            true
        } catch (failedToSave: IOException) {
            println("Failed to save $file: ${failedToSave.message}")
            if (handleErrors) {
                println("Content should be this, good luck with it:")
                println("--------------")
                println(content)
                println("--------------")
            }
            false
        }
    }

    companion object {
        fun open(
            file: File, openFiles: MutableList<OpenFile>,
            switchComponent: SwitchComponent, errorComponent: TextComponent, updateController: () -> Unit,
            pm2Instance: Pm2Instance
        ) {
            var openFile = openFiles.find { it.file == file }
            if (openFile != null) openFiles.remove(openFile)
            if (openFile == null) {
                val content: String
                try {
                    content = Files.readString(file.toPath())
                } catch (failedToOpen: Throwable) {
                    failedToOpen.printStackTrace()
                    return
                }

                val initialModel = try {
                    Pm2Program.compile(content).run()
                } catch (compileError: Pm2CompileError) { null }

                val width = 1600
                val height = 900 // TODO Maybe stop hardcoding this?

                val createPreview = { Pm2PreviewComponent(pm2Instance, initialModel, width, height, errorComponent::setText) }
                openFile = OpenFile(file, TextArea(content, textAreaStyle, null), createPreview)
            }

            openFiles.add(0, openFile)
            switchComponent.setComponent(openFile.textArea)
            updateController()
        }
    }
}

private class SaveOnQuitController(
    val openFiles: List<OpenFile>, val pm2Instance: Pm2Instance
): SimpleFlatController() {
    override fun processEvent(event: Event) {
        if (event is RemoveEvent) {
            for (openFile in openFiles) openFile.save(true)
            vkDeviceWaitIdle(pm2Instance.troll.vkDevice())
            pm2Instance.destroy()
        }
    }
}

private val tabStyle = TextButtonStyle.textAndBorder(
    baseColor = Color.rgbInt(0, 0, 50),
    hoverColor = Color.rgbInt(0, 0, 80),
    font = null
)

private val fileButtonStyle = TextButtonStyle(
    baseTextStyle = TextStyle(fillColor = Color.rgbInt(200, 200, 200), font = null),
    baseBackgroundColor = Color.TRANSPARENT,
    baseBorderColor = Color.TRANSPARENT,
    hoverTextStyle = TextStyle(fillColor = Color.WHITE, font = null),
    hoverBackgroundColor = Color.TRANSPARENT,
    hoverBorderColor = Color.TRANSPARENT,
    horizontalAlignment = HorizontalComponentAlignment.Left,
    verticalAlignment = VerticalComponentAlignment.Middle
)

private val directoryIcon = ImageReference.classLoaderPath("dsl/pm2/ui/directory.png", false)
private val fileIcon = ImageReference.classLoaderPath("dsl/pm2/ui/file.png", false)

fun createPm2Editor(troll: TrollInstance): Component {
    val pm2Instance = Pm2Instance(troll)
    val rootMenu = SimpleFlatMenu(SpaceLayout.Simple, Color.rgbInt(150, 150, 200))

    val openFiles = mutableListOf<OpenFile>()
    val fileTree = TreeViewController.Node(File("pm2-models"), null)

    val errorComponent = TextComponent("", TextStyle(fillColor = Color.RED, font = null))
    val contentBackground = SimpleColorFillComponent(Color.rgbInt(120, 120, 180))
    val content = SwitchComponent(contentBackground)
    val upperBar = SimpleFlatMenu(SpaceLayout.GrowRight, Color.rgbInt(140, 140, 190))

    val fileTabsController = SimpleListViewController(openFiles) { element, _, position, components, refreshController ->
        val componentPosition = position ?: Point.percentage(0, 0)
        val region = RectRegion(
            componentPosition.x, Coordinate.percentage(0),
            componentPosition.x + Coordinate.percentage(480), Coordinate.percentage(100)
        )

        val simpleFileName = element.file.name.substring(0 until element.file.name.length - 4)
        components.add(Pair(TextButton(simpleFileName, null, tabStyle) { event, _ ->
            if (event.button == 0) {
                content.setComponent(element.textArea)
            } else if (event.button == 1) {
                if (element.save(false)) {
                    content.setComponent(contentBackground)
                    content.removeComponent(element.textArea)
                    if (element.preview.isInitialized()) content.removeComponent(element.preview.value)
                    openFiles.remove(element)
                    refreshController()
                } else errorComponent.setText("Saving failed due to an IOException")
            } else if (event.button == 2) {
                content.setComponent(element.preview.value)
                element.updatePreview(errorComponent)
            }
        }, region))

        Point(region.boundX, Coordinate.percentage(0))
    }
    val fileTabs = SimpleFlatMenu(SpaceLayout.GrowRight, Color.rgbInt(160, 160, 220))
    fileTabs.addController(fileTabsController)

    val fileTreeController = TreeViewController(
        fileTree,
        { parent -> Point(parent.x + Coordinate.percentage(20), parent.y) },
        { child -> Point(child.x - Coordinate.percentage(20), child.y) },
        { node, indices, position, components, refreshController ->
            val componentPosition = position ?: Point.percentage(0, 100)
            val region = RectRegion(
                componentPosition.x, componentPosition.y - Coordinate.percentage(12),
                componentPosition.x + Coordinate.percentage(80), componentPosition.y
            )

            if (node.element.isDirectory) {
                components.add(Pair(TextButton(node.element.name, directoryIcon, fileButtonStyle) { _, _ ->
                    val files = node.element.listFiles()
                    if (files == null) {
                        // Edge case: the directory has been removed, or is no longer a directory
                        val parent = fileTree.getChild(indices.subList(0, indices.size - 1))
                        parent.children = null
                        refreshController()
                    } else {
                        if (node.children == null) {
                            val directories = files.filter { it.isDirectory }
                            val modelFiles = files.filter { !it.isDirectory && it.name.endsWith(".pm2") }
                            node.children = (directories + modelFiles).map {
                                TreeViewController.Node(it, null)
                            }.toMutableList()
                        } else node.children = null
                        refreshController()
                    }
                }, region))
            } else if (node.element.name.endsWith(".pm2")){
                components.add(Pair(TextButton(
                    node.element.name.substring(0, node.element.name.length - 4),
                    fileIcon, fileButtonStyle) { _, _ ->
                        OpenFile.open(
                            node.element, openFiles, content, errorComponent,
                            fileTabsController::refresh, pm2Instance
                        )
                }, region))
            }

            Point(region.minX, region.minY)
        }
    )
    val fileTreeMenu = SimpleFlatMenu(SpaceLayout.GrowDown, Color.rgbInt(150, 150, 200))
    fileTreeMenu.addController(fileTreeController)

    rootMenu.addComponent(upperBar, RectRegion.percentage(0, 95, 100, 100))
    rootMenu.addComponent(fileTreeMenu, RectRegion.percentage(0, 0, 20, 95))
    rootMenu.addComponent(fileTabs, RectRegion.percentage(20, 90, 100, 95))
    rootMenu.addComponent(content, RectRegion.percentage(20, 5, 100, 90))
    rootMenu.addComponent(errorComponent, RectRegion.percentage(20, 0, 100, 5))

    rootMenu.addController(SaveOnQuitController(openFiles, pm2Instance))

    return rootMenu
}

fun main() {
    val graviksWindow = GraviksWindow(
        1600, 900, true,
        "Pm2Editor", VK_MAKE_VERSION(0, 1, 0), true
    ) { instance, width, height -> GraviksContext(instance, width, height) }

    createAndControlGruviksWindow(graviksWindow, createPm2Editor(graviksWindow.troll))
}
