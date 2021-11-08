package dragons.plugins.standard.state

class StandardPluginState {

    lateinit var graphics: StandardGraphicsState

    fun hasGraphics() = this::graphics.isInitialized

    val preGraphics = StandardPreGraphicsState()
}
