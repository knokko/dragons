package troll.builder.swapchain;

@FunctionalInterface
public interface CompositeAlphaPicker {

    int chooseCompositeAlpha(int availableMask);
}
