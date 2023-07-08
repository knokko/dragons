package troll.swapchain;

public record TrollSwapchain(
        long vkSwapchain, long[] vkImages, int width, int height, int presentMode
) {
}
