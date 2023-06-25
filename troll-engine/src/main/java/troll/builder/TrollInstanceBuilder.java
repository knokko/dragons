package troll.builder;

import org.lwjgl.vulkan.*;
import troll.exceptions.MissingVulkanExtensionException;
import troll.exceptions.MissingVulkanLayerException;

import java.util.HashSet;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.EXTValidationFeatures.*;
import static org.lwjgl.vulkan.KHRPortabilityEnumeration.VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR;
import static org.lwjgl.vulkan.KHRPortabilityEnumeration.VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.vkEnumerateInstanceExtensionProperties;
import static org.lwjgl.vulkan.VK10.vkEnumerateInstanceLayerProperties;
import static troll.exceptions.VulkanFailureException.assertVkSuccess;

class TrollInstanceBuilder {

    static VkInstance createInstance(TrollBuilder builder) {

        var supportedLayers = new HashSet<String>();
        try (var stack = stackPush()) {
            var pNumLayers = stack.callocInt(1);
            assertVkSuccess(vkEnumerateInstanceLayerProperties(
                    pNumLayers, null
            ), "EnumerateInstanceLayerProperties", "count");
            int numLayers = pNumLayers.get(0);

            var layerProperties = VkLayerProperties.calloc(numLayers, stack);
            assertVkSuccess(vkEnumerateInstanceLayerProperties(
                    pNumLayers, layerProperties
            ), "EnumerateInstanceLayerProperties", "layers");
            for (var layer : layerProperties) {
                supportedLayers.add(layer.layerNameString());
            }
        }

        for (String layer : builder.requiredVulkanLayers) {
            if (!supportedLayers.contains(layer)) throw new MissingVulkanLayerException(layer);
        }
        var enabledLayers = new HashSet<>(builder.requiredVulkanLayers);
        for (String layer : builder.desiredVulkanLayers) {
            if (supportedLayers.contains(layer)) enabledLayers.add(layer);
        }

        var supportedExtensions = new HashSet<String>();
        try (var stack = stackPush()) {
            var extensionsLayers = new HashSet<>(enabledLayers);
            extensionsLayers.add(null);

            for (String layerName : extensionsLayers) {
                var pNumLayers = stack.callocInt(1);
                var pLayerName = layerName != null ? stack.UTF8(layerName) : null;
                assertVkSuccess(vkEnumerateInstanceExtensionProperties(
                        pLayerName, pNumLayers, null
                ), "EnumerateInstanceExtensionProperties", "count");
                int numLayers = pNumLayers.get(0);

                var extensionProperties = VkExtensionProperties.calloc(numLayers, stack);
                assertVkSuccess(vkEnumerateInstanceExtensionProperties(
                        pLayerName, pNumLayers, extensionProperties
                ), "EnumerateInstanceExtensionProperties", "extensions");
                for (var extension : extensionProperties) {
                    supportedExtensions.add(extension.extensionNameString());
                }
            }
        }

        for (String extension : builder.requiredVulkanInstanceExtensions) {
            if (!supportedExtensions.contains(extension)) throw new MissingVulkanExtensionException("instance", extension);
        }
        var enabledExtensions = new HashSet<>(builder.requiredVulkanInstanceExtensions);
        for (String extension : builder.desiredVulkanInstanceExtensions) {
            if (supportedExtensions.contains(extension)) enabledExtensions.add(extension);
        }

        VkInstance vkInstance;
        try (var stack = stackPush()) {
            var appInfo = VkApplicationInfo.calloc(stack);
            appInfo.sType$Default();
            appInfo.pApplicationName(stack.UTF8(builder.applicationName));
            appInfo.applicationVersion(builder.applicationVersion);
            appInfo.pEngineName(stack.UTF8(builder.engineName));
            appInfo.engineVersion(builder.engineVersion);
            appInfo.apiVersion(builder.apiVersion);

            VkValidationFeaturesEXT pValidationFeatures;
            if (builder.validationFeatures != null) {
                pValidationFeatures = VkValidationFeaturesEXT.calloc(stack);
                pValidationFeatures.sType$Default();
                pValidationFeatures.pNext(0L);

                var validationFlags = stack.callocInt(5);
                if (builder.validationFeatures.gpuAssisted()) validationFlags.put(VK_VALIDATION_FEATURE_ENABLE_GPU_ASSISTED_EXT);
                if (builder.validationFeatures.gpuAssistedReserve()) validationFlags.put(VK_VALIDATION_FEATURE_ENABLE_GPU_ASSISTED_RESERVE_BINDING_SLOT_EXT);
                if (builder.validationFeatures.bestPractices()) validationFlags.put(VK_VALIDATION_FEATURE_ENABLE_BEST_PRACTICES_EXT);
                if (builder.validationFeatures.debugPrint()) validationFlags.put(VK_VALIDATION_FEATURE_ENABLE_DEBUG_PRINTF_EXT);
                if (builder.validationFeatures.synchronization()) validationFlags.put(VK_VALIDATION_FEATURE_ENABLE_SYNCHRONIZATION_VALIDATION_EXT);
                validationFlags.flip();

                if (validationFlags.limit() > 0) pValidationFeatures.pEnabledValidationFeatures(validationFlags);
                else pValidationFeatures = null;

            } else pValidationFeatures = null;

            var pEnabledLayers = stack.callocPointer(enabledLayers.size());
            for (String layer : enabledLayers) {
                pEnabledLayers.put(stack.UTF8(layer));
            }
            pEnabledLayers.flip();

            var pEnabledExtensions = stack.callocPointer(enabledExtensions.size());
            for (String extension : enabledExtensions) {
                pEnabledExtensions.put(stack.UTF8(extension));
            }
            pEnabledExtensions.flip();

            var ciInstance = VkInstanceCreateInfo.calloc(stack);
            ciInstance.sType$Default();
            ciInstance.pNext(pValidationFeatures != null ? pValidationFeatures.address() : 0L);
            if (enabledExtensions.contains(VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME)) {
                ciInstance.flags(VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR);
            } else ciInstance.flags(0);
            ciInstance.pApplicationInfo(appInfo);
            ciInstance.ppEnabledLayerNames(pEnabledLayers);
            ciInstance.ppEnabledExtensionNames(pEnabledExtensions);

            vkInstance = builder.vkInstanceCreator.vkCreateInstance(stack, ciInstance);
        }
        return vkInstance;
    }
}
