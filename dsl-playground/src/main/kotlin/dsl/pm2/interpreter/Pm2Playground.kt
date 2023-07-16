package dsl.pm2.interpreter

import dsl.pm2.interpreter.program.Pm2Program
import dsl.pm2.renderer.Pm2Instance
import dsl.pm2.renderer.Pm2Scene
import dsl.pm2.ui.Pm2SceneComponent
import graviks.glfw.GraviksWindow
import graviks2d.context.GraviksContext
import graviks2d.resource.text.TextStyle
import graviks2d.util.Color
import gruviks.component.menu.SimpleFlatMenu
import gruviks.component.text.*
import gruviks.glfw.createAndControlGruviksWindow
import gruviks.space.RectRegion
import gruviks.space.SpaceLayout
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.vulkan.VK10.*
import troll.images.VmaImage
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
        Pm2Program.compile(initialProgramCode).run()
    } catch (compileError: Pm2CompileError) { Pm2Model(listOf(
        Pm2Vertex(-0.8f, -0.7f, Color.RED, 0),
        Pm2Vertex(0.8f, -0.2f, Color.GREEN, 0),
        Pm2Vertex(-0.9f, 0.8f, Color.BLUE, 0)
    ), emptyList())}

    val graviksWindow = GraviksWindow(
        800, 800, true, "DSL playground",
        VK_MAKE_VERSION(0, 1, 0), true
    ) { instance, width, height ->
        GraviksContext(instance, width, height)
    }

    val width = 500
    val height = 500

    val sceneImage: VmaImage
    val sceneSemaphore: Long
    stackPush().use { stack ->
        sceneImage = graviksWindow.troll.images.createSimple(
            stack, width, height, VK_FORMAT_R8G8B8A8_SRGB, VK_SAMPLE_COUNT_1_BIT,
            VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT,
            VK_IMAGE_ASPECT_COLOR_BIT, "Pm2SceneIntermediateImage"
        )
        sceneSemaphore = graviksWindow.troll.sync.createSemaphores("Pm2SceneIntermediateSemaphore", 1)[0]
    }

    val pm2Instance = Pm2Instance(graviksWindow.troll)

    var currentMesh = pm2Instance.allocations.allocateMesh(initialModel)

    val pm2Scene = Pm2Scene(
        graviksWindow.troll,
        pm2Instance.descriptorSetLayout,
        20, 200, 250,
        width, height
    )

    var oldSceneLayout = VK_IMAGE_LAYOUT_UNDEFINED
    var sceneAccessMask = 0
    var sceneStageMask = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT

    val errorComponent = TextComponent("", TextStyle(fillColor = Color.RED, font = null))

    val sceneComponent = Pm2SceneComponent(currentMesh) { mesh, cameraMatrix ->
        try {
            pm2Scene.drawAndCopy(
                    pm2Instance, listOf(mesh), cameraMatrix, sceneSemaphore,
                    destImage = sceneImage.vkImage, oldLayout = oldSceneLayout,
                    srcAccessMask = sceneAccessMask, srcStageMask = sceneStageMask,
                    newLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, dstAccessMask = VK_ACCESS_SHADER_READ_BIT,
                    dstStageMask = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                    offsetX = 0, offsetY = 0, blitSizeX = width, blitSizeY = height
            )
            oldSceneLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL
            sceneAccessMask = VK_ACCESS_SHADER_READ_BIT
            sceneStageMask = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
            Triple(sceneImage.vkImage, sceneImage.vkImageView, sceneSemaphore)
        } catch (runtimeError: Pm2RuntimeError) {
            errorComponent.setText(runtimeError.message!!)
            null
        }
    }

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
    playgroundMenu.addComponent(sceneComponent, RectRegion.percentage(0, 0, 20, 20))
    playgroundMenu.addComponent(TextButton("Recompile", icon = null, style = TextButtonStyle.textAndBorder(
        baseColor = Color.rgbInt(100, 100, 200),
        hoverColor = Color.rgbInt(70, 70, 255)
    )) { _, _ ->
        try {
            val startTime = currentTimeMillis()
            val newProgram = Pm2Program.compile(codeArea.getText())
            val time1 = currentTimeMillis()
            val newVertices = newProgram.run()
            val time2 = currentTimeMillis()
            val newMesh = pm2Instance.allocations.allocateMesh(newVertices)
            val endTime = currentTimeMillis()

            println("Compilation took ${time1 - startTime} ms; Running took ${time2 - time1} ms; Creating mesh took ${endTime - time2} ms")

            pm2Scene.awaitLastDraw()
            pm2Instance.allocations.destroyMesh(currentMesh)
            currentMesh = newMesh
            sceneComponent.setMesh(currentMesh)
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

    createAndControlGruviksWindow(graviksWindow, playgroundMenu) {
        pm2Instance.allocations.destroyMesh(currentMesh)
        pm2Scene.destroy()
        pm2Instance.destroy()

        vkDestroySemaphore(graviksWindow.troll.vkDevice(), sceneSemaphore, null)
        vkDestroyImageView(graviksWindow.troll.vkDevice(), sceneImage.vkImageView, null)
        vmaDestroyImage(graviksWindow.troll.vmaAllocator(), sceneImage.vkImage, sceneImage.vmaAllocation)
    }
}
