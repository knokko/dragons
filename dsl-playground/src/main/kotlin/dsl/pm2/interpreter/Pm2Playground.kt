package dsl.pm2.interpreter

import dsl.pm2.interpreter.program.Pm2Program
import dsl.pm2.renderer.Pm2Instance
import dsl.pm2.ui.Pm2PreviewComponent
import graviks.glfw.GraviksWindow
import graviks2d.context.GraviksContext
import graviks2d.resource.text.TextStyle
import graviks2d.util.Color
import gruviks.component.menu.SimpleFlatMenu
import gruviks.component.text.*
import gruviks.glfw.createAndControlGruviksWindow
import gruviks.space.RectRegion
import gruviks.space.SpaceLayout
import org.lwjgl.vulkan.VK10.*
import java.io.File
import java.io.IOException
import java.lang.System.currentTimeMillis
import java.nio.charset.StandardCharsets
import java.nio.file.Files

fun main() {
    val sourceFile = File("pm2-models/playground.pm2").toPath()
    val initialProgramCode: String = try {
        Files.readString(sourceFile)
    } catch (notFound: IOException) {
        "Insert source code here"
    }
    val initialModel = try {
        Pm2Program.compile(initialProgramCode).run(emptyMap())
    } catch (compileError: Pm2CompileError) { null }

    val graviksWindow = GraviksWindow(
        800, 800, true, "DSL playground",
        VK_MAKE_VERSION(0, 1, 0), true
    ) { instance, width, height ->
        GraviksContext(instance, width, height)
    }

    val width = 500
    val height = 500

    val pm2Instance = Pm2Instance(graviksWindow.troll)
    val errorComponent = TextComponent("", TextStyle(fillColor = Color.RED, font = null))
    val previewComponent = Pm2PreviewComponent(pm2Instance, initialModel, width, height, errorComponent::setText)

    val codeArea = TextArea(initialProgramCode, squareTextAreaStyle(
        defaultTextStyle = TextStyle(fillColor = Color.rgbInt(0, 100, 0), font = null),
        defaultBackgroundColor = Color.rgbInt(20, 20, 20),
        focusTextStyle = TextStyle(fillColor = Color.rgbInt(0, 150, 0), font = null),
        focusBackgroundColor = Color.rgbInt(40, 40, 40),
        lineHeight = 0.04f,
        placeholderStyle = null
    ))

    val playgroundMenu = SimpleFlatMenu(SpaceLayout.Simple, Color.WHITE)
    playgroundMenu.addComponent(errorComponent, RectRegion.percentage(0, 95, 100, 100))
    playgroundMenu.addComponent(codeArea, RectRegion.percentage(1, 20, 99, 95))
    playgroundMenu.addComponent(previewComponent, RectRegion.percentage(0, 0, 20, 20))
    playgroundMenu.addComponent(TextButton("Recompile", icon = null, style = TextButtonStyle.textAndBorder(
        baseColor = Color.rgbInt(100, 100, 200),
        hoverColor = Color.rgbInt(70, 70, 255)
    )) { _, _ ->
        try {
            val startTime = currentTimeMillis()
            val newProgram = Pm2Program.compile(codeArea.getText())
            val time1 = currentTimeMillis()
            val newModel = newProgram.run(emptyMap())
            val time2 = currentTimeMillis()

            println("Compilation took ${time1 - startTime} ms and running took ${time2 - time1} ms")
            previewComponent.updateModel(newModel)

            errorComponent.setText("")
        } catch (compileError: Pm2CompileError) {
            errorComponent.setText(compileError.message!!)
        } catch (runtimeError: Pm2RuntimeError) {
            errorComponent.setText(runtimeError.message!!)
        }
    }, RectRegion.percentage(25, 7, 60, 13))

    playgroundMenu.addComponent(TextButton("Save", icon = null, style = TextButtonStyle.textAndBorder(
        baseColor = Color.rgbInt(100, 100, 200),
        hoverColor = Color.rgbInt(70, 70, 255)
    )) { _, _ ->
        Files.write(sourceFile, codeArea.getText().toByteArray(StandardCharsets.UTF_8))
    }, RectRegion.percentage(70, 7, 100, 13))

    createAndControlGruviksWindow(graviksWindow, playgroundMenu, pm2Instance::destroy)
}
