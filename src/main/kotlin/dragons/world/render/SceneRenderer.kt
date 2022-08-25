package dragons.world.render

import dragons.world.realm.Realm
import org.joml.Matrix4f
import org.joml.Vector3f

interface SceneRenderer {
    fun render(realm: Realm, averageEyePosition: Vector3f, leftCameraMatrix: Matrix4f, rightCameraMatrix: Matrix4f)

    fun submit(realm: Realm, waitSemaphores: LongArray, waitStageMasks: IntArray, signalSemaphores: LongArray)
}
