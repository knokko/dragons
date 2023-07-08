package troll.builder.swapchain;

import troll.surface.SurfaceFormat;

import java.util.Set;

@FunctionalInterface
public interface SurfaceFormatPicker {

    SurfaceFormat chooseSurfaceFormat(Set<SurfaceFormat> availableSurfaceFormats);
}
