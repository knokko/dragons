package troll.instance;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import troll.exceptions.GLFWFailureException;
import troll.exceptions.MissingVulkanExtensionException;
import troll.exceptions.MissingVulkanLayerException;
import troll.exceptions.VulkanFailureException;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memUTF8;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME;
import static org.lwjgl.vulkan.EXTValidationFeatures.*;
import static org.lwjgl.vulkan.KHRGetPhysicalDeviceProperties2.VK_KHR_GET_PHYSICAL_DEVICE_PROPERTIES_2_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRPortabilityEnumeration.VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR;
import static org.lwjgl.vulkan.KHRPortabilityEnumeration.VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.*;
import static troll.exceptions.VulkanFailureException.assertVkSuccess;

public class TrollBuilder {

    public static final VkInstanceCreator DEFAULT_VK_INSTANCE_CREATOR = (stack, ciInstance) -> {
        var pInstance = stack.callocPointer(1);
        assertVkSuccess(vkCreateInstance(ciInstance, null, pInstance), "CreateInstance", "TrollBuilder");
        return new VkInstance(pInstance.get(0), ciInstance);
    };

    private final int apiVersion;
    private final String applicationName;
    private final int applicationVersion;

    private long window = 0;
    private int windowWidth = 0;
    private int windowHeight = 0;
    private boolean initGLFW = true;

    private String engineName = "TrollEngine";
    private int engineVersion = VK_MAKE_VERSION(0, 1, 0);

    private final Set<String> desiredVulkanLayers = new HashSet<>();
    private final Set<String> requiredVulkanLayers = new HashSet<>();

    private final Set<String> desiredVulkanInstanceExtensions = new HashSet<>();
    private final Set<String> requiredVulkanInstanceExtensions = new HashSet<>();

    private ValidationFeatures validationFeatures = null;

    private VkInstanceCreator vkInstanceCreator = DEFAULT_VK_INSTANCE_CREATOR;

    private boolean didBuild = false;

    public TrollBuilder(int apiVersion, String applicationName, int applicationVersion) {
        this.apiVersion = apiVersion;
        this.applicationName = applicationName;
        this.applicationVersion = applicationVersion;

        this.desiredVulkanInstanceExtensions.add(VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME);
    }

    /**
     * If all of {@code window}, {@code width}, and {@code height} are 0, no window will be created.
     * @param window The GLFW window to use, or 0 to create a new one of the given size
     * @param width The width of the window content, in pixels
     * @param height The height of the window content, in pixels
     * @return this
     */
    public TrollBuilder window(long window, int width, int height) {
        this.window = window;
        this.windowWidth = width;
        this.windowHeight = height;
        return this;
    }

    /**
     * <p>
     *      Call this when you want the TrollBuilder to create the window, but without initializing GLFW (e.g. when you
     *      want to initialize GLFW yourself).
     * </p>
     * <p>
     *     This method has no effect when the TrollBuilder does <b>not</b> create a window (in that case, it won't
     *     initialize GLFW anyway).
     * </p>
     * @return this
     */
    public TrollBuilder dontInitGLFW() {
        this.initGLFW = false;
        return this;
    }

    public TrollBuilder engine(String engineName, int engineVersion) {
        this.engineName = engineName;
        this.engineVersion = engineVersion;
        return this;
    }

    public TrollBuilder desiredVkLayers(Collection<String> desiredLayers) {
        desiredVulkanLayers.addAll(desiredLayers);
        return this;
    }

    public TrollBuilder requiredVkLayers(Collection<String> requiredLayers) {
        requiredVulkanLayers.addAll(requiredLayers);
        return this;
    }

    public TrollBuilder desiredVkInstanceExtensions(Collection<String> instanceExtensions) {
        desiredVulkanInstanceExtensions.addAll(instanceExtensions);
        return this;
    }

    public TrollBuilder requiredVkInstanceExtensions(Collection<String> instanceExtensions) {
        requiredVulkanInstanceExtensions.addAll(instanceExtensions);
        return this;
    }

    public TrollBuilder validation(ValidationFeatures validationFeatures) {
        this.validationFeatures = validationFeatures;
        return this;
    }

    public TrollBuilder vkInstanceCreator(VkInstanceCreator creator) {
        this.vkInstanceCreator = creator;
        return this;
    }

