package dsl.pm2.ui

import dsl.pm2.interpreter.*
import dsl.pm2.interpreter.importer.Pm2FileImportFunctions
import dsl.pm2.interpreter.importer.Pm2ImportCache
import dsl.pm2.interpreter.importer.Pm2Importer
import dsl.pm2.interpreter.program.Pm2Program
import dsl.pm2.interpreter.value.Pm2NoneValue
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

private val rootDirectory = File("pm2-models")

private val backgroundColor = Color.rgbInt(30, 0, 90)

private val textAreaStyle = squareTextAreaStyle(
    defaultTextStyle = TextStyle(fillColor = Color.WHITE.scale(0.8f), font = null),
    defaultBackgroundColor = backgroundColor,
    focusTextStyle = TextStyle(fillColor = Color.WHITE, font = null),
    focusBackgroundColor = backgroundColor.scale(1.1f),
    lineHeight = 0.05f, placeholderStyle = null
)

private fun createImporter(sourceFile: File): Pm2Importer {
    val rootPath = rootDirectory.absolutePath
    val sourcePath = sourceFile.absoluteFile.parentFile.absolutePath
    if (!sourcePath.startsWith(rootPath)) {
        throw Error("The source $sourcePath is not a descendant from the root $rootPath")
    }

    var prefix = sourcePath.substring(rootPath.length)
    if (prefix.startsWith("/") || prefix.startsWith("\\")) prefix = prefix.substring(1)
    return Pm2Importer(Pm2ImportCache(Pm2FileImportFunctions(rootDirectory)), prefix)
}

