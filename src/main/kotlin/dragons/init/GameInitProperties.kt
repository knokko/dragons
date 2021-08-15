package dragons.init

/**
 * Properties related to the initialization of the game.
 */
class GameInitProperties(
    /**
     * The arguments passed to the main method
     */
    val mainParameters: MainParameters,
    /**
     * True if this game is being run in development and false if this game is being run in release
     */
    val isInDevelopment: Boolean
)
