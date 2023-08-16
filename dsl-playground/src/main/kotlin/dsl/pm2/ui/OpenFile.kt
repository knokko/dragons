package dsl.pm2.ui

import dsl.pm2.interpreter.Pm2CompileError
import dsl.pm2.interpreter.Pm2RuntimeError
import dsl.pm2.interpreter.importer.Pm2FileImportFunctions
import dsl.pm2.interpreter.importer.Pm2ImportCache
import dsl.pm2.interpreter.importer.Pm2Importer
import dsl.pm2.interpreter.program.Pm2Program
import dsl.pm2.interpreter.value.Pm2NoneValue
import dsl.pm2.parser.parseShape
import dsl.pm2.parser.writeShape
import dsl.pm2.renderer.Pm2Instance
import gruviks.component.Component
import gruviks.component.text.TextArea
import gruviks.component.text.TextComponent
import gruviks.component.util.SwitchComponent
import java.io.File
import java.io.IOException
import java.nio.file.Files

private fun createImporter(sourceFile: File, openFiles: List<OpenFile>): Pm2Importer {
    for (openFile in openFiles) openFile.save(false)

    val rootPath = rootDirectory.absolutePath
    val sourcePath = sourceFile.absoluteFile.parentFile.absolutePath
    if (!sourcePath.startsWith(rootPath)) {
        throw Error("The source $sourcePath is not a descendant from the root $rootPath")
    }

    var prefix = sourcePath.substring(rootPath.length)
    if (prefix.startsWith("/") || prefix.startsWith("\\")) prefix = prefix.substring(1)
    return Pm2Importer(Pm2ImportCache(Pm2FileImportFunctions(rootDirectory)), prefix)
}

internal class OpenFile(
    val modelFile: File,
    val parametersFile: File?,
    val textArea: TextArea?,
    val shapeEditor: Pm2ShapeEditor?,
    private val createPreview: () -> Pm2PreviewComponent?
) {
    val preview = lazy { createPreview() }

    fun getPrimaryComponent(): Component {
        if (textArea != null) return textArea
        return shapeEditor!!
    }

    fun updatePreview(errorComponent: TextComponent, openFiles: List<OpenFile>) {
        // Shape files are the only files without textArea, and also the only files without preview
        if (textArea == null) return

        val modelContent = if (parametersFile == null) textArea.getText() else try {
            val openFile = openFiles.find { it.parametersFile == null && it.modelFile == this.modelFile }
            openFile?.textArea?.getText() ?: Files.readString(modelFile.toPath())
        } catch (failed: Throwable) {
            errorComponent.setText("Failed to open model file: " + failed.message)
            return
        }

        val parameters = if (parametersFile != null) {
            try {
                Pm2Program.compile(textArea.getText(), createImporter(parametersFile, openFiles)).getResultValue()
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
            val newProgram = Pm2Program.compile(modelContent, createImporter(modelFile, openFiles))
            val time1 = System.currentTimeMillis()
            val newModel = newProgram.run(parameters)
            val time2 = System.currentTimeMillis()

            println("Compilation took ${time1 - startTime} ms and running took ${time2 - time1} ms")
            preview.value!!.updateModel(newModel)
            errorComponent.setText("")
        } catch (compileError: Pm2CompileError) {
            errorComponent.setText(compileError.message!!)
        } catch (runtimeError: Pm2RuntimeError) {
            errorComponent.setText(runtimeError.message!!)
        }
    }

    fun save(handleErrors: Boolean): Boolean {
        if (textArea != null) {
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
        } else {
            return try {
                val output = Files.newOutputStream(modelFile.toPath())
                writeShape(shapeEditor!!.points, shapeEditor.triangles, output)
                output.close()
                true
            } catch (failedToSave: IOException) {
                println("Failed to save $modelFile: ${failedToSave.message}")
                false
            }
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
            } else if (file.name.endsWith(".tri2")) {
                modelFile = file
                parametersFile = null
            } else {
                errorComponent.setText("Unexpected file type: $file")
                return
            }

            var openFile = openFiles.find { it.modelFile == modelFile && it.parametersFile == parametersFile }
            if (openFile != null) openFiles.remove(openFile)
            if (openFile == null) {
                if (file.name.endsWith(".pm2") || file.name.endsWith(".pv2")) {
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
                            val parameterProgram =
                                Pm2Program.compile(parameterContent, createImporter(parametersFile!!, openFiles))
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
                        Pm2Program.compile(modelContent, createImporter(modelFile, openFiles)).run(initialParameters)
                    } catch (compileError: Pm2CompileError) {
                        compileError.printStackTrace()
                        null
                    } catch (runtimeError: Pm2RuntimeError) {
                        null
                    }

                    val width = 1600
                    val height = 900 // TODO Maybe stop hardcoding this?

                    val textArea = TextArea(parameterContent ?: modelContent, textAreaStyle, null)
                    val createPreview =
                        { Pm2PreviewComponent(pm2Instance, initialModel, width, height, errorComponent::setText) }
                    openFile = OpenFile(modelFile, parametersFile, textArea, null, createPreview)
                } else if (file.name.endsWith("tri2")) {
                    val shapeEditor = if (file.length() == 0L) {
                        Pm2ShapeEditor(mutableSetOf(), mutableListOf())
                    } else {
                        try {
                            val input = Files.newInputStream(file.toPath())
                            val (vertexSet, triangleList) = parseShape(input)
                            input.close()
                            Pm2ShapeEditor(vertexSet.toMutableSet(), triangleList.toMutableList())
                        } catch (failed: Throwable) {
                            failed.printStackTrace()
                            errorComponent.setText("Failed to open triangle file: ${failed.message}")
                            return
                        }
                    }

                    openFile = OpenFile(modelFile, null, null, shapeEditor) { null }
                } else {
                    throw IllegalArgumentException("Unexpected file $file")
                }
            }

            openFiles.add(0, openFile)
            switchComponent.setComponent(openFile.getPrimaryComponent())
            updateController()
        }
    }
}