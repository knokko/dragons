package troll.sync;

public record WaitSemaphore(long vkSemaphore, int stageMask) {
}
