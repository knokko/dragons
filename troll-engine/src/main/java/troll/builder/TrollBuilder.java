package troll.builder;

import org.lwjgl.vulkan.*;
import troll.builder.device.*;
import troll.builder.instance.ValidationFeatures;
import troll.builder.instance.VkInstanceCreator;
import troll.builder.queue.MinimalQueueFamilyMapper;
import troll.builder.queue.QueueFamilyMapper;
import troll.exceptions.*;
import troll.instance.TrollInstance;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.system.MemoryUtil.memUTF8;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME;
import static org.lwjgl.vulkan.EXTMemoryBudget.VK_EXT_MEMORY_BUDGET_EXTENSION_NAME;
import static org.lwjgl.vulkan.EXTValidationFeatures.*;
import static org.lwjgl.vulkan.KHRBindMemory2.VK_KHR_BIND_MEMORY_2_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRDedicatedAllocation.VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRGetMemoryRequirements2.VK_KHR_GET_MEMORY_REQUIREMENTS_2_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRGetPhysicalDeviceProperties2.VK_KHR_GET_PHYSICAL_DEVICE_PROPERTIES_2_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRPortabilityEnumeration.VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.*;
import static troll.exceptions.VulkanFailureException.assertVkSuccess;

public class TrollBuilder {

    public static final VkInstanceCreator DEFAULT_VK_INSTANCE_CREATOR = (stack, ciInstance) -> {
        var pInstance = stack.callocPointer(1);
        assertVkSuccess(vkCreateInstance(ciInstance, null, pInstance), "CreateInstance", "TrollBuilder");
        return new VkInstance(pInstance.get(0), ciInstance);
    };

    public static final VkDeviceCreator DEFAULT_VK_DEVICE_CREATOR = (stack, physicalDevice, ciDevice) -> {
        var pDevice = stack.callocPointer(1);
        assertVkSuccess(vkCreateDevice(physicalDevice, ciDevice, null, pDevice), "CreateDevice", "TrollBuilder");
        return new VkDevice(pDevice.get(0), physicalDevice, ciDevice);
    };

    final int apiVersion;
    final String applicationName;
    final int applicationVersion;

    long window = 0;
    int windowWidth = 0;
    int windowHeight = 0;
    boolean initGLFW = true;

    String engineName = "TrollEngine";
    int engineVersion = VK_MAKE_VERSION(0, 1, 0);

    final Set<String> desiredVulkanLayers = new HashSet<>();
    final Set<String> requiredVulkanLayers = new HashSet<>();

    final Set<String> desiredVulkanInstanceExtensions = new HashSet<>();
    final Set<String> requiredVulkanInstanceExtensions = new HashSet<>();

    final Set<String> desiredVulkanDeviceExtensions = new HashSet<>();
    final Set<String> requiredVulkanDeviceExtensions = new HashSet<>();

    ValidationFeatures validationFeatures = null;

    VkInstanceCreator vkInstanceCreator = DEFAULT_VK_INSTANCE_CREATOR;
    PhysicalDeviceSelector deviceSelector = new SimpleDeviceSelector(
            VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU,
            VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU
    );
    VkDeviceCreator vkDeviceCreator = DEFAULT_VK_DEVICE_CREATOR;

    FeaturePicker10 vkDeviceFeaturePicker10;
    FeaturePicker11 vkDeviceFeaturePicker11;
    FeaturePicker12 vkDeviceFeaturePicker12;
    FeaturePicker13 vkDeviceFeaturePicker13;

    QueueFamilyMapper queueFamilyMapper = new MinimalQueueFamilyMapper();

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

    public TrollBuilder physicalDeviceSelector(PhysicalDeviceSelector selector) {
        this.deviceSelector = selector;
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

    public TrollBuilder desiredVkDeviceExtensions(Collection<String> deviceExtensions) {
        desiredVulkanDeviceExtensions.addAll(deviceExtensions);
        return this;
    }

    public TrollBuilder requiredDeviceExtensions(Collection<String> deviceExtensions) {
        requiredVulkanDeviceExtensions.addAll(deviceExtensions);
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

    public TrollBuilder vkDeviceCreator(VkDeviceCreator creator) {
        this.vkDeviceCreator = creator;
        return this;
    }

    public TrollBuilder queueFamilyMapper(QueueFamilyMapper mapper) {
        this.queueFamilyMapper = mapper;
        return this;
    }

    public TrollInstance build() throws GLFWFailureException, VulkanFailureException, MissingVulkanLayerException,
            MissingVulkanExtensionException, NoVkPhysicalDeviceException {
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
            this.requiredVulkanDeviceExtensions.add(VK_KHR_SWAPCHAIN_EXTENSION_NAME);
        }

        // Nice for VMA
        if (VK_API_VERSION_MAJOR(apiVersion) == 1 && VK_API_VERSION_MINOR(apiVersion) == 0) {
            this.desiredVulkanInstanceExtensions.add(VK_KHR_GET_PHYSICAL_DEVICE_PROPERTIES_2_EXTENSION_NAME);
            this.desiredVulkanDeviceExtensions.add(VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME);
            this.desiredVulkanDeviceExtensions.add(VK_KHR_GET_MEMORY_REQUIREMENTS_2_EXTENSION_NAME);
            this.desiredVulkanDeviceExtensions.add(VK_KHR_BIND_MEMORY_2_EXTENSION_NAME);
            this.desiredVulkanDeviceExtensions.add(VK_EXT_MEMORY_BUDGET_EXTENSION_NAME);
        }

        if (validationFeatures != null) {
            this.requiredVulkanInstanceExtensions.add(VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
            this.requiredVulkanInstanceExtensions.add(VK_EXT_VALIDATION_FEATURES_EXTENSION_NAME);
            this.requiredVulkanLayers.add("VK_LAYER_KHRONOS_validation");
        }

        var instanceResult = TrollInstanceBuilder.createInstance(this);
        var deviceResult = TrollDeviceBuilder.createDevice(this, instanceResult.vkInstance());

        return new TrollInstance(
                window, deviceResult.windowSurface(),
                instanceResult.vkInstance(), deviceResult.vkPhysicalDevice(), deviceResult.vkDevice(),
                instanceResult.enabledExtensions(), deviceResult.enabledExtensions(),
                deviceResult.queueFamilies(), deviceResult.vmaAllocator()
        );
    }
}
