package troll.buffer;

public record MappedVmaBuffer(VmaBuffer buffer, long hostAddress) {
}
