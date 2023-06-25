package troll.queue;

public record QueueFamilies(
        QueueFamily graphics, QueueFamily compute, QueueFamily transfer, QueueFamily present
) { }
