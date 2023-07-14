package troll.builder;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.VmaAllocatorCreateInfo;
import org.lwjgl.util.vma.VmaVulkanFunctions;
import org.lwjgl.vulkan.*;
import troll.exceptions.NoVkPhysicalDeviceException;
import troll.queue.QueueFamilies;
import troll.queue.QueueFamily;
import troll.queue.TrollQueue;

import java.nio.ByteBuffer;
import java.util.*;

import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.EXTMemoryBudget.VK_EXT_MEMORY_BUDGET_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRBindMemory2.VK_KHR_BIND_MEMORY_2_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRDedicatedAllocation.VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRGetMemoryRequirements2.VK_KHR_GET_MEMORY_REQUIREMENTS_2_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.vkGetPhysicalDeviceFeatures2;
import static troll.exceptions.VulkanFailureException.assertVkSuccess;

class TrollDeviceBuilder {

    static Result createDevice(TrollBuilder builder, VkInstance vkInstance) {
        VkPhysicalDevice vkPhysicalDevice;
        VkDevice vkDevice;
        Set<String> enabledExtensions;
        long windowSurface;
        QueueFamilies queueFamilies;
        long vmaAllocator;

        try (var stack = stackPush()) {

            if (builder.window != 0L) {
                var pSurface = stack.callocLong(1);
                assertVkSuccess(glfwCreateWindowSurface(
                        vkInstance, builder.window, null, pSurface
                ), "glfwCreateWindowSurface", null);
                windowSurface = pSurface.get(0);
            } else windowSurface = 0L;

            vkPhysicalDevice = builder.deviceSelector.choosePhysicalDevice(
                    stack, vkInstance, windowSurface, builder.requiredVulkanDeviceExtensions
            );
            if (vkPhysicalDevice == null) throw new NoVkPhysicalDeviceException();
        }

        try (var stack = stackPush()) {
            var pNumSupportedExtensions = stack.callocInt(1);
            assertVkSuccess(vkEnumerateDeviceExtensionProperties(
                    vkPhysicalDevice, (ByteBuffer) null, pNumSupportedExtensions, null
            ), "EnumerateDeviceExtensionProperties", "TrollDeviceBuilder count");
            int numSupportedExtensions = pNumSupportedExtensions.get(0);

            var pSupportedExtensions = VkExtensionProperties.calloc(numSupportedExtensions, stack);
            assertVkSuccess(vkEnumerateDeviceExtensionProperties(
                    vkPhysicalDevice, (ByteBuffer) null, pNumSupportedExtensions, pSupportedExtensions
            ), "EnumerateDeviceExtensionProperties", "TrollDeviceBuilder extensions");

            Set<String> supportedExtensions = new HashSet<>(numSupportedExtensions);
            for (int index = 0; index < numSupportedExtensions; index++) {
                supportedExtensions.add(pSupportedExtensions.get(index).extensionNameString());
            }
            for (var extension : builder.requiredVulkanDeviceExtensions) {
                if (!supportedExtensions.contains(extension)) {
                    // This is a programming error because the physical device selector must not choose physical
                    // devices that don't support all required extensions
                    throw new Error("Chosen device doesn't support required extension " + extension);
                }
            }

            enabledExtensions = new HashSet<>(builder.requiredVulkanDeviceExtensions);
            for (var extension : builder.desiredVulkanDeviceExtensions) {
                if (supportedExtensions.contains(extension)) enabledExtensions.add(extension);
            }

            PointerBuffer ppEnabledExtensions = stack.callocPointer(enabledExtensions.size());
            for (var extension : enabledExtensions) {
                ppEnabledExtensions.put(stack.UTF8(extension));
            }
            ppEnabledExtensions.flip();

            if (VK_API_VERSION_MAJOR(builder.apiVersion) != 1) {
                throw new UnsupportedOperationException("Unknown api major version: " + VK_API_VERSION_MAJOR(builder.apiVersion));
            }

            int minorVersion = VK_API_VERSION_MINOR(builder.apiVersion);
            VkPhysicalDeviceFeatures enabledFeatures10;
            VkPhysicalDeviceFeatures2 enabledFeatures2 = null;
            if (minorVersion == 0) {
                var supportedFeatures10 = VkPhysicalDeviceFeatures.calloc(stack);
                vkGetPhysicalDeviceFeatures(vkPhysicalDevice, supportedFeatures10);
                enabledFeatures10 = VkPhysicalDeviceFeatures.calloc(stack);
                if (builder.vkDeviceFeaturePicker10 != null) {
                    builder.vkDeviceFeaturePicker10.enableFeatures(stack, supportedFeatures10, enabledFeatures10);
                }
            } else {
                var supportedFeatures11 = VkPhysicalDeviceVulkan11Features.calloc(stack);
                supportedFeatures11.sType$Default();
                var supportedFeatures12 = VkPhysicalDeviceVulkan12Features.calloc(stack);
                supportedFeatures12.sType$Default();
                var supportedFeatures13 = VkPhysicalDeviceVulkan13Features.calloc(stack);
                supportedFeatures13.sType$Default();
                var supportedFeatures2 = VkPhysicalDeviceFeatures2.calloc(stack);
                supportedFeatures2.sType$Default();
                supportedFeatures2.pNext(supportedFeatures11);
                if (minorVersion >= 2) {
                    supportedFeatures2.pNext(supportedFeatures12);
                }
                if (minorVersion >= 3) {
                    supportedFeatures2.pNext(supportedFeatures13);
                }

                vkGetPhysicalDeviceFeatures2(vkPhysicalDevice, supportedFeatures2);

                enabledFeatures2 = VkPhysicalDeviceFeatures2.calloc(stack);
                enabledFeatures2.sType$Default();
                enabledFeatures10 = enabledFeatures2.features();

                if (builder.vkDeviceFeaturePicker10 != null) {
                    builder.vkDeviceFeaturePicker10.enableFeatures(stack, supportedFeatures2.features(), enabledFeatures10);
                }
                if (builder.vkDeviceFeaturePicker11 != null) {
                    var enabledFeatures11 = VkPhysicalDeviceVulkan11Features.calloc(stack);
                    enabledFeatures11.sType$Default();
                    builder.vkDeviceFeaturePicker11.enableFeatures(stack, supportedFeatures11, enabledFeatures11);
                    enabledFeatures2.pNext(enabledFeatures11);
                }
                if (builder.vkDeviceFeaturePicker12 != null) {
                    var enabledFeatures12 = VkPhysicalDeviceVulkan12Features.calloc(stack);
                    enabledFeatures12.sType$Default();
                    builder.vkDeviceFeaturePicker12.enableFeatures(stack, supportedFeatures12, enabledFeatures12);
                    enabledFeatures2.pNext(enabledFeatures12);
                }
                if (builder.vkDeviceFeaturePicker13 != null) {
                    var enabledFeatures13 = VkPhysicalDeviceVulkan13Features.calloc(stack);
                    enabledFeatures13.sType$Default();
                    builder.vkDeviceFeaturePicker13.enableFeatures(stack, supportedFeatures13, enabledFeatures13);
                    enabledFeatures2.pNext(enabledFeatures13);
                }
            }

            var pNumQueueFamilies = stack.callocInt(1);
            vkGetPhysicalDeviceQueueFamilyProperties(vkPhysicalDevice, pNumQueueFamilies, null);
            int numQueueFamilies = pNumQueueFamilies.get(0);
            var pQueueFamilies = VkQueueFamilyProperties.calloc(numQueueFamilies, stack);
            vkGetPhysicalDeviceQueueFamilyProperties(vkPhysicalDevice, pNumQueueFamilies, pQueueFamilies);

            boolean[] queueFamilyPresentSupport = new boolean[numQueueFamilies];
            for (int familyIndex = 0; familyIndex < numQueueFamilies; familyIndex++) {
                if (windowSurface != 0L) {
                    var pPresentSupport = stack.callocInt(1);
                    assertVkSuccess(vkGetPhysicalDeviceSurfaceSupportKHR(
                            vkPhysicalDevice, familyIndex, windowSurface, pPresentSupport
                    ), "GetPhysicalDeviceSurfaceSupportKHR", "TrollDeviceBuilder");
                    queueFamilyPresentSupport[familyIndex] = pPresentSupport.get(0) == VK_TRUE;
                } else queueFamilyPresentSupport[familyIndex] = true;
            }

            var queueFamilyMapping = builder.queueFamilyMapper.mapQueueFamilies(pQueueFamilies, queueFamilyPresentSupport);
            queueFamilyMapping.validate();

            var uniqueQueueFamilies = new HashMap<Integer, float[]>();
            // Do presentFamily first so that it will be overwritten by the others if the queue family is shared
            uniqueQueueFamilies.put(queueFamilyMapping.presentFamilyIndex(), new float[] { 1f });
            uniqueQueueFamilies.put(queueFamilyMapping.graphicsFamilyIndex(), queueFamilyMapping.graphicsPriorities());
            uniqueQueueFamilies.put(queueFamilyMapping.computeFamilyIndex(), queueFamilyMapping.computePriorities());
            uniqueQueueFamilies.put(queueFamilyMapping.transferFamilyIndex(), queueFamilyMapping.transferPriorities());

            var pQueueCreateInfos = VkDeviceQueueCreateInfo.calloc(uniqueQueueFamilies.size(), stack);
            int ciQueueIndex = 0;
            for (var entry : uniqueQueueFamilies.entrySet()) {
                var ciQueue = pQueueCreateInfos.get(ciQueueIndex);
                ciQueue.sType$Default();
                ciQueue.flags(0);
                ciQueue.queueFamilyIndex(entry.getKey());
                ciQueue.pQueuePriorities(stack.floats(entry.getValue()));

                ciQueueIndex += 1;
            }

            var ciDevice = VkDeviceCreateInfo.calloc(stack);
            ciDevice.sType$Default();
            if (enabledFeatures2 != null) ciDevice.pNext(enabledFeatures2);
            ciDevice.flags(0);
            ciDevice.pQueueCreateInfos(pQueueCreateInfos);
            ciDevice.ppEnabledLayerNames(null); // Device layers are deprecated
            ciDevice.ppEnabledExtensionNames(ppEnabledExtensions);
            if (enabledFeatures2 == null) ciDevice.pEnabledFeatures(enabledFeatures10);

            vkDevice = builder.vkDeviceCreator.vkCreateDevice(stack, vkPhysicalDevice, enabledExtensions, ciDevice);

            var queueFamilyMap = new HashMap<Integer, QueueFamily>();
            for (var entry : uniqueQueueFamilies.entrySet()) {
                queueFamilyMap.put(entry.getKey(), getQueueFamily(stack, vkDevice, entry.getKey(), entry.getValue().length));
            }

            queueFamilies = new QueueFamilies(
                    queueFamilyMap.get(queueFamilyMapping.graphicsFamilyIndex()),
                    queueFamilyMap.get(queueFamilyMapping.computeFamilyIndex()),
                    queueFamilyMap.get(queueFamilyMapping.transferFamilyIndex()),
                    queueFamilyMap.get(queueFamilyMapping.presentFamilyIndex())
            );

            var vmaVulkanFunctions = VmaVulkanFunctions.calloc(stack);
            vmaVulkanFunctions.set(vkInstance, vkDevice);

            int vmaFlags = 0;
            if (enabledExtensions.contains(VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME)
                    && enabledExtensions.contains(VK_KHR_GET_MEMORY_REQUIREMENTS_2_EXTENSION_NAME)
            ) {
                vmaFlags |= VMA_ALLOCATOR_CREATE_KHR_DEDICATED_ALLOCATION_BIT;
            }
            if (enabledExtensions.contains(VK_KHR_BIND_MEMORY_2_EXTENSION_NAME)) {
                vmaFlags |= VMA_ALLOCATOR_CREATE_KHR_BIND_MEMORY2_BIT;
            }
            if (enabledExtensions.contains(VK_EXT_MEMORY_BUDGET_EXTENSION_NAME)) {
                vmaFlags |= VMA_ALLOCATOR_CREATE_EXT_MEMORY_BUDGET_BIT;
            }

            var ciAllocator = VmaAllocatorCreateInfo.calloc(stack);
            ciAllocator.flags(vmaFlags);
            ciAllocator.physicalDevice(vkPhysicalDevice);
            ciAllocator.device(vkDevice);
            ciAllocator.instance(vkInstance);
            ciAllocator.pVulkanFunctions(vmaVulkanFunctions);
            ciAllocator.vulkanApiVersion(builder.apiVersion);

            var pAllocator = stack.callocPointer(1);

            assertVkSuccess(vmaCreateAllocator(
                    ciAllocator, pAllocator
            ), "VmaCreateAllocator", "TrollDeviceBuilder");
            vmaAllocator = pAllocator.get(0);
        }

        return new Result(vkPhysicalDevice, vkDevice, enabledExtensions, windowSurface, queueFamilies, vmaAllocator);
    }

    private static QueueFamily getQueueFamily(MemoryStack stack, VkDevice vkDevice, int familyIndex, int queueCount) {
        List<TrollQueue> queues = new ArrayList<>(queueCount);
        for (int queueIndex = 0; queueIndex < queueCount; queueIndex++) {
            var pQueue = stack.callocPointer(1);
            vkGetDeviceQueue(vkDevice, familyIndex, queueIndex, pQueue);
            queues.add(new TrollQueue(new VkQueue(pQueue.get(0), vkDevice)));
        }
        return new QueueFamily(familyIndex, Collections.unmodifiableList(queues));
    }

    record Result(
            VkPhysicalDevice vkPhysicalDevice, VkDevice vkDevice, Set<String> enabledExtensions,
            long windowSurface, QueueFamilies queueFamilies, long vmaAllocator
    ) {}
}
