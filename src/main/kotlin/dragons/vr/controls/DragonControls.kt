package dragons.vr.controls

import org.joml.Vector2f

class DragonControls(
    val walkDirection: Vector2f,
    val cameraTurnDirection: Float,
    val isSpitting: Boolean,
    val isUsingPower: Boolean,
    val shouldToggleMenu: Boolean,
    val shouldToggleLeftWing: Boolean,
    val shouldToggleRightWing: Boolean,
    val isGrabbingLeft: Boolean,
    val isGrabbingRight: Boolean
) {
}