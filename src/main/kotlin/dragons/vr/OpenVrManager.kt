package dragons.vr

import dragons.init.GameInitProperties
import dragons.init.trouble.SimpleStartupException
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
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.openvr.*
import org.lwjgl.openvr.VR.*
import org.lwjgl.openvr.VRCompositor.*
import org.lwjgl.openvr.VRSystem.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memAddress
import org.lwjgl.system.MemoryUtil.memUTF8
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkPhysicalDevice
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import java.lang.Thread.sleep

fun tryInitOpenVR(initProps: GameInitProperties, logger: Logger): VrManager {
    if (!VR_IsRuntimeInstalled()) {
        logger.warn("No OpenVR runtime found")
        if (initProps.mainParameters.requiresHmd) {
            throw SimpleStartupException(
                "No OpenVR runtime found", listOf(
                    "It looks like the OpenVR (SteamVR) runtime is not installed on your computer.",
                    "Please restart this game after installing it.",
                    "You can close this window."
                )
            )
        } else {
            logger.info("Using a dummy VR manager")
            return DummyVrManager()
        }
    }

    logger.info("The OpenVR runtime path is ${VR_RuntimePath()}")

    if (!VR_IsHmdPresent()) {
        logger.warn("Can't find HMD")
        if (initProps.mainParameters.requiresHmd) {
            throw SimpleStartupException(
                "Can't find HMD", listOf(
                    "It looks like no HMD (VR helmet) is connected to your computer.",
                    "Please restart this game after plugging it in.",
                    "You can close this window."
                )
            )
        } else {
            logger.info("Using a dummy VR manager")
            return DummyVrManager()
        }
    }

    stackPush().use { stack ->
        val pVrError = stack.callocInt(1)
        val token = VR_InitInternal(pVrError, EVRApplicationType_VRApplication_Scene)

        val vrError = pVrError[0]
        if (vrError != 0) {
            logger.warn("VR_InitInternal returned $vrError")
            val vrErrorName = VR_GetVRInitErrorAsSymbol(vrError)
            if (initProps.mainParameters.requiresHmd) {
                throw SimpleStartupException(
                    "Failed to initialize OpenVR", listOf(
                        "VR_InitInternal returned error code $vrError ($vrErrorName).",
                        "You should restart the game after fixing this.",
                        "You can close this window."
                    )
                )
            } else {
                logger.info("Using a dummy VR manager")
                return DummyVrManager()
            }
        }

        OpenVR.create(token)
        logger.info("Initialized OpenVR with token $token")
    }

    return OpenVrManager()
}

class OpenVrManager: VrManager {

    private var requestedStop = false
    private lateinit var graphicsState: StaticGraphicsState
    private lateinit var resolveHelper: ResolveHelper

    private val leftResolveImage = CompletableDeferred<VulkanImage>()
    private val rightResolveImage = CompletableDeferred<VulkanImage>()

    init {
        VRCompositor_SetExplicitTimingMode(
            EVRCompositorTimingMode_VRCompositorTimingMode_Explicit_RuntimePerformsPostPresentHandoff
        )
    }

    override fun getVulkanInstanceExtensions(availableExtensions: Set<String>): Set<String> {
        val extensionStringSize = VRCompositor_GetVulkanInstanceExtensionsRequired(null)
        val logger = getLogger("VR")
        val result = mutableSetOf<String>()
        if (extensionStringSize > 0) {
            val extensionString = stackPush().use { stack ->
                val extensionBuffer = stack.calloc(extensionStringSize)
                VRCompositor_GetVulkanInstanceExtensionsRequired(extensionBuffer)
                memUTF8(memAddress(extensionBuffer))
            }

            val extensionArray = extensionString.split(" ")
            logger.info("The following ${extensionArray.size} Vulkan instance extensions are required for OpenVR:")
            for (extension in extensionArray) {
                logger.info(extension)
            }

            result.addAll(extensionArray)
        }

        return result
    }

    override fun getVulkanDeviceExtensions(device: VkPhysicalDevice, deviceName: String, availableExtensions: Set<String>): Set<String> {
        val extensionStringSize = VRCompositor_GetVulkanDeviceExtensionsRequired(device.address(), null)
        val logger = getLogger("VR")
        val result = mutableSetOf<String>()
        if (extensionStringSize > 0) {
            val extensionString = stackPush().use { stack ->
                val extensionBuffer = stack.calloc(extensionStringSize)
                VRCompositor_GetVulkanDeviceExtensionsRequired(device.address(), extensionBuffer)
                memUTF8(memAddress(extensionBuffer))
            }

            val extensionArray = extensionString.split(" ")
            logger.info("The following ${extensionArray.size} Vulkan device extensions are required for $deviceName in OpenVR:")
            for (extension in extensionArray) {
                logger.info(extension)
            }

            result.addAll(extensionArray)
        }

        return result
    }

