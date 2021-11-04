package dragons.vr

import org.joml.Matrix4f
import org.lwjgl.openvr.HmdMatrix34
import org.lwjgl.openvr.HmdMatrix44
import org.lwjgl.openvr.TrackedDevicePose
import org.lwjgl.openvr.VR.*
import org.lwjgl.openvr.VRCompositor.*
import org.lwjgl.openvr.VRSystem.*
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memAddress
import org.lwjgl.system.MemoryUtil.memUTF8
import org.lwjgl.vulkan.VkPhysicalDevice
import org.slf4j.LoggerFactory.getLogger
import java.lang.Thread.sleep

class OpenVrManager: VrManager {
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

    private fun vrToJomlMatrix(vrMatrix: HmdMatrix34): Matrix4f {
        return Matrix4f(
            vrMatrix.m(0), vrMatrix.m(4), vrMatrix.m(8), 0f,
            vrMatrix.m(1), vrMatrix.m(5), vrMatrix.m(9), 0f,
            vrMatrix.m(2), vrMatrix.m(6), vrMatrix.m(10), 0f,
            vrMatrix.m(3), vrMatrix.m(7), vrMatrix.m(11), 1f
        )
    }

    private fun vrToJomlMatrix(vrMatrix: HmdMatrix44): Matrix4f {
        return Matrix4f(vrMatrix.m())
    }

    private fun createEyeMatrix(pose: TrackedDevicePose, leftOrRight: Int): Matrix4f {

        return stackPush().use { stack ->
            val matrixBuffer = HmdMatrix44.calloc(stack)
            val matrixBuffer2 = HmdMatrix34.calloc(stack)
            val projectionMatrix = vrToJomlMatrix(VRSystem_GetProjectionMatrix(leftOrRight, 0.01f, 100f, matrixBuffer)).transpose()

            val rawViewMatrix = pose.mDeviceToAbsoluteTracking()
            val viewMatrix = vrToJomlMatrix(rawViewMatrix).invert()

            val eyeToHeadTransform = vrToJomlMatrix(VRSystem_GetEyeToHeadTransform(leftOrRight, matrixBuffer2))

            projectionMatrix.mul(eyeToHeadTransform).mul(viewMatrix)

            // matMVP = m_mat4ProjectionLeft * m_mat4eyePosLeft * m_mat4HMDPose;
            // m_mat4ProjectionLeft is obtained from GetProjectionMatrix
            // m_mat4eyePosLeft is obtained from GetEyeToHeadTransform
            // m_mat4HMDPose is the inverse of DeviceToAbsoluteTracking of Hmd (viewMatrix)
        }
    }

    override fun prepareRender(): Pair<Matrix4f, Matrix4f>? {
        var result: Pair<Matrix4f, Matrix4f>? = null
        stackPush().use { stack ->
            val renderPoses = TrackedDevicePose.calloc(k_unMaxTrackedDeviceCount, stack)
            val gamePoses = null

            val getPoseResult = VRCompositor_WaitGetPoses(renderPoses, gamePoses)
            if (getPoseResult == 0) {
                val renderPose = renderPoses[0]
                if (renderPose.bPoseIsValid()) {
                    result = Pair(createEyeMatrix(renderPose, EVREye_Eye_Left), createEyeMatrix(renderPose, EVREye_Eye_Right))
                }
            } else {
                getLogger("VR").error("VRCompositor_WaitGetPoses returned $getPoseResult")
                sleep(11)
            }
        }

        return result
    }

    override fun submitFrames() {
        TODO("Not yet implemented")
    }

    override fun destroy() {
        getLogger("VR").info("Shutting down OpenVR")
        VR_ShutdownInternal()
    }
}
