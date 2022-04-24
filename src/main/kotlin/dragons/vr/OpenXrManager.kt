package dragons.vr

import dragons.init.GameInitProperties
import dragons.state.StaticGraphicsState
import dragons.vr.controls.DragonControls
import dragons.vr.openxr.*
import dragons.vr.openxr.createOpenXrInstance
import dragons.vulkan.queue.DeviceQueue
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjgl.PointerBuffer
import org.lwjgl.openxr.*
import org.lwjgl.openxr.EXTDebugUtils.xrDestroyDebugUtilsMessengerEXT
import org.lwjgl.openxr.KHRVulkanEnable2.*
import org.lwjgl.openxr.XR10.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memAddress
import org.lwjgl.system.MemoryUtil.memUTF8
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import java.lang.Thread.sleep

internal fun tryInitOpenXR(initProps: GameInitProperties, logger: Logger): VrManager {

    val (xrInstance, xrDebugMessenger) = createOpenXrInstance(initProps, logger)
    if (xrInstance == null) {
        logger.info("Falling back to DummyVrManager...")
        return DummyVrManager()
    }

    val xrSystemId = createOpenXrSystem(initProps, logger, xrInstance)
    if (xrSystemId == null) {
        logger.info("Falling back to DummyVrManager...")
        return DummyVrManager()
    }

    checkOpenXrVulkanVersion(xrInstance, xrSystemId)
    val (width, height) = determineOpenXrSwapchainSize(xrInstance, xrSystemId)
    return OpenXrManager(xrInstance, xrDebugMessenger, xrSystemId, width, height)
}

