package dragons.vulkan.init

import dragons.plugin.PluginInfo
import dragons.plugin.PluginInstance
import dragons.plugin.PluginManager
import dragons.plugin.interfaces.vulkan.VulkanDeviceActor
import dragons.plugin.util.createDummyPluginInstance
import dragons.vr.DummyVrManager
import dragons.vulkan.util.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memPutInt
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.*
import java.util.jar.JarInputStream

class TestDevice {

    @Test
    fun testGetEnabledFeatures10() {
        stackPush().use { stack ->
            val features = VkPhysicalDeviceFeatures.calloc(stack)
            assertTrue(getEnabledFeatures(features).isEmpty());

            features.drawIndirectFirstInstance(true)
            assertEquals(setOf("DRAWINDIRECTFIRSTINSTANCE"), getEnabledFeatures(features))

            features.depthClamp(true)
            assertEquals(setOf("DRAWINDIRECTFIRSTINSTANCE", "DEPTHCLAMP"), getEnabledFeatures(features))
        }
    }

    @Test
    fun testEnableFeatures10() {
        stackPush().use { stack ->
            val features = VkPhysicalDeviceFeatures.calloc(stack)
            enableFeatures(features, setOf("DEPTHCLAMP", "DRAWINDIRECTFIRSTINSTANCE"))
            assertEquals(setOf("DEPTHCLAMP", "DRAWINDIRECTFIRSTINSTANCE"), getEnabledFeatures(features))

            enableFeatures(features, setOf())
            assertEquals(setOf("DEPTHCLAMP", "DRAWINDIRECTFIRSTINSTANCE"), getEnabledFeatures(features))
        }
    }

