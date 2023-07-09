package troll.swapchain;

public record AcquireResult(
        long vkSwapchain,
        long vkImage,
        int imageIndex,
        long acquireSemaphore,
        long presentSemaphore,
        int width,
        int height,
        long swapchainID
) {
}
