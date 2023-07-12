package troll.pipelines;

import org.lwjgl.vulkan.VkSpecializationInfo;

public record ShaderInfo(int stage, long module, VkSpecializationInfo specialization) {
}
