package troll.queue;

import org.lwjgl.vulkan.VkQueue;

import java.util.List;

public record QueueFamily(List<VkQueue> queues) {}
