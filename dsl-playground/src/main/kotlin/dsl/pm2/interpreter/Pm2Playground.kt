package dsl.pm2.interpreter

import dsl.pm2.interpreter.program.Pm2Program
import dsl.pm2.renderer.Pm2Instance
import dsl.pm2.renderer.Pm2Scene
import dsl.pm2.renderer.checkReturnValue
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
import org.lwjgl.util.vma.VmaAllocationCreateInfo
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkImageCreateInfo
import org.lwjgl.vulkan.VkImageViewCreateInfo
import org.lwjgl.vulkan.VkSemaphoreCreateInfo
import java.io.File
import java.lang.System.currentTimeMillis
import java.nio.charset.StandardCharsets
import java.nio.file.Files

fun main() {
    val initialProgramCode = Files.readString(File("pm2-models/playground.pm2").toPath())
    val initialVertices = try {
        Pm2Program.compile(initialProgramCode).run()
    } catch (compileError: Pm2CompileError) { listOf(
        Pm2Vertex(-0.8f, -0.7f, Color.RED),
        Pm2Vertex(0.8f, -0.2f, Color.GREEN),
        Pm2Vertex(-0.9f, 0.8f, Color.BLUE)
    )}

    val graviksWindow = GraviksWindow(
        800, 800, "DSL Playground", true, "Gruviks Tester",
        VK_MAKE_VERSION(0, 1, 0), true
    ) { instance, width, height ->
        GraviksContext(instance, width, height)
    }

    val width = 500
    val height = 500

    val sceneImage: Long
    val sceneImageView: Long
    val sceneImageAllocation: Long
    val sceneSemaphore: Long
    stackPush().use { stack ->
        val ciImage = VkImageCreateInfo.calloc(stack)
        ciImage.`sType$Default`()
        ciImage.flags(0)
        ciImage.imageType(VK_IMAGE_TYPE_2D)
        ciImage.format(VK_FORMAT_R8G8B8A8_SRGB)
        ciImage.extent().set(width, height, 1)
        ciImage.mipLevels(1)
        ciImage.arrayLayers(1)
        ciImage.samples(VK_SAMPLE_COUNT_1_BIT)
        ciImage.tiling(VK_IMAGE_TILING_OPTIMAL)
        ciImage.usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT)
        ciImage.sharingMode(VK_SHARING_MODE_EXCLUSIVE)
        ciImage.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)

        val ciAllocation = VmaAllocationCreateInfo.calloc(stack)
        ciAllocation.usage(VMA_MEMORY_USAGE_AUTO)

        val pImage = stack.callocLong(1)
        val pAllocation = stack.callocPointer(1)

        checkReturnValue(vmaCreateImage(
            graviksWindow.graviksInstance.vmaAllocator, ciImage, ciAllocation, pImage, pAllocation, null
        ), "VmaCreateImage")

        sceneImage = pImage[0]
        sceneImageAllocation = pAllocation[0]

        val ciView = VkImageViewCreateInfo.calloc(stack)
        ciView.`sType$Default`()
        ciView.flags(0)
        ciView.image(sceneImage)
        ciView.viewType(VK_IMAGE_VIEW_TYPE_2D)
        ciView.format(ciImage.format())
        ciView.components().set(
            VK_COMPONENT_SWIZZLE_IDENTITY, VK_COMPONENT_SWIZZLE_IDENTITY,
            VK_COMPONENT_SWIZZLE_IDENTITY, VK_COMPONENT_SWIZZLE_IDENTITY
        )
        ciView.subresourceRange {
            it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
            it.baseMipLevel(0)
            it.levelCount(1)
            it.baseArrayLayer(0)
            it.layerCount(1)
        }

        val pView = stack.callocLong(1)
        checkReturnValue(vkCreateImageView(
            graviksWindow.graviksInstance.device, ciView, null, pView
        ), "CreateImageView")
        sceneImageView = pView[0]

        val ciSemaphore = VkSemaphoreCreateInfo.calloc(stack)
        ciSemaphore.`sType$Default`()
        ciSemaphore.flags(0)

        val pSemaphore = stack.callocLong(1)
        checkReturnValue(vkCreateSemaphore(
            graviksWindow.graviksInstance.device, ciSemaphore, null, pSemaphore
        ), "CreateSemaphore")

        sceneSemaphore = pSemaphore[0]
    }

    val pm2Instance = Pm2Instance(
        graviksWindow.graviksInstance.device,
        graviksWindow.graviksInstance.vmaAllocator,
        graviksWindow.graviksInstance.queueFamilyIndex
    )

    var currentMesh = pm2Instance.allocations.allocateMesh(initialVertices)

    // TODO Create some method for this
    val pm2Scene = Pm2Scene(
        graviksWindow.graviksInstance.device,
        graviksWindow.graviksInstance.vmaAllocator,
        graviksWindow.graviksInstance.queueFamilyIndex,
        20, 200, 250,
        width, height
    )

    var oldSceneLayout = VK_IMAGE_LAYOUT_UNDEFINED
    var sceneAccessMask = 0
    var sceneStageMask = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT

    val sceneComponent = Pm2SceneComponent(currentMesh) {
        pm2Scene.drawAndCopy(
            pm2Instance, listOf(it), sceneSemaphore, graviksWindow.graviksInstance::synchronizedQueueSubmit,
            destImage = sceneImage, oldLayout = oldSceneLayout, srcAccessMask = sceneAccessMask, srcStageMask = sceneStageMask,
            newLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, dstAccessMask = VK_ACCESS_SHADER_READ_BIT,
            dstStageMask = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
            offsetX = 0, offsetY = 0, blitSizeX = width, blitSizeY = height
        )
        oldSceneLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL
        sceneAccessMask = VK_ACCESS_SHADER_READ_BIT
        sceneStageMask = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
        Triple(sceneImage, sceneImageView, sceneSemaphore)
    }

    val codeArea = TextArea(initialProgramCode, squareTextAreaStyle(
        defaultTextStyle = TextStyle(fillColor = Color.rgbInt(0, 100, 0), font = null),
        defaultBackgroundColor = Color.rgbInt(20, 20, 20),
        focusTextStyle = TextStyle(fillColor = Color.rgbInt(0, 150, 0), font = null),
        focusBackgroundColor = Color.rgbInt(40, 40, 40),
        lineHeight = 0.04f,
        placeholderStyle = null
    ))

    val errorComponent = TextComponent("", TextStyle(fillColor = Color.RED, font = null))

    val playgroundMenu = SimpleFlatMenu(SpaceLayout.Simple, Color.WHITE)
    playgroundMenu.addComponent(errorComponent, RectRegion.percentage(0, 95, 100, 100))
    playgroundMenu.addComponent(codeArea, RectRegion.percentage(0, 20, 100, 95))
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
        Files.write(File("pm2-models/playground.pm2").toPath(), codeArea.getText().toByteArray(StandardCharsets.UTF_8))
    }, RectRegion.percentage(70, 7, 100, 13))

    createAndControlGruviksWindow(graviksWindow, playgroundMenu) {
        pm2Instance.allocations.destroyMesh(currentMesh)
        pm2Scene.destroy()
        pm2Instance.destroy()

        vkDestroySemaphore(graviksWindow.graviksInstance.device, sceneSemaphore, null)
        vkDestroyImageView(graviksWindow.graviksInstance.device, sceneImageView, null)
        vmaDestroyImage(graviksWindow.graviksInstance.vmaAllocator, sceneImage, sceneImageAllocation)
    }
}
