package dragons.vr

import dragons.plugin.interfaces.vulkan.VulkanStaticMemoryUser
import dragons.state.StaticGraphicsState
import dragons.geometry.Angle
import dragons.geometry.Distance
import dragons.vulkan.RenderImageInfo
import dragons.vulkan.memory.VulkanImage
import dragons.vulkan.memory.claim.ImageMemoryClaim
import dragons.vulkan.queue.QueueManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.joml.Math.toRadians
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPhysicalDevice
import java.lang.System.currentTimeMillis
import java.lang.Thread.sleep

class DummyVrManager(
    val instanceExtensions: Set<String> = setOf(),
    val deviceExtensions: Set<String> = setOf()
): VrManager {

    private var requestedStop = false
    private val leftResolveImage = CompletableDeferred<VulkanImage>()
    private val rightResolveImage = CompletableDeferred<VulkanImage>()

    private lateinit var vkDevice: VkDevice
    private lateinit var queueManager: QueueManager
    private lateinit var resolveHelper: ResolveHelper

    override fun getVulkanInstanceExtensions(availableExtensions: Set<String>): Set<String> {
        return instanceExtensions
    }

    override fun getVulkanDeviceExtensions(
        device: VkPhysicalDevice, deviceName: String, availableExtensions: Set<String>
    ): Set<String> {
        return deviceExtensions
    }

    // These values are pretty arbitrary, but some decision had to be made
    override fun getWidth() = 1600
    override fun getHeight() = 900

    override fun claimStaticMemory(
        agent: VulkanStaticMemoryUser.Agent, queueManager: QueueManager, renderImageInfo: RenderImageInfo
    ) {
        val width = this.getWidth()
        val height = this.getHeight()

        for (resolveImage in arrayOf(this.leftResolveImage, this.rightResolveImage)) {
            agent.claims.images.add(
                ImageMemoryClaim(
                    width = width, height = height,
                    queueFamily = queueManager.generalQueueFamily,
                    imageFormat = renderImageInfo.colorFormat,
                    tiling = VK_IMAGE_TILING_OPTIMAL, samples = VK_SAMPLE_COUNT_1_BIT,
                    // These images are only used as resolve target and as the source of an image to buffer copy
                    imageUsage = VK_IMAGE_USAGE_TRANSFER_SRC_BIT or VK_IMAGE_USAGE_TRANSFER_DST_BIT,
                    initialLayout = VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                    aspectMask = VK_IMAGE_ASPECT_COLOR_BIT, accessMask = VK_ACCESS_TRANSFER_READ_BIT,
                    dstPipelineStageMask = VK_PIPELINE_STAGE_TRANSFER_BIT, prefill = null, storeResult = resolveImage
                )
            )
        }
    }

    override fun setGraphicsState(graphicsState: StaticGraphicsState) {
        this.vkDevice = graphicsState.boiler.vkDevice()
        this.queueManager = graphicsState.queueManager

        // Note: the resolve images should be finished by now, so this shouldn't block for long
        val (leftResolveImage, rightResolveImage) = runBlocking { Pair(leftResolveImage.await(), rightResolveImage.await()) }

        this.resolveHelper = ResolveHelper(
            graphicsState = graphicsState,
            defaultResolveImageLayout = VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
            defaultResolveImageStageMask = VK_PIPELINE_STAGE_TRANSFER_BIT,
            defaultResolveImageDstAccessMask = VK_ACCESS_TRANSFER_READ_BIT,
            leftResolveImages = arrayOf(leftResolveImage),
            rightResolveImages = arrayOf(rightResolveImage),
        )
    }

    private var lastRenderTime: Long? = null

    override fun prepareRender(nearPlane: Distance, farPlane: Distance, extraRotationY: Angle): CameraMatrices {
        if (lastRenderTime != null) {
            // This should cause a framerate of ~90 fps
            val nextRenderTime = lastRenderTime!! + 1000 / 90
            val currentTime = currentTimeMillis()
            if (currentTime < nextRenderTime) {
                sleep(nextRenderTime - currentTime)
            }
        }

        val projectionMatrix = Matrix4f().scale(1f, -1f, 1f).perspective(
            toRadians(70f),
            getWidth().toFloat() / getHeight().toFloat(),
            nearPlane.meters.toFloat(), farPlane.meters.toFloat(), true
        )

        // Let the camera rotate slowly
        val viewMatrix = Matrix4f()
            .rotateXYZ(toRadians(20f), toRadians(((currentTimeMillis() / 10) % 360).toFloat()) - extraRotationY.radians, 0f)
            .translate(0f, -1.7f, 0f)
        val combinedMatrix = projectionMatrix.mul(viewMatrix)

        lastRenderTime = currentTimeMillis()
        return CameraMatrices(
            averageRealEyePosition = Vector3f(0f, 1.7f, 0f),
            averageVirtualEyePosition = Vector3f(0f, 1.7f, 0f),
            averageViewMatrix = viewMatrix,
            leftEyeMatrix = Matrix4f(combinedMatrix).translate(-0.04f, 0f, 0f),
            rightEyeMatrix = Matrix4f(combinedMatrix).translate(0.04f, 0f, 0f)
        )
    }

    override fun markFirstFrameQueueSubmit() {
        // We don't need to do anything here
    }

    override fun resolveAndSubmitFrames(waitSemaphore: Long?, takeScreenshot: Boolean) {
        if (waitSemaphore != null) {
            this.resolveHelper.resolve(
                this.vkDevice, 0, 0,
                this.queueManager, waitSemaphore, takeScreenshot
            )
        }
    }

    override fun destroy() {
        vkDeviceWaitIdle(vkDevice)
        this.resolveHelper.destroy(vkDevice)
    }

    override fun requestStop() {
        this.requestedStop = true
    }

    override fun shouldStop() = requestedStop
}
