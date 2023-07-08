package troll.swapchain;

import troll.surface.SurfaceFormat;

public record SwapchainSettings(int imageUsage, SurfaceFormat surfaceFormat, int compositeAlpha) {
}
