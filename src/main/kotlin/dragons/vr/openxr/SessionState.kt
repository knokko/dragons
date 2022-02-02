package dragons.vr.openxr

import org.lwjgl.openxr.*
import org.lwjgl.openxr.XR10.*
import org.lwjgl.system.MemoryStack.stackPush
import org.slf4j.LoggerFactory

private val RENDER_SESSION_STATES = arrayOf(
    XR_SESSION_STATE_READY,
    XR_SESSION_STATE_SYNCHRONIZED,
    XR_SESSION_STATE_VISIBLE,
    XR_SESSION_STATE_FOCUSED
)

internal class SessionState(
    private val xrInstance: XrInstance,
    private val xrSession: XrSession
) {
    private var state = XR_SESSION_STATE_IDLE
    private var shouldExit = false

    fun update() {
        val logger = LoggerFactory.getLogger("VR")

        var shouldBeginSession = false
        var shouldEndSession = false

        stackPush().use { stack ->
            val nextEvent = XrEventDataBuffer.malloc(stack)
            while (true) {
                nextEvent.type(XR_TYPE_EVENT_DATA_BUFFER)
                nextEvent.next(0L)

                val pollResult = xrPollEvent(xrInstance, nextEvent)
                assertXrSuccess(pollResult, "xrPollEvent")
                if (pollResult == XR_EVENT_UNAVAILABLE) {
                    break
                }

                if (nextEvent.type() == XR_TYPE_EVENT_DATA_SESSION_STATE_CHANGED) {

                    val sessionStateEvent = XrEventDataSessionStateChanged.create(nextEvent.address())
                    logger.info("Changing OpenXR session state from ${this.state} to ${sessionStateEvent.state()}")
                    this.state = sessionStateEvent.state()

                    if (this.state == XR_SESSION_STATE_READY) {
                        shouldBeginSession = true
                    } else if (this.state == XR_SESSION_STATE_STOPPING) {
                        shouldEndSession = true
                    } else if (this.state == XR_SESSION_STATE_EXITING) {
                        shouldExit = true
                    }
                } else {
                    // For now, we are only interested in session state changed events
                    logger.info("Received OpenXR event ${nextEvent.type()}")
                }
            }

            if (shouldBeginSession) {

                val biSession = XrSessionBeginInfo.calloc(stack)
                biSession.type(XR_TYPE_SESSION_BEGIN_INFO)
                biSession.primaryViewConfigurationType(XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO)

                logger.info("Beginning OpenXR session...")
                assertXrSuccess(
                    xrBeginSession(xrSession, biSession), "BeginSession"
                )
            }

            if (shouldEndSession) {
                logger.info("Ending OpenXR session...")
                assertXrSuccess(xrEndSession(xrSession), "EndSession")
            }
        }
    }

    fun shouldTryRender(): Boolean {
        return RENDER_SESSION_STATES.contains(this.state)
    }

    fun shouldExit() = shouldExit
}