    override fun claimStaticMemory(
        agent: VulkanStaticMemoryUser.Agent, queueManager: QueueManager, renderImageInfo: RenderImageInfo
    ) {
        val width = this.getWidth()
        val height = this.getHeight()

        for (resolveImage in arrayOf(this.leftResolveImage, this.rightResolveImage)) {
            agent.claims.images.add(ImageMemoryClaim(
                width = width, height = height,
                queueFamily = queueManager.generalQueueFamily,
                imageFormat = renderImageInfo.colorFormat,
                tiling = VK_IMAGE_TILING_OPTIMAL, samples = VK_SAMPLE_COUNT_1_BIT,
                // Note: transfer_src and sampled are required by OpenVR; transfer_dst is required for resolving itself
                imageUsage = VK_IMAGE_USAGE_TRANSFER_SRC_BIT or VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT,
                initialLayout = VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                aspectMask = VK_IMAGE_ASPECT_COLOR_BIT, accessMask = VK_ACCESS_TRANSFER_WRITE_BIT,
                dstPipelineStageMask = VK_PIPELINE_STAGE_TRANSFER_BIT, prefill = null, storeResult = resolveImage
            ))
        }
    }

    // I might want to cache this
    override fun getWidth(): Int {
        return stackPush().use { stack ->
            val pWidth = stack.callocInt(1)
            val pHeight = stack.callocInt(1)
            VRSystem_GetRecommendedRenderTargetSize(pWidth, pHeight)
            pWidth[0]
        }
    }

    override fun getHeight(): Int {
        return stackPush().use { stack ->
            val pWidth = stack.callocInt(1)
            val pHeight = stack.callocInt(1)
            VRSystem_GetRecommendedRenderTargetSize(pWidth, pHeight)
            pHeight[0]
        }
    }