private class OpenFile(
    val modelFile: File,
    val parametersFile: File?,
    val textArea: TextArea,
    private val createPreview: () -> Pm2PreviewComponent
) {
    val preview = lazy { createPreview() }

    fun updatePreview(errorComponent: TextComponent, openFiles: List<OpenFile>) {
        val modelContent = if (parametersFile == null) textArea.getText() else try {
            val openFile = openFiles.find { it.parametersFile == null && it.modelFile == this.modelFile }
            openFile?.textArea?.getText() ?: Files.readString(modelFile.toPath())
        } catch (failed: Throwable) {
            errorComponent.setText("Failed to open model file: " + failed.message)
            return
        }

        val parameters = if (parametersFile != null) {
            try {
                Pm2Program.compile(textArea.getText(), createImporter(parametersFile)).getResultValue()
            } catch (compileError: Pm2CompileError) {
                errorComponent.setText(compileError.message ?: "Failed to compile parameters")
                compileError.printStackTrace()
                return
            } catch (runtimeError: Pm2RuntimeError) {
                errorComponent.setText(runtimeError.message ?: "Failed to run parameters")
                return
            }
        } else Pm2NoneValue()

        try {
            val startTime = System.currentTimeMillis()
            val newProgram = Pm2Program.compile(modelContent, createImporter(modelFile))
            val time1 = System.currentTimeMillis()
            val newModel = newProgram.run(parameters)
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
        val file = parametersFile ?: modelFile
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
            val modelFile: File
            val parametersFile: File?
            if (file.name.endsWith(".pm2")) {
                modelFile = file
                parametersFile = null
            } else if (file.name.endsWith(".pv2")) {
                val currentDirectory = file.parentFile
                if (currentDirectory == null) {
                    errorComponent.setText("Can't find directory containing this file")
                    return
                }

                val parentDirectory = currentDirectory.parentFile
                if (parentDirectory == null) {
                    errorComponent.setText("Can't find parent directory of this file")
                    return
                }

                modelFile = File("$parentDirectory/${currentDirectory.name}.pm2")
                parametersFile = file
            } else {
                errorComponent.setText("Unexpected file type: $file")
                return
            }

            var openFile = openFiles.find { it.modelFile == modelFile && it.parametersFile == parametersFile }
            if (openFile != null) openFiles.remove(openFile)
            if (openFile == null) {
                val modelContent: String
                val parameterContent: String?
                try {
                    modelContent = Files.readString(modelFile.toPath())
                    parameterContent = if (parametersFile != null) Files.readString(parametersFile.toPath())
                    else null
                } catch (failedToOpen: Throwable) {
                    errorComponent.setText(failedToOpen.message ?: "Failed to open file")
                    failedToOpen.printStackTrace()
                    return
                }

                val initialParameters = if (parameterContent != null) {
                    try {
                        val parameterProgram = Pm2Program.compile(parameterContent, createImporter(parametersFile!!))
                        parameterProgram.getResultValue()
                    } catch (compileError: Pm2CompileError) {
                        compileError.printStackTrace()
                        errorComponent.setText(compileError.message ?: "Parameter compile error")
                        Pm2NoneValue()
                    } catch (runtimeError: Pm2RuntimeError) {
                        errorComponent.setText(runtimeError.message ?: "Parameter runtime error")
                        Pm2NoneValue()
                    }
                } else Pm2NoneValue()

                val initialModel = try {
                    Pm2Program.compile(modelContent, createImporter(modelFile)).run(initialParameters)
                } catch (compileError: Pm2CompileError) {
                    compileError.printStackTrace()
                    null
                } catch (runtimeError: Pm2RuntimeError) {
                    null
                }

                val width = 1600
                val height = 900 // TODO Maybe stop hardcoding this?

                val textArea = TextArea(parameterContent ?: modelContent, textAreaStyle, null)
                val createPreview = { Pm2PreviewComponent(pm2Instance, initialModel, width, height, errorComponent::setText) }
                openFile = OpenFile(modelFile, parametersFile, textArea, createPreview)
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
    baseColor = Color.WHITE.scale(0.8f),
    hoverColor = Color.WHITE,
    font = null
)

private val fileButtonStyle = TextButtonStyle(
    baseTextStyle = TextStyle(fillColor = Color.WHITE.scale(0.8f), font = null),
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
private val parametersIcon = ImageReference.classLoaderPath("dsl/pm2/ui/parameters.png", false)

fun createPm2Editor(troll: TrollInstance): Component {
    val pm2Instance = Pm2Instance(troll)
    val rootMenu = SimpleFlatMenu(SpaceLayout.Simple, backgroundColor.scale(1.2f))

    val openFiles = mutableListOf<OpenFile>()
    val fileTree = TreeViewController.Node(rootDirectory, null)

    val errorComponent = TextComponent("", TextStyle(fillColor = Color.RED, font = null))
    val contentBackground = SimpleColorFillComponent(backgroundColor)
    val content = SwitchComponent(contentBackground)
    val upperBar = SimpleFlatMenu(SpaceLayout.GrowRight, backgroundColor.scale(1.3f))

    val fileTabsController = SimpleListViewController(openFiles) { element, _, position, components, refreshController ->
        val componentPosition = position ?: Point.percentage(0, 0)
        val region = RectRegion(
            componentPosition.x, Coordinate.percentage(0),
            componentPosition.x + Coordinate.percentage(480), Coordinate.percentage(100)
        )

        val fileName = (element.parametersFile ?: element.modelFile).name
        val simpleFileName = fileName.substring(0 until fileName.length - 4)
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
                element.updatePreview(errorComponent, openFiles)
            }
        }, region))

        Point(region.boundX, Coordinate.percentage(0))
    }
    val fileTabs = SimpleFlatMenu(SpaceLayout.GrowRight, backgroundColor.scale(1.2f))
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
                            val valueFiles = files.filter { !it.isDirectory && it.name.endsWith(".pv2") }
                            node.children = (directories + modelFiles + valueFiles).map {
                                TreeViewController.Node(it, null)
                            }.toMutableList()
                        } else node.children = null
                        refreshController()
                    }
                }, region))
            } else if (node.element.name.endsWith(".pm2") || node.element.name.endsWith(".pv2")) {
                val icon = if (node.element.name.endsWith(".pm2")) fileIcon else parametersIcon
                val simpleName = node.element.name.substring(0, node.element.name.length - 4)
                components.add(Pair(TextButton(simpleName, icon, fileButtonStyle) { _, _ ->
                    OpenFile.open(
                        node.element, openFiles, content, errorComponent,
                        fileTabsController::refresh, pm2Instance
                    )
                }, region))
            }

            Point(region.minX, region.minY)
        }
    )
    val fileTreeMenu = SimpleFlatMenu(SpaceLayout.GrowDown, backgroundColor.scale(1.4f))
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