    public TrollInstance build() throws GLFWFailureException, VulkanFailureException, MissingVulkanLayerException,
            MissingVulkanExtensionException {
        if (didBuild) throw new IllegalStateException("This builder has been used already");
        didBuild = true;

        if (window == 0L && windowWidth != 0 && windowHeight != 0) {
            if (initGLFW && !glfwInit()) throw new GLFWFailureException("glfwInit() returned false");
            glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
            window = glfwCreateWindow(windowWidth, windowHeight, applicationName, 0L, 0L);
            if (window == 0) throw new GLFWFailureException("glfwCreateWindow() returned 0");
        }

        if (window != 0L) {
            if (!glfwVulkanSupported()) throw new GLFWFailureException("glfwVulkanSupported() returned false");
            var glfwExtensions = glfwGetRequiredInstanceExtensions();
            if (glfwExtensions == null) throw new GLFWFailureException("glfwGetRequiredInstanceExtensions() returned null");
            for (int extensionIndex = 0; extensionIndex < glfwExtensions.limit(); extensionIndex++) {
                this.requiredVulkanInstanceExtensions.add(memUTF8(glfwExtensions.get(extensionIndex)));
            }
        }

        // Nice for VMA
        if (VK_VERSION_MAJOR(apiVersion) == 1 && VK_VERSION_MINOR(apiVersion) == 0) {
            this.desiredVulkanInstanceExtensions.add(VK_KHR_GET_PHYSICAL_DEVICE_PROPERTIES_2_EXTENSION_NAME);
        }

        if (validationFeatures != null) {
            this.requiredVulkanInstanceExtensions.add(VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
            this.requiredVulkanInstanceExtensions.add(VK_EXT_VALIDATION_FEATURES_EXTENSION_NAME);
            this.requiredVulkanLayers.add("VK_LAYER_KHRONOS_validation");
        }

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

        for (String layer : requiredVulkanLayers) {
            if (!supportedLayers.contains(layer)) throw new MissingVulkanLayerException(layer);
        }
        var enabledLayers = new HashSet<>(requiredVulkanLayers);
        for (String layer : desiredVulkanLayers) {
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

        for (String extension : requiredVulkanInstanceExtensions) {
            if (!supportedExtensions.contains(extension)) throw new MissingVulkanExtensionException("instance", extension);
        }
        var enabledExtensions = new HashSet<>(requiredVulkanInstanceExtensions);
        for (String extension : desiredVulkanInstanceExtensions) {
            if (supportedExtensions.contains(extension)) enabledExtensions.add(extension);
        }

        try (var stack = stackPush()) {
            var appInfo = VkApplicationInfo.calloc(stack);
            appInfo.sType$Default();
            appInfo.pApplicationName(stack.UTF8(applicationName));
            appInfo.applicationVersion(applicationVersion);
            appInfo.pEngineName(stack.UTF8(engineName));
            appInfo.engineVersion(engineVersion);
            appInfo.apiVersion(apiVersion);

            VkValidationFeaturesEXT pValidationFeatures;
            if (validationFeatures != null) {
                pValidationFeatures = VkValidationFeaturesEXT.calloc(stack);
                pValidationFeatures.sType$Default();
                pValidationFeatures.pNext(0L);

                var validationFlags = stack.callocInt(5);
                if (validationFeatures.gpuAssisted) validationFlags.put(VK_VALIDATION_FEATURE_ENABLE_GPU_ASSISTED_EXT);
                if (validationFeatures.gpuAssistedReserve) validationFlags.put(VK_VALIDATION_FEATURE_ENABLE_GPU_ASSISTED_RESERVE_BINDING_SLOT_EXT);
                if (validationFeatures.bestPractices) validationFlags.put(VK_VALIDATION_FEATURE_ENABLE_BEST_PRACTICES_EXT);
                if (validationFeatures.debugPrint) validationFlags.put(VK_VALIDATION_FEATURE_ENABLE_DEBUG_PRINTF_EXT);
                if (validationFeatures.synchronization) validationFlags.put(VK_VALIDATION_FEATURE_ENABLE_SYNCHRONIZATION_VALIDATION_EXT);
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

            var vkInstance = vkInstanceCreator.vkCreateInstance(stack, ciInstance);
            return new TrollInstance(window, vkInstance);
        }
    }

    public record ValidationFeatures(
            boolean gpuAssisted, boolean gpuAssistedReserve, boolean debugPrint,
            boolean bestPractices, boolean synchronization
    ) {

        @Override
        public String toString() {
            return String.format(
                    "Validation(gpu=%b, reserve=%b, print=%b, best=%b, sync=%b)",
                    gpuAssisted, gpuAssistedReserve, debugPrint, bestPractices, synchronization
            );
        }
    }

    @FunctionalInterface
    public interface VkInstanceCreator {

        VkInstance vkCreateInstance(MemoryStack stack, VkInstanceCreateInfo ciInstance);
    }
}
