package dragons.vr

import dragons.state.StaticGraphicsState
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
import org.slf4j.LoggerFactory.getLogger
import java.lang.Thread.sleep

class OpenVrManager: VrManager {

    private lateinit var graphicsState: StaticGraphicsState

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
    }

    private fun vrToJomlMatrix(vrMatrix: HmdMatrix34): Matrix4f {
        return Matrix4f(
            vrMatrix.m(0), vrMatrix.m(4), vrMatrix.m(8), 0f,
            vrMatrix.m(1), vrMatrix.m(5), vrMatrix.m(9), 0f,
            vrMatrix.m(2), vrMatrix.m(6), vrMatrix.m(10), 0f,
            vrMatrix.m(3), vrMatrix.m(7), vrMatrix.m(11), 1f
        )
    }

    private fun vrToJomlMatrix(vrMatrix: HmdMatrix44): Matrix4f {
        return Matrix4f(vrMatrix.m()).transpose()
    }

    private fun createEyeMatrix(stack: MemoryStack, pose: TrackedDevicePose, leftOrRight: Int): Pair<Matrix4f, Vector3f> {
        val matrixBuffer = HmdMatrix44.calloc(stack)
        val matrixBuffer2 = HmdMatrix34.calloc(stack)

        // I'm not sure why I need to scale this by (1, -1, 1), but the results are much better
//        val projectionMatrix = vrToJomlMatrix(
//            VRSystem_GetProjectionMatrix(leftOrRight, 0.01f, 100f, matrixBuffer)
//        ).scale(1f, -1f, 1f)
//        val projectionMatrix = Matrix4f().perspective(
//            Angle.degrees(70f).radians, getWidth().toFloat() / getHeight().toFloat(), 0.01f, 100f, true
//        ).scale(1f, -1f, 1f)

        val pfLeft = stack.callocFloat(1)
        val pfRight = stack.callocFloat(1)
        val pfTop = stack.callocFloat(1)
        val pfBottom = stack.callocFloat(1)
        VRSystem_GetProjectionRaw(leftOrRight, pfLeft, pfRight, pfTop, pfBottom)
        //val projectionMatrix = Matrix4f().setFrustum(pfLeft[0], pfRight[0], pfBottom[0], pfTop[0], 0.01f, 100f, true).scale(1f, -1f, 1f)
        val projectionMatrix = composeProjection(pfLeft[0], pfRight[0], pfTop[0], pfBottom[0], 0.01f, 100f).scale(1f, -1f, 1f)

        // These matrices are inverted in https://github.com/ValveSoftware/openvr/blob/master/samples/hellovr_vulkan/hellovr_vulkan_main.cpp
        // I'm not sure why, but I will just follow it.
        val transformToDeviceMatrix = vrToJomlMatrix(pose.mDeviceToAbsoluteTracking()).invert()
        //val transformToDeviceMatrix = Matrix4f().rotateX((System.currentTimeMillis() % 5000).toFloat() / 300f)

        val deviceToEyeMatrix = vrToJomlMatrix(VRSystem_GetEyeToHeadTransform(leftOrRight, matrixBuffer2)).invert()

        val transformToEyeMatrix = deviceToEyeMatrix.mul(transformToDeviceMatrix)
        val eyePosition = transformToEyeMatrix.getTranslation(Vector3f()).mul(-1f)

        return Pair(projectionMatrix.mul(transformToEyeMatrix), eyePosition)
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
        p.m00(2 * idx)
        p.m10(0f)
        p.m20(sx * idx)
        p.m30(0f)
        p.m01(0f)
        p.m11(2 * idy)
        p.m21(sy * idy)
        p.m31(0f)
        p.m02(0f)
        p.m12(0f)
        p.m22(-(zFar + zNear) * idz)
        p.m32(-2 * zFar * zNear * idz)
        p.m03(0f)
        p.m13(0f)
        p.m23(-1.0f)
        p.m33(0f)
        return p
    }

    override fun prepareRender(): Triple<Vector3f, Matrix4f, Matrix4f>? {
        var result: Triple<Vector3f, Matrix4f, Matrix4f>? = null
        stackPush().use { stack ->
            val renderPoses = TrackedDevicePose.calloc(k_unMaxTrackedDeviceCount, stack)
            val gamePoses = null

            val getPoseResult = VRCompositor_WaitGetPoses(renderPoses, gamePoses)
            if (getPoseResult == 0) {
                val renderPose = renderPoses[0]
                if (renderPose.bPoseIsValid()) {
                    val (leftEyeMatrix, leftEyePosition) = createEyeMatrix(stack, renderPose, EVREye_Eye_Left)
                    val (rightEyeMatrix, rightEyePosition) = createEyeMatrix(stack, renderPose, EVREye_Eye_Right)
                    val averageEyePosition = leftEyePosition.add(rightEyePosition).mul(0.5f)
                    result = Triple(
                        averageEyePosition, leftEyeMatrix, rightEyeMatrix
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

    override fun submitFrames() {
        stackPush().use { stack ->
            val queue = graphicsState.queueManager.generalQueueFamily.getRandomPriorityQueue()

            val vulkanTexture = VRVulkanTextureData.calloc(stack)
            // Fill image in later
            vulkanTexture.m_pDevice(graphicsState.vkDevice.address())
            vulkanTexture.m_pPhysicalDevice(graphicsState.vkPhysicalDevice.address())
            vulkanTexture.m_pInstance(graphicsState.vkInstance.address())
            vulkanTexture.m_pQueue(queue.handle.address())
            vulkanTexture.m_nQueueFamilyIndex(graphicsState.queueManager.generalQueueFamily.index)
            vulkanTexture.m_nWidth(getWidth())
            vulkanTexture.m_nHeight(getHeight())
            vulkanTexture.m_nFormat(VK_FORMAT_R8G8B8A8_SRGB)
            vulkanTexture.m_nSampleCount(VK_SAMPLE_COUNT_1_BIT)

            val vrTexture = Texture.calloc(stack)
            vrTexture.handle(vulkanTexture.address())
            vrTexture.eType(ETextureType_TextureType_Vulkan)
            vrTexture.eColorSpace(EColorSpace_ColorSpace_Auto)

            synchronized(queue) {
                vulkanTexture.m_nImage(graphicsState.coreMemory.leftResolveImage.handle)
                val leftResult = VRCompositor_Submit(EVREye_Eye_Left, vrTexture, null, EVRSubmitFlags_Submit_Default)

                vulkanTexture.m_nImage(graphicsState.coreMemory.rightResolveImage.handle)
                val rightResult = VRCompositor_Submit(EVREye_Eye_Right, vrTexture, null, EVRSubmitFlags_Submit_Default)

                if (leftResult != 0 || rightResult != 0) {
                    println("Submit results are ($leftResult, $rightResult)")
                }
            }
        }
    }

    override fun destroy() {
        getLogger("VR").info("Shutting down OpenVR")
        vkDeviceWaitIdle(graphicsState.vkDevice)
        VR_ShutdownInternal()
    }
}
