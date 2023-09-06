package dragons.init

/**
 * A nicer representation of the parameters that were passed to the main method
 */
class MainParameters(args: Array<String>) {

    /**
     * This parameter determines whether the game is allowed to be 'played' without HMD. When this is set to true, the
     * game will run with a fake VR manager that won't display anything and will return artificial values for all input
     * sources. Rendering will still happen 'normally', but won't be displayed anywhere unless a screenshot is made
     * (at least, once I implement screenshots). When this parameter is set to false, the game will refuse to start if
     * no HMD can be found.
     *
     * Setting this to true is useful for development because it allows a lot of testing without needing to connect an
     * HMD. This parameter is false by default, but developers should set it in their development environment.
     */
    val requiresHmd: Boolean
    val forbidDebug: Boolean
    val testParameters: TestingParameters

    init {
        for (arg in args) {
            if (!ALL_PARAMETERS.contains(arg)) {
                throw IllegalArgumentException("Unknown main argument '$arg'")
            }
        }

        requiresHmd = !args.contains("allowWithoutHmd")
        forbidDebug = args.contains("noDebug")

        testParameters = TestingParameters(args)
    }

    companion object {
        val ALL_PARAMETERS = arrayOf("allowWithoutHmd", "noDebug") + TestingParameters.ALL_PARAMETERS
    }
}

class TestingParameters(
    val staticMemory: Boolean
) {
    constructor(mainArgs: Array<String>) : this(
        staticMemory = mainArgs.contains("testStaticMemory")
    )

    companion object {
        val ALL_PARAMETERS = arrayOf("testStaticMemory")
    }
}
