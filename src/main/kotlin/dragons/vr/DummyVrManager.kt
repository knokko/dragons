package dragons.vr

class DummyVrManager: VrManager {
    override fun getVulkanInstanceExtensions(availableExtensions: Set<String>): Set<String> {
        return setOf()
    }

    override fun destroy() {}
}
