package dragons.vr

import dragons.init.GameInitProperties
import dragons.state.StaticGraphicsState
import dragons.util.getIntConstantName
import dragons.vr.openxr.*
import dragons.vr.openxr.createOpenXrInstance
import dragons.vulkan.queue.DeviceQueue
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.PointerBuffer
import org.lwjgl.openxr.*
import org.lwjgl.openxr.KHRVulkanEnable2.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceProperties
import org.slf4j.Logger

internal fun tryInitOpenXR(initProps: GameInitProperties, logger: Logger): VrManager {

    val xrInstance = createOpenXrInstance(initProps, logger)
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
    return OpenXrManager(xrInstance, xrSystemId, width, height)
}

internal class OpenXrManager(
    private val xrInstance: XrInstance,
    private val xrSystemId: Long,
    private val width: Int,
    private val height: Int
) : VrManager {

    private lateinit var graphicsState: StaticGraphicsState
    private lateinit var vkQueue: DeviceQueue

    // TODO Ensure queue synchronization in xrBeginFrame, xrEndFrame, xrAcquireSwapchainImage, and xrReleaseSwapchainImage
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
        this.vkQueue = graphicsState.queueManager.generalQueueFamily.getRandomPriorityQueue()

        TODO("Create XRVK session...")
    }

    override fun prepareRender(): Triple<Vector3f, Matrix4f, Matrix4f>? {
        TODO("Not yet implemented")
    }

    override fun markFirstFrameQueueSubmit() {
        TODO("Not yet implemented")
    }

    override fun submitFrames() {
        TODO("Not yet implemented")
    }

    override fun destroy() {
        TODO("Not yet implemented")
    }
}
