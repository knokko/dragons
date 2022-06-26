package dragons.vr.openxr

import dragons.util.Angle
import dragons.vr.leftViewMatrix
import org.joml.Math.*
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.lwjgl.openxr.*
import org.lwjgl.openxr.XR10.*
import org.lwjgl.system.MemoryStack.stackPush
import org.slf4j.LoggerFactory.getLogger

internal fun createRenderSpace(xrSession: XrSession): XrSpace {
    return stackPush().use { stack ->
        val ciRenderSpace = XrReferenceSpaceCreateInfo.calloc(stack)
        ciRenderSpace.`type$Default`()
        ciRenderSpace.referenceSpaceType(XR_REFERENCE_SPACE_TYPE_STAGE)
        ciRenderSpace.poseInReferenceSpace { pose ->
            // position will stay (0, 0, 0)
            pose.orientation { orientation ->
                // x, y, and z will stay 0
                orientation.w(1f)
            }
        }

        val pRenderSpace = stack.callocPointer(1)
        assertXrSuccess(
            xrCreateReferenceSpace(xrSession, ciRenderSpace, pRenderSpace),
            "CreateReferenceSpace", "render"
        )
        XrSpace(pRenderSpace[0], xrSession)
    }
}

// TODO Don't use these global variables
private var lastRealPosition = Vector3f()
private var lastVirtualPosition = Vector3f()

internal fun getCameraMatrices(
    xrSession: XrSession, renderSpace: XrSpace, pViews: XrView.Buffer, displayTime: Long, extraRotationY: Angle
): Triple<Vector3f, Matrix4f, Matrix4f>? {
    stackPush().use { stack ->

        val liView = XrViewLocateInfo.calloc(stack)
        liView.`type$Default`()
        liView.viewConfigurationType(XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO)
        liView.displayTime(displayTime)
        liView.space(renderSpace)

        val viewState = XrViewState.calloc(stack)
        viewState.`type$Default`()

        val pNumViews = stack.callocInt(1)

        for (index in 0 until 2) {
            pViews[index].`type$Default`()
        }

        assertXrSuccess(
            xrLocateViews(xrSession, liView, viewState, pNumViews, pViews),
            "LocateViews", "eyes"
        )

        if (pNumViews[0] != 2) {
            throw RuntimeException("xrLocateViews returned ${pNumViews[0]} views, but 2 were expected")
        }

        val hasPosition = (viewState.viewStateFlags() and XR_VIEW_STATE_POSITION_VALID_BIT.toLong()) != 0L
        val hasOrientation = (viewState.viewStateFlags() and XR_VIEW_STATE_ORIENTATION_VALID_BIT.toLong()) != 0L

        if (!hasPosition) {
            getLogger("VR").info("Skipping rendering because position is not tracked")
            return null
        }

        if (!hasOrientation) {
            getLogger("VR").info("Skipping rendering because orientation is not tracked")
            return null
        }

        val leftEyePosition = pViews[0].pose().`position$`()
        val rightEyePosition = pViews[1].pose().`position$`()
        val averageRealEyePosition = Vector3f(
            leftEyePosition.x() + rightEyePosition.x(),
            leftEyePosition.y() + rightEyePosition.y(),
            leftEyePosition.z() + rightEyePosition.z()
        ).mul(0.5f)

        val realMovement = averageRealEyePosition.sub(lastRealPosition, Vector3f())
        val virtualMovement = Vector3f(
            cos(-extraRotationY.radians) * realMovement.x + sin(-extraRotationY.radians) * realMovement.z,
            realMovement.y,
            -sin(-extraRotationY.radians) * realMovement.x + cos(-extraRotationY.radians) * realMovement.z
        )

        val averageVirtualEyePosition = lastVirtualPosition.add(virtualMovement, Vector3f())

        val perEyeResults = (0 until 2).map { eyeIndex ->
            val fov = pViews[eyeIndex].fov()

            // TODO Stop hardcoding nearZ and farZ (note: it happens also in OpenVrManager)
            val nearZ = 0.01f
            val farZ = 100f
            val projectionMatrix = Matrix4f().scale(1f, -1f, 1f).frustum(
                tan(fov.angleLeft()) * nearZ, tan(fov.angleRight()) * nearZ,
                tan(fov.angleDown()) * nearZ, tan(fov.angleUp()) * nearZ,
                nearZ, farZ, true
            )

            val position = pViews[eyeIndex].pose().`position$`()
            val orientation = pViews[eyeIndex].pose().orientation()
            val viewMatrix = Matrix4f()
                .rotateY(-extraRotationY.radians)
                .translate(
                    position.x(), position.y(), position.z()
                )
                .translate(
                    -averageRealEyePosition.x, -averageRealEyePosition.y, -averageRealEyePosition.z
                )
                .rotateAffine(
                    Quaternionf(orientation.x(), orientation.y(), orientation.z(), orientation.w())
                )

                .invertAffine()

            if (eyeIndex == 0) leftViewMatrix = Matrix4f(viewMatrix)

            val eyeMatrix = projectionMatrix.mul(viewMatrix)
            eyeMatrix
        }

        val leftEyeMatrix = perEyeResults[0]
        val rightEyeMatrix = perEyeResults[1]

        lastRealPosition = averageRealEyePosition
        lastVirtualPosition = Vector3f(averageVirtualEyePosition)

        return Triple(averageVirtualEyePosition, leftEyeMatrix, rightEyeMatrix)
    }
}