    override fun setGraphicsState(graphicsState: StaticGraphicsState) {
        this.graphicsState = graphicsState

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

    private fun vrToJomlMatrix(vrMatrix: HmdMatrix34): Matrix4f {
        return Matrix4f(
            vrMatrix.m(0), vrMatrix.m(4), vrMatrix.m(8), 0f,
            vrMatrix.m(1), vrMatrix.m(5), vrMatrix.m(9), 0f,
            vrMatrix.m(2), vrMatrix.m(6), vrMatrix.m(10), 0f,
            vrMatrix.m(3), vrMatrix.m(7), vrMatrix.m(11), 1f
        )
    }

    private fun createEyeMatrix(
        stack: MemoryStack, pose: TrackedDevicePose, leftOrRight: Int,
        nearPlane: Distance, farPlane: Distance, extraRotationY: Angle
    ): Triple<Matrix4f, Matrix4f, Vector3f> {
        val matrixBuffer = HmdMatrix34.calloc(stack)

        val pfLeft = stack.callocFloat(1)
        val pfRight = stack.callocFloat(1)
        val pfTop = stack.callocFloat(1)
        val pfBottom = stack.callocFloat(1)
        VRSystem_GetProjectionRaw(leftOrRight, pfLeft, pfRight, pfTop, pfBottom)
        val projectionMatrix = composeProjection(
            pfLeft[0], pfRight[0], pfTop[0], pfBottom[0],
            nearPlane.meters.toFloat(), farPlane.meters.toFloat()
        ).scale(1f, -1f, 1f)

        val transformToDeviceMatrix = vrToJomlMatrix(pose.mDeviceToAbsoluteTracking()).rotateY(extraRotationY.radians).invert()
        val deviceToEyeMatrix = vrToJomlMatrix(VRSystem_GetEyeToHeadTransform(leftOrRight, matrixBuffer)).invert()

        val transformToEyeMatrix = deviceToEyeMatrix.mul(transformToDeviceMatrix)
        val eyePosition = transformToEyeMatrix.getTranslation(Vector3f()).mul(-1f)

        return Triple(projectionMatrix.mul(transformToEyeMatrix), transformToEyeMatrix, eyePosition)
    }

    private fun composeProjection(
        fLeft: Float,
        fRight: Float,
        fTop: Float,
        fBottom: Float,
        zNear: Float,
        zFar: Float
    ): Matrix4f {
        val idx = 1.0f / (fRight - fLeft)
        val idy = 1.0f / (fBottom - fTop)
        val idz = 1.0f / (zFar - zNear)
        val sx = fRight + fLeft
        val sy = fBottom + fTop
        val p = Matrix4f()

        // This is a slightly modified version of https://github.com/ValveSoftware/openvr/issues/1052
        // The original post was for OpenGL, but I negated p.m21 to account for Vulkans slightly different coordinate system
        // Also, this matrix is scaled by (1, -1, 1) after this method for the same reason
        p.m00(2 * idx); p.m10(0f);      p.m20(sx * idx);              p.m30(0f)
        p.m01(0f);      p.m11(2 * idy); p.m21(-sy * idy);             p.m31(0f)
        p.m02(0f);      p.m12(0f);      p.m22(-(zFar + zNear) * idz); p.m32(-2 * zFar * zNear * idz)
        p.m03(0f);      p.m13(0f);      p.m23(-1.0f);                 p.m33(0f)

        return p
    }

    override fun prepareRender(nearPlane: Distance, farPlane: Distance, extraRotationY: Angle): CameraMatrices? {
        var result: CameraMatrices? = null
        stackPush().use { stack ->
            val renderPoses = TrackedDevicePose.calloc(k_unMaxTrackedDeviceCount, stack)
            val gamePoses = null

            val getPoseResult = VRCompositor_WaitGetPoses(renderPoses, gamePoses)
            if (getPoseResult == 0) {
                val renderPose = renderPoses[0]
                if (renderPose.bPoseIsValid()) {
                    val (leftEyeMatrix, leftViewMatrix, leftEyePosition) = createEyeMatrix(
                        stack, renderPose, EVREye_Eye_Left, nearPlane, farPlane, extraRotationY
                    )
                    val (rightEyeMatrix, rightViewMatrix, rightEyePosition) = createEyeMatrix(
                        stack, renderPose, EVREye_Eye_Right, nearPlane, farPlane, extraRotationY
                    )
                    val averageEyePosition = leftEyePosition.add(rightEyePosition).mul(0.5f)
                    result = CameraMatrices(
                        averageRealEyePosition = averageEyePosition,
                        averageVirtualEyePosition = averageEyePosition,
                        averageViewMatrix = leftViewMatrix.add(rightViewMatrix).mulComponentWise(Matrix4f().set(FloatArray(16) { 0.5f })),
                        leftEyeMatrix = leftEyeMatrix,
                        rightEyeMatrix = rightEyeMatrix
                    )
                }
            } else {
                getLogger("VR").error("VRCompositor_WaitGetPoses returned $getPoseResult")
                sleep(11)
            }
        }

        return result
    }

    override fun markFirstFrameQueueSubmit() {
        val result = VRCompositor_SubmitExplicitTimingData()
        if (result != 0) println("VRCompositor_SubmitExplicitTimingData() returned $result")
    }

    override fun resolveAndSubmitFrames(waitSemaphore: Long?, takeScreenshot: Boolean) {
        stackPush().use { stack ->
            val queue = graphicsState.queueManager.generalQueueFamily.getRandomPriorityQueue()

            val vulkanTexture = VRVulkanTextureData.calloc(stack)
            // Fill image in later
            vulkanTexture.m_pDevice(graphicsState.troll.vkDevice().address())
            vulkanTexture.m_pPhysicalDevice(graphicsState.troll.vkPhysicalDevice().address())
            vulkanTexture.m_pInstance(graphicsState.troll.vkInstance().address())
            vulkanTexture.m_pQueue(queue.vkQueue.address())
            vulkanTexture.m_nQueueFamilyIndex(graphicsState.queueManager.generalQueueFamily.index)
            vulkanTexture.m_nWidth(getWidth())
            vulkanTexture.m_nHeight(getHeight())
            vulkanTexture.m_nFormat(VK_FORMAT_R8G8B8A8_SRGB)
            vulkanTexture.m_nSampleCount(VK_SAMPLE_COUNT_1_BIT)

            val vrTexture = Texture.calloc(stack)
            vrTexture.handle(vulkanTexture.address())
            vrTexture.eType(ETextureType_TextureType_Vulkan)
            vrTexture.eColorSpace(EColorSpace_ColorSpace_Auto)

            // Note: the resolve images should be finished by now, so this shouldn't block for long
            val (leftResolveImage, rightResolveImage) = runBlocking { Pair(leftResolveImage.await(), rightResolveImage.await()) }

            if (waitSemaphore != null) {
                this.resolveHelper.resolve(
                    graphicsState.troll.vkDevice(), 0, 0,
                    graphicsState.queueManager, waitSemaphore, takeScreenshot
                )
            }

            synchronized(queue) {
                vulkanTexture.m_nImage(leftResolveImage.handle)
                val leftResult = VRCompositor_Submit(EVREye_Eye_Left, vrTexture, null, EVRSubmitFlags_Submit_Default)

                vulkanTexture.m_nImage(rightResolveImage.handle)
                val rightResult = VRCompositor_Submit(EVREye_Eye_Right, vrTexture, null, EVRSubmitFlags_Submit_Default)

                if (leftResult != 0 || rightResult != 0) {
                    println("Submit results are ($leftResult, $rightResult)")
                }
            }
        }
    }

    // TODO Implement getDragonControls() and test the new joystick + physical controls

    override fun destroy() {
        getLogger("VR").info("Shutting down OpenVR")
        vkDeviceWaitIdle(graphicsState.troll.vkDevice())
        this.resolveHelper.destroy(graphicsState.troll.vkDevice())
        VR_ShutdownInternal()
    }

    override fun requestStop() {
        this.requestedStop = true
    }

    override fun shouldStop() = requestedStop
}