internal class OpenXrManager(
    private val xrInstance: XrInstance,
    private val xrDebugMessenger: XrDebugUtilsMessengerEXT?,
    private val xrSystemId: Long,
    private val width: Int,
    private val height: Int
) : VrManager {

    private val lastViews = XrView.calloc(2)

    private lateinit var graphicsState: StaticGraphicsState
    private lateinit var vkQueue: DeviceQueue
    private lateinit var xrSession: XrSession
    private lateinit var sessionState: SessionState
    private lateinit var renderSpace: XrSpace
    private lateinit var swapchains: List<OpenXrSwapchain>
    private lateinit var resolveHelper: ResolveHelper
    private lateinit var input: XrInput

    private lateinit var acquiredSwapchainImageIndices: List<Int>

    private var didRender = false
    private var nextDisplayTime = 0L

    override fun createVulkanInstance(
        ciInstance: VkInstanceCreateInfo,
        pInstance: PointerBuffer
    ): Int {
        return stackPush().use { stack ->
            val ciVulkanInstance = XrVulkanInstanceCreateInfoKHR.calloc(stack)
            ciVulkanInstance.`type$Default`()
            ciVulkanInstance.systemId(xrSystemId)
            ciVulkanInstance.pfnGetInstanceProcAddr(VK.getFunctionProvider().getFunctionAddress("vkGetInstanceProcAddr"))
            ciVulkanInstance.vulkanCreateInfo(ciInstance)

            val pVulkanResult = stack.callocInt(1)
            assertXrSuccess(
                xrCreateVulkanInstanceKHR(xrInstance, ciVulkanInstance, pInstance, pVulkanResult),
                "CreateVulkanInstanceKHR"
            )
            pVulkanResult[0]
        }
    }

    override fun getVulkanInstanceExtensions(availableExtensions: Set<String>): Set<String> {
        // Required Vulkan extensions for the OpenXR runtime are handled in `createVulkanInstance` instead of here
        return emptySet()
    }

    override fun getVulkanDeviceExtensions(
        device: VkPhysicalDevice,
        deviceName: String,
        availableExtensions: Set<String>
    ): Set<String> {
        // Required Vulkan device extensions for the OpenXR runtime are handled in `createVulkanLogicalDevice` instead
        return emptySet()
    }

    override fun enumerateVulkanPhysicalDevices(
        logger: Logger,
        vkInstance: VkInstance,
        stack: MemoryStack
    ): PointerBuffer {

        val giDevice = XrVulkanGraphicsDeviceGetInfoKHR.calloc(stack)
        giDevice.`type$Default`()
        giDevice.systemId(xrSystemId)
        giDevice.vulkanInstance(vkInstance)

        val pPhysicalDevice = stack.callocPointer(1)
        assertXrSuccess(
            xrGetVulkanGraphicsDevice2KHR(xrInstance, giDevice, pPhysicalDevice),
            "GetVulkanGraphicsDevice2KHR"
        )

        val physicalDevice = VkPhysicalDevice(pPhysicalDevice[0], vkInstance)
        val deviceProperties = VkPhysicalDeviceProperties.calloc(stack)
        vkGetPhysicalDeviceProperties(physicalDevice, deviceProperties)
        logger.info("The OpenXR runtime wants the game to use physical device ${deviceProperties.deviceNameString()}")

        return pPhysicalDevice
    }

    override fun createVulkanLogicalDevice(
        physicalDevice: VkPhysicalDevice,
        ciDevice: VkDeviceCreateInfo,
        pDevice: PointerBuffer
    ): Int {
        return stackPush().use { stack ->

            val cixDevice = XrVulkanDeviceCreateInfoKHR.calloc(stack)
            cixDevice.`type$Default`()
            cixDevice.systemId(xrSystemId)
            cixDevice.pfnGetInstanceProcAddr(VK.getFunctionProvider().getFunctionAddress("vkGetInstanceProcAddr"))
            cixDevice.vulkanPhysicalDevice(physicalDevice)
            cixDevice.vulkanCreateInfo(ciDevice)

            val pVulkanResult = stack.callocInt(1)
            assertXrSuccess(
                xrCreateVulkanDeviceKHR(xrInstance, cixDevice, pDevice, pVulkanResult),
                "CreateVulkanDeviceKHR"
            )
            pVulkanResult[0]
        }
    }

    override fun getWidth() = width

    override fun getHeight() = height

    override fun setGraphicsState(graphicsState: StaticGraphicsState) {
        this.graphicsState = graphicsState
        this.vkQueue = graphicsState.queueManager.generalQueueFamily.getFirstPriorityQueue()

        stackPush().use { stack ->
            val graphicsBinding = XrGraphicsBindingVulkan2KHR.calloc(stack)
            graphicsBinding.`type$Default`()
            graphicsBinding.instance(graphicsState.vkInstance)
            graphicsBinding.physicalDevice(graphicsState.vkPhysicalDevice)
            graphicsBinding.device(graphicsState.vkDevice)
            graphicsBinding.queueFamilyIndex(graphicsState.queueManager.generalQueueFamily.index)
            graphicsBinding.queueIndex(graphicsState.queueManager.generalQueueFamily.getFirstPriorityQueueIndex())

            val ciSession = XrSessionCreateInfo.calloc(stack)
            ciSession.`type$Default`()
            ciSession.next(graphicsBinding.address())
            ciSession.systemId(xrSystemId)

            val pSession = stack.callocPointer(1)
            assertXrSuccess(xrCreateSession(xrInstance, ciSession, pSession), "CreateSession")
            this.xrSession = XrSession(pSession[0], xrInstance)
        }

        this.sessionState = SessionState(xrInstance, xrSession)
        this.renderSpace = createRenderSpace(xrSession)

        this.swapchains = createOpenXrSwapchains(xrSession, graphicsState, width, height)
        this.resolveHelper = ResolveHelper(
            graphicsState = graphicsState,
            defaultResolveImageLayout = VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
            defaultResolveImageStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
            defaultResolveImageDstAccessMask = VK_ACCESS_COLOR_ATTACHMENT_READ_BIT,
            leftResolveImages = this.swapchains[0].images.toTypedArray(),
            rightResolveImages = this.swapchains[1].images.toTypedArray()
        )

        this.input = XrInput(this.xrInstance, this.xrSession)
    }

    override fun prepareRender(extraRotationY: Float): Triple<Vector3f, Matrix4f, Matrix4f>? {
        val logger = getLogger("VR")
        this.sessionState.update()
        val result = if (this.sessionState.shouldTryRender()) {
            stackPush().use { stack ->
                val frameState = XrFrameState.calloc(stack)
                frameState.type(XR_TYPE_FRAME_STATE)

                assertXrSuccess(
                    xrWaitFrame(xrSession, null, frameState), "WaitFrame"
                )

                synchronized(this.vkQueue.handle) {
                    assertXrSuccess(
                        xrBeginFrame(xrSession, null), "BeginFrame"
                    )
                }

                val displayTime = frameState.predictedDisplayTime()
                this.nextDisplayTime = displayTime

                val result = if (frameState.shouldRender()) {

                    this.acquiredSwapchainImageIndices = this.swapchains.map { swapchain ->
                        val pSwapchainImageIndex = stack.callocInt(1)
                        synchronized(this.vkQueue.handle) {
                            assertXrSuccess(
                                xrAcquireSwapchainImage(swapchain.handle, null, pSwapchainImageIndex),
                                "AcquireSwapchainImage"
                            )
                        }
                        pSwapchainImageIndex[0]
                    }

                    getCameraMatrices(xrSession, renderSpace, lastViews, displayTime, extraRotationY)
                } else {
                    logger.info("Shouldn't render")
                    null
                }

                result
            }
        } else {
            logger.info("Don't try to render")
            sleep(100)
            null
        }

        this.didRender = result != null
        return result
    }

    override fun markFirstFrameQueueSubmit() {
        // This is not needed in OpenXR
    }

    override fun resolveAndSubmitFrames(waitSemaphore: Long?, takeScreenshot: Boolean) {
        if (sessionState.shouldTryRender()) {
            stackPush().use { stack ->
                val layers = if (didRender) {
                    for (swapchain in this.swapchains) {

                        val wiImage = XrSwapchainImageWaitInfo.calloc(stack)
                        wiImage.`type$Default`()
                        wiImage.timeout(10_000_000_000L) // 10 seconds should be long enough

                        assertXrSuccess(
                            xrWaitSwapchainImage(swapchain.handle, wiImage), "WaitSwapchainImage"
                        )
                    }
                    if (waitSemaphore != null) {
                        resolveHelper.resolve(
                            graphicsState.vkDevice,
                            this.acquiredSwapchainImageIndices[0],
                            this.acquiredSwapchainImageIndices[1],
                            graphicsState.queueManager, waitSemaphore, takeScreenshot
                        )
                    }

                    for (swapchain in swapchains) {
                        synchronized(this.vkQueue.handle) {
                            assertXrSuccess(
                                xrReleaseSwapchainImage(swapchain.handle, null),
                                "ReleaseSwapchainImage"
                            )
                        }
                    }

                    val projectionViews = XrCompositionLayerProjectionView.calloc(2, stack)
                    for (eyeIndex in 0 until 2) {
                        val projectionView = projectionViews[eyeIndex]
                        projectionView.`type$Default`()
                        projectionView.pose(lastViews[eyeIndex].pose())
                        projectionView.fov(lastViews[eyeIndex].fov())
                        projectionView.subImage { subImage ->
                            subImage.swapchain(swapchains[eyeIndex].handle)
                            subImage.imageRect { imageRect ->
                                // offset will stay 0
                                imageRect.extent { imageExtent ->
                                    imageExtent.set(width, height)
                                }
                            }
                            // image array index will stay 0
                        }
                    }

                    val layer = XrCompositionLayerProjection.calloc(stack)
                    layer.`type$Default`()
                    layer.layerFlags(0)
                    layer.space(this.renderSpace)
                    layer.views(projectionViews)

                    stack.pointers(layer)
                } else {
                    null
                }

                // DONT change this to malloc because that will fail when layers == null
                val eiFrame = XrFrameEndInfo.calloc(stack)
                eiFrame.`type$Default`()
                eiFrame.displayTime(nextDisplayTime)
                eiFrame.environmentBlendMode(XR_ENVIRONMENT_BLEND_MODE_OPAQUE)
                eiFrame.layers(layers)

                synchronized(this.vkQueue.handle) {
                    assertXrSuccess(
                        xrEndFrame(xrSession, eiFrame), "EndFrame"
                    )
                }
            }
        }
    }

    override fun getDragonControls(): DragonControls {
        return this.input.getDragonControls()
    }

    override fun destroy() {
        this.resolveHelper.destroy(graphicsState.vkDevice)
        lastViews.free()
        xrDestroySpace(renderSpace)
        for (swapchain in swapchains) {
            for (swapchainImage in swapchain.images) {
                vkDestroyImageView(graphicsState.vkDevice, swapchainImage.fullView!!, null)
            }
            xrDestroySwapchain(swapchain.handle)
        }
        if (xrDebugMessenger != null) {
            xrDestroyDebugUtilsMessengerEXT(xrDebugMessenger)
        }
        xrDestroyInstance(xrInstance)
    }

    override fun requestStop() {
        assertXrSuccess(
            xrRequestExitSession(xrSession), "RequestExitSession"
        )
    }

    override fun shouldStop() = sessionState.shouldExit()
}
