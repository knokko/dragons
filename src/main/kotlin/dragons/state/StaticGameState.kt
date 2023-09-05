package dragons.state

import dragons.init.GameInitProperties
import dragons.vr.VrManager
import knokko.plugin.PluginManager
import kotlinx.coroutines.CoroutineScope

/**
 * The game state that will be constructed after initializing the game and kept until the game session is closed (which
 * happens when the player quits or the game crashes).
 */
class StaticGameState(
        /**
     * The graphics resources that will are created during the initialization of the game and remain valid during the
     * entire game session.
     */
    val graphics: StaticGraphicsState,

        val initProperties: GameInitProperties,

        val pluginManager: PluginManager,

        val vrManager: VrManager,

        /**
     * The `ClassLoader` that loaded all the plug-ins and can be used to find plug-in resources.
     */
    val classLoader: ClassLoader,

        /**
     * The `CoroutineScope` that runs throughout the entire game session. This is preferred over the `GlobalScope`
     * because:
     *
     *  - It is encapsulated in the game state, which makes it easier to unit test functions that rely on it. (If this
     *  is used in a unit test, every unit test has its own `StaticGameState` and thus its own `coroutineScope`. If the
     *  `GlobalScope` were used, multiple unit tests would share it, which could lead to non-deterministic problems.)
     *  - The game system will wait for all tasks in this scope to finish before it will start destroying graphics
     *  resources. (That having said, it will cancel tasks if the game wants to shut down, but some tasks take way too
     *  long.)
     *  - It is easier to monitor resource leaks.
     *  - It doesn't require an `OptIn`.
     */
    val coroutineScope: CoroutineScope
)
