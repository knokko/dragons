package dragons.vr

import org.joml.Matrix4f
import org.joml.Vector3f

class CameraMatrices(
    /**
     * See documentation of averageVirtualEyePosition
     */
    val averageRealEyePosition: Vector3f,
    /**
     * ## Expected position
     * There are 2 ways players can walk in VR:
     * - Use the left joystick
     * - Walk in real life
     *
     * And there are 2 ways players can rotate in VR:
     * - Use the right joystick
     * - Rotate their real head
     *
     * This variable is needed to handle real life walking. To do so, 2 types of positions are considered:
     * - The physical position
     * - The expected position
     *
     * The physical position is simply the real life position of the player, compared to some fixed origin. This is
     * easy to extract from the OpenVR or OpenXR runtime. This is very useful until the player rotates via the right
     * joystick. Consider the following scenario:
     * 1) The player walks 1 meter forward in real life
     * 2) The player rotates 90 degrees to the right using the right joystick
     * 3) The player walks 1 meter forward in real life
     *
     * Assuming that the player starts at the origin, the Z-axis is the forward direction, and the X-axis is the right
     * direction, the **physical** position would be (0, 0, 2) because the player walked 2 meters forward in total.
     * However, the player took a virtual turn to the right, so he would expect to have walked 1 meter forward and 1
     * meter to the right, so his **expected** position would be (1, 0, 1). This variable will hold the **expected**
     * position rather than the **physical** position.
     *
     * ## Average eye position
     * To handle lighting correctly, the position of the camera is needed. But... there are 2 cameras in VR: the left
     * eye and the right eye. Computing the lighting separately for each eye would be a lot of work (both to program and
     * to compute), and the difference would probably hard to see because the eyes are very close to each other. To
     * work around this problem, the **average** of the left eye position and the right eye position is used. (This
     * pint will be exactly between the left eye and the right eye.)
     *
     * ## Coordinate system
     * This position is relative to some *origin on the ground* in the *real* world and uses the *meter* as distance
     * unit.
     *
     * ### Y-axis
     * The y-coordinate of this position is the vertical coordinate and a positive y-value indicates that the
     * position is *above* the *origin/ground* (which should usually be the case).
     *
     * ### X-axis and Z-axis
     * The x-axis and the z-axis are consistent with the eye matrices (so if the eye matrices indicate that the player
     * is looking in the positive X-direction and the (x, z) coordinates of the `averageVirtualEyePosition` are (1, 0),
     * the player expects the origin to be 1 meter behind him.
     *
     * Other than that, the real-world directions of these axes are not
     * specified (so the positive X-direction could be north, but could just as well be east, or some direction
     * between south and west).
     *
     * ## Value
     * This value of this variable will be the **expected** **average** eye position. (But it is called
     * *averageVirtualEyePosition* because the term **expected** is probably confusing to see in the code.)
     *
     * ## Purpose
     * This, together with the position that is managed by the walking joystick, is needed to determine the in-game
     * position of the player. Since the eye matrices use the average eye position as origin, the translation of the
     * eye matrices can **not** be used for this!
     */
    val averageVirtualEyePosition: Vector3f,
    /**
     * The average of the view matrices of the left eye and the right eye. This is currently used to determine in
     * which direction the player is looking. Note: since the average eye position is considered as the origin, the
     * translation of this matrix should be (nearly) 0.
     */
    val averageViewMatrix: Matrix4f,
    /**
     * The VR equivalent of projectionMatrix * viewMatrix for the left eye. This variable should be propagated to the
     * shaders. Note: this matrix will consider the average eye position as the origin, so its translation will be
     * very small.
     */
    val leftEyeMatrix: Matrix4f,
    /**
     * The VR equivalent of projectionMatrix * viewMatrix for the right eye. This variable should be propagated to the
     * shaders. Note: this matrix will consider the average eye position as the origin, so its translation will be
     * very small.
     */
    val rightEyeMatrix: Matrix4f
)
