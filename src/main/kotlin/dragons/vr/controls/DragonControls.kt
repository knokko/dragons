package dragons.vr.controls

import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f

class DragonControls(
    val walkDirection: Vector2f,
    val cameraTurnDirection: Float,
    val isSpitting: Boolean,
    val isUsingPower: Boolean,
    val shouldToggleMenu: Boolean,
    val shouldToggleLeftWing: Boolean,
    val shouldToggleRightWing: Boolean,
    val isGrabbingLeft: Boolean,
    val isGrabbingRight: Boolean,
    val leftHandPosition: Vector3f?,
    val rightHandPosition: Vector3f?,
    val leftHandOrientation: Quaternionf?,
    val rightHandOrientation: Quaternionf?
)
