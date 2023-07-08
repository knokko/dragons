package troll.debug;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDebugUtilsObjectNameInfoEXT;
import troll.instance.TrollInstance;

import static org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME;
import static org.lwjgl.vulkan.EXTDebugUtils.vkSetDebugUtilsObjectNameEXT;
import static troll.exceptions.VulkanFailureException.assertVkSuccess;

public class TrollDebug {

    private final TrollInstance instance;
    private final boolean hasDebug;

    public TrollDebug(TrollInstance instance) {
        this.instance = instance;
        this.hasDebug = instance.instanceExtensions.contains(VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
    }

    public void name(MemoryStack stack, long object, int type, String name) {
        if (hasDebug) {
            var nameInfo = VkDebugUtilsObjectNameInfoEXT.calloc(stack);
            nameInfo.sType$Default();
            nameInfo.objectType(type);
            nameInfo.objectHandle(object);
            nameInfo.pObjectName(stack.UTF8(name));

            assertVkSuccess(vkSetDebugUtilsObjectNameEXT(
                    instance.vkDevice(), nameInfo
            ), "SetDebugUtilsObjectNameEXT", name);
        }
    }
}
