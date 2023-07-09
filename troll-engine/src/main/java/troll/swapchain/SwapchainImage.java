package troll.swapchain;

class SwapchainImage {

    final long vkImage;
    final int index;

    long acquireSemaphore, acquireFence, presentSemaphore;

    SwapchainImage(long vkImage, int index) {
        this.vkImage = vkImage;
        this.index = index;
    }
}