    @Test
    fun testGetEnabledFeatures11() {
        stackPush().use { stack ->
            val features = VkPhysicalDeviceVulkan11Features.calloc(stack)
            features.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_FEATURES)
            features.pNext(123) // This field should be ignored

            assertTrue(getEnabledFeatures(features).isEmpty())

            features.multiview(true)
            features.shaderDrawParameters(true)
            assertEquals(setOf("MULTIVIEW", "SHADERDRAWPARAMETERS"), getEnabledFeatures(features))
        }
    }

    @Test
    fun testEnableFeatures11() {
        stackPush().use { stack ->
            val features = VkPhysicalDeviceVulkan11Features.calloc(stack)

            enableFeatures(features, setOf("MULTIVIEW", "SHADERDRAWPARAMETERS"))
            assertEquals(setOf("MULTIVIEW", "SHADERDRAWPARAMETERS"), getEnabledFeatures(features))

            enableFeatures(features, setOf())
            assertEquals(setOf("MULTIVIEW", "SHADERDRAWPARAMETERS"), getEnabledFeatures(features))
        }
    }

    @Test
    fun testGetEnabledFeatures12() {
        stackPush().use { stack ->
            val features = VkPhysicalDeviceVulkan12Features.calloc(stack)
            assertTrue(getEnabledFeatures(features).isEmpty())

            features.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES)
            features.pNext(567)
            assertTrue(getEnabledFeatures(features).isEmpty())

            features.drawIndirectCount(true)
            features.bufferDeviceAddress(true)
            assertEquals(setOf("DRAWINDIRECTCOUNT", "BUFFERDEVICEADDRESS"), getEnabledFeatures(features))
        }
    }

    @Test
    fun testEnableFeatures12() {
        stackPush().use { stack ->
            val features = VkPhysicalDeviceVulkan12Features.calloc(stack)
            features.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES)
            features.pNext(820)

            enableFeatures(features, setOf("DRAWINDIRECTCOUNT", "BUFFERDEVICEADDRESS"))
            assertEquals(setOf("DRAWINDIRECTCOUNT", "BUFFERDEVICEADDRESS"), getEnabledFeatures(features))
        }
    }

    @Test
    fun testPickQueuePriorities() {
        stackPush().use { stack ->
            assertEquals(
                Pair(stack.floats(1f), QueueFamilyInfo(6, 1, 0)),
                pickQueuePriorities(1, 6, stack)
            )
            assertEquals(
                Pair(stack.floats(1f, 0f), QueueFamilyInfo(15, 1, 1)),
                pickQueuePriorities(2, 15, stack)
            )
            assertEquals(
                Pair(stack.floats(1f, 1f, 0f), QueueFamilyInfo(0, 2, 1)),
                pickQueuePriorities(3, 0, stack)
            )
        }
    }

    @Test
    fun testPopulateDeviceCreateInfo() {

        val availableExtensions = setOf(
            "extension1", "hello?", "extension2", "extension3", "extension4", "extension5", "extension6", "extension7"
        )

        val availableFeaturesSet10 = setOf(
            "DRAWINDIRECTFIRSTINSTANCE", "DEPTHCLAMP", "FILLMODENONSOLID", "DEPTHBOUNDS", "FRAGMENTSTORESANDATOMICS"
        )
        val availableFeaturesSet11 = setOf(
            "MULTIVIEW", "SHADERDRAWPARAMETERS", "VARIABLEPOINTERS", "PROTECTEDMEMORY", "SAMPLERYCBCRCONVERSION"
        )
        val availableFeaturesSet12 = setOf(
            "DRAWINDIRECTCOUNT", "BUFFERDEVICEADDRESS", "HOSTQUERYRESET", "SHADERFLOAT16", "DESCRIPTORBINDINGPARTIALLYBOUND"
        )

        fun testShared(agent: VulkanDeviceActor.Agent) {
            // The available features should be correct
            assertEquals(availableFeaturesSet10, getEnabledFeatures(agent.availableFeatures10))
            assertEquals(availableFeaturesSet11, getEnabledFeatures(agent.availableFeatures11))
            assertEquals(availableFeaturesSet12, getEnabledFeatures(agent.availableFeatures12))

            // The requested and required features should be initially empty
            assertTrue(getEnabledFeatures(agent.requestedFeatures10).isEmpty())
            assertTrue(getEnabledFeatures(agent.requestedFeatures11).isEmpty())
            assertTrue(getEnabledFeatures(agent.requestedFeatures12).isEmpty())
            assertTrue(getEnabledFeatures(agent.requiredFeatures10).isEmpty())
            assertTrue(getEnabledFeatures(agent.requiredFeatures11).isEmpty())
            assertTrue(getEnabledFeatures(agent.requiredFeatures12).isEmpty())

            // And their sTypes should be correct (also the available features)
            assertEquals(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_FEATURES, agent.availableFeatures11.sType())
            assertEquals(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_FEATURES, agent.requestedFeatures11.sType())
            assertEquals(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_FEATURES, agent.requiredFeatures11.sType())
            assertEquals(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES, agent.availableFeatures12.sType())
            assertEquals(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES, agent.requestedFeatures12.sType())
            assertEquals(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES, agent.requiredFeatures12.sType())

            // And each pNext should be 0
            assertEquals(0, agent.availableFeatures11.pNext())
            assertEquals(0, agent.availableFeatures12.pNext())
            assertEquals(0, agent.requestedFeatures11.pNext())
            assertEquals(0, agent.requestedFeatures12.pNext())
            assertEquals(0, agent.requiredFeatures11.pNext())
            assertEquals(0, agent.requiredFeatures12.pNext())

            assertNull(agent.extendNextChain)
        }

        class DeviceActor1: VulkanDeviceActor {
            override fun manipulateVulkanDevice(pluginInstance: PluginInstance, agent: VulkanDeviceActor.Agent) {
                testShared(agent)

                agent.extendNextChain = VkBaseOutStructure.create()
                agent.extendNextChain!!.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)

                agent.requestedExtensions.add("nope")
                agent.requestedExtensions.add("extension3")
                agent.requestedExtensions.add("extension7")
                agent.requiredExtensions.add("extension4")

                agent.requestedFeatures10.drawIndirectFirstInstance(true)
                agent.requestedFeatures10.sparseResidency16Samples(true)
                agent.requiredFeatures10.depthClamp(true)

                agent.requestedFeatures11.multiview(true)
                agent.requestedFeatures11.multiviewTessellationShader(true)
                agent.requiredFeatures11.shaderDrawParameters(true)

                agent.requestedFeatures12.drawIndirectCount(true)
                agent.requestedFeatures12.bufferDeviceAddressCaptureReplay(true)
                agent.requiredFeatures12.bufferDeviceAddress(true)
            }
        }

        class DeviceActor2: VulkanDeviceActor {
            override fun manipulateVulkanDevice(pluginInstance: PluginInstance, agent: VulkanDeviceActor.Agent) {
                testShared(agent)

                agent.extendNextChain = VkBaseOutStructure.create()
                agent.extendNextChain!!.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)

                agent.requestedExtensions.add("won't find this")
                agent.requestedExtensions.add("extension5")
                agent.requestedExtensions.add("extension7")
                agent.requiredExtensions.add("extension6")

                agent.requestedFeatures10.fillModeNonSolid(true)
                agent.requestedFeatures10.inheritedQueries(true)
                agent.requiredFeatures10.depthBounds(true)

                agent.requestedFeatures11.variablePointers(true)
                agent.requestedFeatures11.multiviewGeometryShader(true)
                agent.requiredFeatures11.protectedMemory(true)

                agent.requestedFeatures12.hostQueryReset(true)
                agent.requestedFeatures12.descriptorBindingSampledImageUpdateAfterBind(true)
                agent.requiredFeatures12.shaderFloat16(true)
            }
        }

        stackPush().use { stack ->
            val vkInstance = createDummyVulkanInstance()
            val vkPhysicalDevice = getDummyVulkanPhysicalDevice(vkInstance)

            val pluginManager = PluginManager(listOf(
                Pair(DeviceActor1(), createDummyPluginInstance("plugin1")),
                Pair(DeviceActor2(), createDummyPluginInstance("plugin2"))
            ))
            val vrManager = DummyVrManager(
                deviceExtensions = setOf("extension1", "extension2")
            )

            val ciDevice = VkDeviceCreateInfo.calloc(stack)

            val availableFeatures10 = VkPhysicalDeviceFeatures.calloc(stack)
            enableFeatures(availableFeatures10, availableFeaturesSet10)

            val availableFeatures11 = VkPhysicalDeviceVulkan11Features.calloc(stack)
            availableFeatures11.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_FEATURES)
            enableFeatures(availableFeatures11, availableFeaturesSet11)

            val availableFeatures12 = VkPhysicalDeviceVulkan12Features.calloc(stack)
            availableFeatures12.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES)
            enableFeatures(availableFeatures12, availableFeaturesSet12)

            val queueFamilies = VkQueueFamilyProperties.calloc(3, stack)
            run {
                val unusedFamily = queueFamilies[0]
                memPutInt(
                    unusedFamily.address() + VkQueueFamilyProperties.QUEUEFLAGS,
                    VK_QUEUE_SPARSE_BINDING_BIT or VK_QUEUE_PROTECTED_BIT
                )
                memPutInt(unusedFamily.address() + VkQueueFamilyProperties.QUEUECOUNT, 6)
            }
            run {
                val generalFamily = queueFamilies[1]
                memPutInt(
                    generalFamily.address() + VkQueueFamilyProperties.QUEUEFLAGS,
                    VK_QUEUE_COMPUTE_BIT or VK_QUEUE_GRAPHICS_BIT
                )
                memPutInt(generalFamily.address() + VkQueueFamilyProperties.QUEUECOUNT, 2)
            }
            run {
                val transferFamily = queueFamilies[2]
                memPutInt(
                    transferFamily.address() + VkQueueFamilyProperties.QUEUEFLAGS,
                    VK_QUEUE_SPARSE_BINDING_BIT or VK_QUEUE_TRANSFER_BIT
                )
                memPutInt(transferFamily.address() + VkQueueFamilyProperties.QUEUECOUNT, 1)
            }

            val populateResult = populateDeviceCreateInfo(
                ciDevice, vkInstance, vkPhysicalDevice, stack, pluginManager, vrManager, availableExtensions,
                queueFamilies, availableFeatures10, availableFeatures11, availableFeatures12
            )

            assertEquals(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO, ciDevice.sType())
            assertNotNull(findInNextChain(ciDevice, VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO))
            assertNotNull(findInNextChain(ciDevice, VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO))
            assertEquals(0, ciDevice.flags())
            assertEquals(2, ciDevice.queueCreateInfoCount())
            for (ciQueue in ciDevice.pQueueCreateInfos()) {
                assertEquals(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO, ciQueue.sType())
                assertEquals(0, ciQueue.pNext())
                assertEquals(0, ciQueue.pNext())
            }
            run {
                val generalFamily = ciDevice.pQueueCreateInfos().find { ciQueue ->
                    ciQueue.queueFamilyIndex() == 1
                }!!
                assertEquals(stack.floats(1f, 0f), generalFamily.pQueuePriorities())
                assertEquals(QueueFamilyInfo(1, 1, 1), populateResult.generalQueueFamily)
            }
            run {
                val transferFamily = ciDevice.pQueueCreateInfos().find { ciQueue ->
                    ciQueue.queueFamilyIndex() == 2
                }!!
                assertEquals(stack.floats(1f), transferFamily.pQueuePriorities())
                assertEquals(QueueFamilyInfo(2, 1, 0), populateResult.transferOnlyQueueFamily!!)
            }
            assertNull(populateResult.computeOnlyQueueFamily)
            assertEquals(0, ciDevice.enabledLayerCount())

            val expectedExtensions = setOf("extension1", "extension2", "extension3", "extension4", "extension5", "extension6", "extension7")
            val expectedFeatures10 = setOf("DRAWINDIRECTFIRSTINSTANCE", "DEPTHCLAMP", "FILLMODENONSOLID", "DEPTHBOUNDS")
            val expectedFeatures11 = setOf("MULTIVIEW", "SHADERDRAWPARAMETERS", "VARIABLEPOINTERS", "PROTECTEDMEMORY")
            val expectedFeatures12 = setOf("DRAWINDIRECTCOUNT", "BUFFERDEVICEADDRESS", "HOSTQUERYRESET", "SHADERFLOAT16")
            assertEquals(expectedExtensions.size, ciDevice.enabledExtensionCount())
            assertEquals(expectedExtensions, decodeStringsToSet(ciDevice.ppEnabledExtensionNames()!!))
            assertEquals(expectedExtensions, populateResult.enabledExtensions)

            assertEquals(expectedFeatures10, getEnabledFeatures(ciDevice.pEnabledFeatures()!!))
            assertEquals(expectedFeatures10, getEnabledFeatures(populateResult.enabledFeatures10))
            run {
                val pFeatures11 = findInNextChain(ciDevice, VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_FEATURES)!!
                assertEquals(expectedFeatures11, getEnabledFeatures(VkPhysicalDeviceVulkan11Features.create(pFeatures11.address())))
                assertEquals(expectedFeatures11, getEnabledFeatures(populateResult.enabledFeatures11))
            }
            run {
                val pFeatures12 = findInNextChain(ciDevice, VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES)!!
                assertEquals(expectedFeatures12, getEnabledFeatures(VkPhysicalDeviceVulkan12Features.create(pFeatures12.address())))
                assertEquals(expectedFeatures12, getEnabledFeatures(populateResult.enabledFeatures12))
            }

            vkDestroyInstance(vkInstance, null)
        }
    }
}
