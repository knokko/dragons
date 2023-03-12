package dragons.plugins.standard.vulkan.panel

import dragons.vulkan.memory.VulkanImage
import graviks2d.context.GraviksContext
import graviks2d.context.TranslucentPolicy
import graviks2d.core.GraviksInstance
import kotlinx.coroutines.CompletableDeferred
import org.lwjgl.vulkan.VK10.*
import java.util.concurrent.ArrayBlockingQueue

class Panel(
    graviksInstance: GraviksInstance,
    val image: VulkanImage
) {
    private val graviks = GraviksContext(
        instance = graviksInstance, width = this.width, height = this.height,
        translucentPolicy = TranslucentPolicy.Manual, vertexBufferSize = 50_000, operationBufferSize = 250_000
    )

    private var imageLayout = VK_IMAGE_LAYOUT_UNDEFINED

    val width: Int
    get() = this.image.width

    val height: Int
    get() = this.image.height

    private val renderQueue = ArrayBlockingQueue<() -> Boolean>(100)

    private val thread: Thread = Thread {
        while (true) {
            val nextAction = this.renderQueue.take()
            val shouldContinue = nextAction()
            if (!shouldContinue) break
        }
    }

    init {
        this.thread.start()
    }

    fun execute(action: (GraviksContext) -> Unit) {
        this.renderQueue.put {
            action(this.graviks)
            true
        }
    }

    fun updateImage(signalSemaphore: Long, submissionMarker: CompletableDeferred<Unit>) {
        this.execute {
            this.graviks.copyColorImageTo(
                destImage = this.image.handle, destBuffer = null, destImageFormat = VK_FORMAT_R8G8B8A8_UNORM,
                originalImageLayout = this.imageLayout,
                finalImageLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                imageSrcAccessMask = VK_ACCESS_SHADER_READ_BIT, imageSrcStageMask = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                imageDstAccessMask = VK_ACCESS_SHADER_READ_BIT, imageDstStageMask = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                signalSemaphore = signalSemaphore, submissionMarker = submissionMarker, shouldAwaitCompletion = false
            )
            this.imageLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL
        }
    }

    fun destroy() {
        this.renderQueue.put { false }
        this.thread.join()
        this.graviks.destroy()
    }
}
